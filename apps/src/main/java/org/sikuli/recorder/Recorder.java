package org.sikuli.recorder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.awt.Toolkit;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javax.imageio.ImageIO;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;
import org.sikuli.script.ScreenImage;
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

    private MouseEventDetector mouseDetector;
    private final CaptureContext captureContext = new CaptureContext();

    public Recorder(){
        mouseDetector = new MouseEventDetector();
        EventDetector d2 = new ScreenshotEventDetector();
        addEventDetector(mouseDetector);
        addEventDetector(d2);
        // Default to full primary screen
        setRegionOfInterest(new Screen());
        // Thread capture context to detectors
        for (EventDetector d : detectors) {
            d.setCaptureContext(captureContext);
        }
    }

    public File getEventDir() {
        return writer.getEventDir();
    }

    public void setEventDir(File dir) {
        writer.setEventDir(dir);
    }

    private List<EventDetector> detectors = Lists.newArrayList();
    private ScreenshotEventDetector screenshotDetectorRef = null;

    DefaultEventWriter writer = new DefaultEventWriter();
    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private Thread consoleStopper;
    private Thread fileStopper;
    public void addEventDetector(EventDetector d) {
        d.setWriter(writer);
        d.setCaptureContext(captureContext);
        detectors.add(d);
        if (d instanceof ScreenshotEventDetector) {
            this.screenshotDetectorRef = (ScreenshotEventDetector) d;
        }
    }

    public void startRecording(){
        if (!hasStarted.compareAndSet(false, true)) {
            logger.debug("Recording already started; ignoring duplicate hotkey");
            return;
        }
        logger.info("Start Recording");
        System.out.println("Recording started.");
        System.out.println(" Temp dir: " + System.getProperty("java.io.tmpdir"));
        System.out.println(" Event folder: " + getEventDir().getAbsolutePath());
        System.out.println(" Capture backend: " + captureContext.getBackend());
        if (regionOfInterest != null) {
            System.out.println(String.format(" ROI: x=%d y=%d w=%d h=%d",
                    regionOfInterest.getX(), regionOfInterest.getY(), regionOfInterest.getW(), regionOfInterest.getH()));
        }

        // Probe a single screen capture to surface Screen Recording issues on macOS.
        try {
            ScreenImage probe = regionOfInterest.getScreen().capture(regionOfInterest);
            if (probe == null || probe.getImage() == null) {
                System.out.println(" Probe capture returned no image. On macOS, grant Screen Recording to your Terminal/IDE.");
            } else {
                try {
                    File f = new File(getEventDir(), "probe.screenshot.png");
                    ImageIO.write(probe.getImage(), "png", f);
                } catch (IOException io) {
                    System.out.println(" Probe capture could not be saved: " + io.getMessage());
                }
            }
        } catch (Throwable t) {
            System.out.println(" Probe capture failed: " + t.getMessage());
            System.out.println(" On macOS, enable: Privacy & Security → Screen Recording for your Terminal/IDE, then retry.");
        }

        for (EventDetector d : detectors){
            d.start();
        }
        startConsoleStopper();
        startFileStopper();
        System.out.println(" Tip: Press Enter in this console to stop (fallback), or use the hotkey.");
        System.out.println(" Tip: Create a file named 'STOP' in current directory or event folder to stop.");

        // After 15s, if no mouse detected, print a hint for macOS permissions and ROI
        new Thread(() -> {
            try { Thread.sleep(15000); } catch (InterruptedException ignored) {}
            try {
                if (hasStarted.get() && mouseDetector != null && !mouseDetector.hasDetected()) {
                    System.out.println(" Note: No mouse activity detected in ROI in the first 15s.");
                    System.out.println("  - Ensure you click inside the specified ROI: "
                            + String.format("x=%d y=%d w=%d h=%d", regionOfInterest.getX(), regionOfInterest.getY(), regionOfInterest.getW(), regionOfInterest.getH()));
                    System.out.println("  - On macOS, verify Input Monitoring permission for your Terminal/IDE.");
                }
            } catch (Throwable ignored) {}
        }, "Recorder-NoMouseActivityWarn").start();
    }

    public void setRegionOfInterest(Region screenRegion) {
        regionOfInterest = screenRegion;
        for (EventDetector d : detectors){
            d.setRegionOfInterest(screenRegion);
        }
    }

    public void setCaptureBackend(CaptureContext.Backend backend) {
        captureContext.setBackend(backend);
    }

    private void startConsoleStopper() {
        if (consoleStopper != null && consoleStopper.isAlive()) return;
        consoleStopper = new Thread(() -> {
            try {
                // Read full lines; stop on empty line (Enter) or 'stop'
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
                String line;
                while ((line = br.readLine()) != null) {
                    String t = line.trim();
                    if (t.isEmpty() || t.equalsIgnoreCase("stop")) {
                        System.out.println(" Console stop detected. Stopping recording...");
                        try { GlobalScreen.unregisterNativeHook(); } catch (NativeHookException ignored) {}
                        escapeSignal.countDown();
                        break;
                    }
                }
            } catch (IOException ignored) {
            }
        }, "Recorder-ConsoleStopper");
        consoleStopper.setDaemon(true);
        consoleStopper.start();
    }

    private void startFileStopper() {
        if (fileStopper != null && fileStopper.isAlive()) return;
        fileStopper = new Thread(() -> {
            File cwdStop = new File("STOP");
            while (true) {
                try {
                    File evtStop = new File(getEventDir(), "STOP");
                    if (cwdStop.exists() || evtStop.exists()) {
                        System.out.println(" File STOP detected. Stopping recording...");
                        try { GlobalScreen.unregisterNativeHook(); } catch (NativeHookException ignored) {}
                        escapeSignal.countDown();
                        break;
                    }
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    break;
                } catch (Throwable t) {
                    // ignore and continue
                }
            }
        }, "Recorder-FileStopper");
        fileStopper.setDaemon(true);
        fileStopper.start();
    }

    CountDownLatch escapeSignal = new CountDownLatch(1);

    class GuidedKeyListener implements NativeKeyListener {
        private Logger logger = LoggerFactory.getLogger(GuidedKeyListener.class);
        public void nativeKeyPressed(NativeKeyEvent e) {
            int code = e.getKeyCode();
            if (code == NativeKeyEvent.VC_ENTER) {
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
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            return;
        }

        GlobalScreen.addNativeKeyListener(new HotKeyListener());
        System.out.println("Global key listener active. If hotkeys do nothing on macOS:");
        System.out.println(" - Enable Privacy & Security → Input Monitoring for your Terminal/IDE");
        System.out.println(" - Enable Privacy & Security → Accessibility for your Terminal/IDE");
        System.out.println(" - Enable Privacy & Security → Screen Recording for your Terminal/IDE");
        System.out.println(" - Close any app with Secure Input active (e.g., password prompts)");
        System.out.println("You can also press Enter here to stop.");

        try {
            escapeSignal.await();
        } catch (InterruptedException e) {
        }

        stopRecording();
        System.out.println("Recording is stopped.");
    }

    public void startGuided() {
        System.out.println("Start Recording (guided mode): Enter=capture, Esc=finish");
        // Start only MouseEventDetector(s)
        for (EventDetector d : detectors) {
            if (d instanceof MouseEventDetector) {
                d.start();
            }
        }
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            return;
        }
        GlobalScreen.addNativeKeyListener(new GuidedKeyListener());
        try {
            escapeSignal.await();
        } catch (InterruptedException e) {
        }
        stopRecording();
        System.out.println("Guided recording is stopped.");
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
                    System.out.println("Hotkey detected: ALT+SHIFT+2 (start)");
                    startRecording();
                }

                // ALT+SHIFT+ESC
                if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE && isShiftPressed && isAltPressed){
                    logger.trace("ALT+SHIFT+ESC is pressed");
                    System.out.println("Hotkey detected: ALT+SHIFT+ESC (stop)");
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
                    System.out.println("Hotkey detected: CMD/CTRL+SHIFT+2 (start)");
                    startRecording();
                }

                // Stop: be permissive on macOS – accept CMD/CTRL+SHIFT+ESC, SHIFT+ESC, or plain ESC once recording started
                if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
                    boolean anyModifierCombo = macLikeModifier || isShiftPressed;
                    boolean plainEsc = !isMetaPressed && !isCtrlPressed && !isAltPressed;
                    if (anyModifierCombo || (plainEsc && hasStarted.get())) {
                        logger.trace("ESC stop detected (mac-permissive)");
                        System.out.println("Hotkey detected: ESC (stop)");
                        try {
                            GlobalScreen.unregisterNativeHook();
                        } catch (NativeHookException ex) {
                            logger.warn("Failed to unregister native hook", ex);
                        }
                        escapeSignal.countDown();
                    }
                }
            }
        }

        public void nativeKeyReleased(NativeKeyEvent e) { }
        public void nativeKeyTyped(NativeKeyEvent e) { }
    }

    public void printHelp() {
        System.out.println("Platform: " + System.getProperty("os.name"));
        if (isWindows()){
            System.out.println("Press [Alt-Shift-2] to start recording");
            System.out.println("Press [Alt-Shift-ESC] to stop recording");
        }else{
            System.out.println("Press [Command-Shift-2] (or [Ctrl-Shift-2]) to start recording");
            System.out.println("Press [Command-Shift-ESC] (or [Ctrl-Shift-ESC]) to stop recording");
            System.out.println("Tip: While recording, plain [Esc] will also stop.");
            System.out.println("If hotkeys do not work, open System Settings → Privacy & Security and grant: Input Monitoring, Accessibility, and Screen Recording to your Terminal/IDE. Then restart it.");
            System.out.println("Fallbacks: Press Enter here, type 'stop'+Enter, or touch a file named 'STOP' in the current dir or event folder.");
        }
    }
}
