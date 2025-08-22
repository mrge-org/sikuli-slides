package org.sikuli.recorder.detector;

import java.awt.image.BufferedImage;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.MouseInfo;
import java.awt.Point;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.sikuli.recorder.event.ScreenShotEvent;
import org.sikuli.script.ScreenImage;
import org.sikuli.recorder.CaptureContext;
import org.sikuli.script.Region;

public class ScreenshotEventDetector extends EventDetector {

    public void stop(){
        running = false;
        try {
            if (capturingThread != null) {
                capturingThread.join();
            }
        } catch (InterruptedException e) {
        }
    }

    volatile  boolean running = true;
    private Thread capturingThread;
    private volatile int lastImgW = -1, lastImgH = -1;

    
    public void start(){
        
        
        capturingThread = new Thread(){
            public void run(){
                while (running){
                    running = true;
                    performScreenCapture(false);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
                
        capturingThread.start();
    }

    // Guided mode: perform a single capture on demand (no background loop)
    public void captureOnce() {
        performScreenCapture(true);
    }

    private void performScreenCapture(boolean hideCursor){
        Region roi = getRegionOfInterest();
        int rx = roi.getX(), ry = roi.getY(), rw = roi.getW(), rh = roi.getH();
        BufferedImage imgSikuli = null;
        BufferedImage imgAwt = null;
        try {
            // Optionally move cursor out of ROI during capture and restore afterwards
            Point prevLoc = null;
            Robot mover = null;
            if (hideCursor) {
                try {
                    prevLoc = MouseInfo.getPointerInfo().getLocation();
                    mover = new Robot();
                    // move far to bottom-right of ROI to avoid cursor in capture
                    int mx = Math.max(0, rx + rw + 200);
                    int my = Math.max(0, ry + rh + 200);
                    mover.mouseMove(mx, my);
                    // small wait for cursor to settle
                    try { Thread.sleep(40); } catch (InterruptedException ignored) {}
                } catch (Throwable ignored) {}
            }
            CaptureContext.Backend backend = getCaptureContext().getBackend();
            // Capture using selected backend(s)
            if (backend == CaptureContext.Backend.sikuli || backend == CaptureContext.Backend.both) {
                ScreenImage si = roi.getScreen().capture(roi);
                if (si != null) imgSikuli = si.getImage();
            }
            if (backend == CaptureContext.Backend.awt_raw || backend == CaptureContext.Backend.both) {
                try {
                    Robot r = new Robot();
                    imgAwt = r.createScreenCapture(new Rectangle(rx, ry, rw, rh));
                } catch (AWTException awte) {
                    // ignore; will fallback to sikuli image if available
                }
            }

            BufferedImage imageToEmit;
            String emittedBackend;
            if (backend == CaptureContext.Backend.awt_raw || (backend == CaptureContext.Backend.both && imgAwt != null)) {
                imageToEmit = imgAwt != null ? imgAwt : imgSikuli;
                emittedBackend = "awt_raw";
            } else {
                imageToEmit = imgSikuli;
                emittedBackend = "sikuli";
            }

            if (imageToEmit == null) return;

            // Update scale: image pixels vs ROI width/height
            double sx = (rw > 0) ? (imageToEmit.getWidth() * 1.0 / rw) : 1.0;
            double sy = (rh > 0) ? (imageToEmit.getHeight() * 1.0 / rh) : 1.0;
            // Assume square pixels; use X scale
            getCaptureContext().setScale(sx);

            // Write diagnostics if dims changed
            if (imageToEmit.getWidth() != lastImgW || imageToEmit.getHeight() != lastImgH) {
                lastImgW = imageToEmit.getWidth();
                lastImgH = imageToEmit.getHeight();
                writeDiagnostics(roi, imgSikuli, imgAwt, emittedBackend, sx, sy);
            }

            ScreenShotEvent e = new ScreenShotEvent();
            e.setImage(imageToEmit);
            eventDetected(e);
            // restore cursor
            if (hideCursor) {
                try {
                    if (mover != null && prevLoc != null) {
                        mover.mouseMove(prevLoc.x, prevLoc.y);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            // swallow errors to keep detector running
        }
    }

    private void writeDiagnostics(Region roi, BufferedImage sikuli, BufferedImage awt, String emitted, double sx, double sy) {
        try {
            File dir = getEventDir();
            if (sikuli != null) {
                try { ImageIO.write(sikuli, "png", new File(dir, "probe.sikuli.png")); } catch (IOException ignored) {}
            }
            if (awt != null) {
                try { ImageIO.write(awt, "png", new File(dir, "probe.awt.png")); } catch (IOException ignored) {}
            }
            File meta = new File(dir, "probe.meta.txt");
            StringBuilder sb = new StringBuilder();
            sb.append("ROI ").append(roi.getX()).append(",").append(roi.getY()).append(" ")
              .append(roi.getW()).append("x").append(roi.getH()).append("\n");
            if (sikuli != null) sb.append("sikuli ").append(sikuli.getWidth()).append("x").append(sikuli.getHeight()).append("\n");
            if (awt != null) sb.append("awt_raw ").append(awt.getWidth()).append("x").append(awt.getHeight()).append("\n");
            sb.append("emitted ").append(emitted).append("\n");
            sb.append("scaleX ").append(sx).append(" scaleY ").append(sy).append("\n");
            java.nio.file.Files.write(meta.toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }
}