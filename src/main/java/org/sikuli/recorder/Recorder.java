package org.sikuli.recorder;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.awt.Toolkit;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;
import org.sikuli.recorder.detector.EventDetector;
import org.sikuli.recorder.detector.MouseEventDetector;
import org.sikuli.recorder.detector.ScreenshotEventDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;


public class Recorder {
	
	static Logger logger = LoggerFactory.getLogger(Recorder.class);

	// Region of interest; visualization removed during migration
	private Region regionOfInterest;
	private boolean guidedMode = false;
	private ScreenshotEventDetector screenshotDetectorRef = null;

	public Recorder(){
		EventDetector d1 = new MouseEventDetector();
		EventDetector d2 = new ScreenshotEventDetector();
		addEventDetector(d1);
		addEventDetector(d2);
		// Default to full primary screen
		setRegionOfInterest(new Screen());
	}	

	public File getEventDir() {
		return writer.getEventDir();
	}

	class GuidedKeyListener implements NativeKeyListener {
		private Logger logger = LoggerFactory.getLogger(GuidedKeyListener.class);
		public void nativeKeyPressed(NativeKeyEvent e) {
			int code = e.getKeyCode();
			if (code == NativeKeyEvent.VC_ENTER) {
				// Beep and capture once without moving the mouse
				try { Toolkit.getDefaultToolkit().beep(); } catch (Throwable ignored) {}
				if (screenshotDetectorRef != null) {
					try { screenshotDetectorRef.captureOnce(); } catch (Throwable t) { logger.warn("captureOnce failed", t); }
				}
			}
			if (code == NativeKeyEvent.VC_ESCAPE) {
				logger.info("ESC pressed - finishing guided recording");
				try { GlobalScreen.unregisterNativeHook(); } catch (Throwable ignored) {}
				escapeSignal.countDown();
			}
		}
		public void nativeKeyReleased(NativeKeyEvent e) {}
		public void nativeKeyTyped(NativeKeyEvent e) {}
	}

	public void setEventDir(File dir) {
		writer.setEventDir(dir);

	}

	private List<EventDetector> detectors = Lists.newArrayList();


	DefaultEventWriter writer = new DefaultEventWriter();
	public void addEventDetector(EventDetector d) {
		d.setWriter(writer);
		detectors.add(d);
		if (d instanceof ScreenshotEventDetector) {
			this.screenshotDetectorRef = (ScreenshotEventDetector) d;
		}
	}

	public void startRecording(){
		logger.info("Start Recording");
		for (EventDetector d : detectors){
			d.start();
		}
	}



	public void setRegionOfInterest(Region screenRegion) {
		regionOfInterest = screenRegion;
		for (EventDetector d : detectors){
			d.setRegionOfInterest(screenRegion);
		}
	}


	CountDownLatch escapeSignal = new CountDownLatch(1);

	public void stopRecording(){	
		for (EventDetector d : detectors){
			d.stop();
		}		
	}

	public void start(){

		try {
			GlobalScreen.registerNativeHook();
		}
		catch (NativeHookException ex) {
			logger.error("There was a problem registering the native hook: {}", ex.getMessage());
			return;
			//System.exit(1);
		}

		//Construct the example object and initialze native hook.
		GlobalScreen.addNativeKeyListener(new HotKeyListener());

		try {
			escapeSignal.await();
		} catch (InterruptedException e) {
		}

		stopRecording();
		logger.info("Recording is stopped.");

	}

	public void startGuided() {
		this.guidedMode = true;
		logger.info("Start Recording (guided mode): Enter=capture, Esc=finish");
		// Start only mouse detector (to record clicks). Do not start screenshot loop.
		for (EventDetector d : detectors){
			if (d instanceof MouseEventDetector) {
				d.start();
			}
		}
		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException ex) {
			logger.error("There was a problem registering the native hook: {}", ex.getMessage());
			return;
		}
		GlobalScreen.addNativeKeyListener(new GuidedKeyListener());
		try {
			escapeSignal.await();
		} catch (InterruptedException e) {
		}
		stopRecording();
		logger.info("Guided recording is stopped.");
	}
	
	boolean isWindows(){
		String currentOs = System.getProperty("os.name");   
		return currentOs.toLowerCase().contains("win");
	}

	class HotKeyListener implements NativeKeyListener {

		private Logger logger = LoggerFactory.getLogger(HotKeyListener.class); 

		public void nativeKeyPressed(NativeKeyEvent e) {

			boolean isMetaPressed = (e.getModifiers() & NativeKeyEvent.META_MASK) > 0;
			boolean isAltPressed = (e.getModifiers() & NativeKeyEvent.ALT_MASK) > 0;
			boolean isShiftPressed = (e.getModifiers() & NativeKeyEvent.SHIFT_MASK) > 0;
			boolean isCtrlPressed = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) > 0;

			if(isWindows()){		        
		        
				// ALT+SHIFT+2
				if (e.getKeyCode() == NativeKeyEvent.VC_2 && isShiftPressed && isAltPressed){                
					logger.trace("ALT+SHIFT+2 is pressed");
					startRecording();
				}

				// ALT+SHIFT+ESC
				if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE && isShiftPressed && isAltPressed){
					logger.trace("ALT+SHIFT+ESC is pressed");
					try {
						GlobalScreen.unregisterNativeHook();
					} catch (NativeHookException ex) {
						logger.warn("Failed to unregister native hook", ex);
					}
					escapeSignal.countDown();
				}

		    }
			else{
				// Accept either CMD (META) or CTRL with SHIFT on non-Windows platforms
				boolean macLikeModifier = isShiftPressed && (isMetaPressed || isCtrlPressed);
				// CMD/CTRL + SHIFT + 2
				if (e.getKeyCode() == NativeKeyEvent.VC_2 && macLikeModifier){                
					logger.trace("CMD/CTRL+SHIFT+2 is pressed");
					startRecording();
				}

				// CMD/CTRL + SHIFT + ESC
				if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE && macLikeModifier){
					logger.trace("CMD/CTRL+SHIFT+ESC is pressed");
					try {
						GlobalScreen.unregisterNativeHook();
					} catch (NativeHookException ex) {
						logger.warn("Failed to unregister native hook", ex);
					}
					escapeSignal.countDown();
				}
			}

		}

		public void nativeKeyReleased(NativeKeyEvent e) {
		}

		public void nativeKeyTyped(NativeKeyEvent e) {
		}

	}

	public void printHelp() {
		logger.info("Platform: {}", System.getProperty("os.name"));
		if (isWindows()){			
			logger.info("Press [Alt-Shift-2] to start recording");
			logger.info("Press [Alt-Shift-ESC] to stop recording");
		}else{
			logger.info("Press [Command-Shift-2] (or [Ctrl-Shift-2]) to start recording");
			logger.info("Press [Command-Shift-ESC] (or [Ctrl-Shift-ESC]) to stop recording");
		}	    
	}
}