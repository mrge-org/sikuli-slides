package org.sikuli.slides.apps;

import java.io.File;
import java.util.List;

import org.sikuli.script.Region;
import org.sikuli.recorder.Recorder;
import org.sikuli.recorder.pptx.PPTXGenerator;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

public class RecorderMain {

	public static void main(String[] args) {	
		
		final List<String> parse;
		try {
			parse = Args.parse(Command.class, args);
		} catch (IllegalArgumentException e) {
			Args.usage(Command.class, "java -jar sikuli-slides-1.5.0.jar record [options]");
			System.exit(1);
			return;
		}
		
		if (Command.help){
			Args.usage(Command.class, "java -jar sikuli-slides-1.5.0.jar record [options]");
			System.exit(1);
			return;			
		}
	       

		// Hardening for macOS: ensure JNativeHook can extract natives reliably when shaded
        // Must be set BEFORE any reference to GlobalScreen/Recorder that triggers native load
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            // Avoid headless mode and force a safe, writable temp directory
            System.setProperty("java.awt.headless", "false");
            // Use a canonical, world-writable tmp to avoid path/permission oddities
            System.setProperty("java.io.tmpdir", "/tmp");
        }
        
        Recorder rec = new Recorder();
        final java.util.concurrent.atomic.AtomicBoolean finalized = new java.util.concurrent.atomic.AtomicBoolean(false);

        // Shutdown hook to finalize PPTX on SIGINT (Ctrl-C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (finalized.get()) return;
                finalized.set(true);
                System.out.println("Shutdown detected (SIGINT/SIGTERM). Finalizing slides...");
                try { com.github.kwhat.jnativehook.GlobalScreen.unregisterNativeHook(); } catch (Throwable ignored) {}
                try { rec.stopRecording(); } catch (Throwable ignored) {}
                java.io.File eventDir = rec.getEventDir();
                java.io.File output;
                if (Command.output == null)
                    output = new java.io.File(eventDir.getName() + ".pptx");
                else
                    output = new java.io.File(Command.output);
                if (!finalized.get()) {
                    PPTXGenerator.generate(eventDir, output);
                    System.out.println("Slides are saved as " + output);
                    finalized.set(true);
                }
            } catch (Throwable t) {
                // best effort
            }
        }, "RecorderMain-ShutdownHook"));
		rec.printHelp();
		
		if (Command.bounds != null){
			int x = Command.bounds[0];
			int y = Command.bounds[1];
			int w = Command.bounds[2];
			int h = Command.bounds[3];			
			rec.setRegionOfInterest(new Region(x,y,w,h));
		}
		
		rec.start();
		
		File eventDir = rec.getEventDir();		
		
		File output;
		if (Command.output == null)
			output = new File(eventDir.getName() + ".pptx");
		else
			output = new File(Command.output);	
						
		if (!finalized.get()) {
            PPTXGenerator.generate(eventDir, output);
            System.out.println("Slides are saved as " + output);
            finalized.set(true);
        }
	}

	
	static class Command {
		 @Argument(value = "output", description = "This is the output file (e.g., output.pptx)", required = false)
	     static private String output = null;

		 @Argument(value = "help", description = "Print help message", required = false)
	     static private boolean help = false;		 		 				 
		 
		
		 @Argument(value = "region", description = "Screen region (x, y, width, height) to record (e.g., 100,100,400,400)", required = false, delimiter = ",")
		 static private Integer[] bounds = null;		 
	}

       
}
