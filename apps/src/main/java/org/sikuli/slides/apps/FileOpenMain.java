package org.sikuli.slides.apps;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import org.sikuli.slides.api.Context;
import org.sikuli.slides.api.SlideExecutionException;
import org.sikuli.slides.api.Slides;
import org.apache.log4j.Logger;

import com.install4j.api.launcher.StartupNotification;
import com.install4j.api.launcher.StartupNotification.Listener;


public class FileOpenMain {

    private static final Logger LOG = Logger.getLogger(FileOpenMain.class);

    static final CountDownLatch doneSignal = new CountDownLatch(1);
    private static File inputFile;

    public static void main(String[] args) {
        if (args.length == 1){
            inputFile = new File(args[0]);
            executeFile(inputFile);
        } else {
            StartupNotification.registerStartupListener(new Listener(){
                @Override
                public synchronized void startupPerformed(String filename) {
                    if (inputFile == null){
                        inputFile = new File(filename);
                        executeFile(inputFile);
                    }
                }
            });
        }
    }

    private static void executeFile(File file){
        try {
            URL url = file.toURI().toURL();
            Context context = new Context();
            Slides.execute(url, context);
        } catch (MalformedURLException e) {
            LOG.error("Invalid file: " + file + ": " + e.getMessage());
        } catch (SlideExecutionException e) {
            LOG.error("Execution failed: " + e.getMessage());
        }
    }

}
