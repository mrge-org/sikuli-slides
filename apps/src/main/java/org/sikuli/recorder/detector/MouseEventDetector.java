package org.sikuli.recorder.detector;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import org.sikuli.script.Region;
import org.sikuli.recorder.event.ClickEvent;

public class MouseEventDetector extends EventDetector implements NativeMouseInputListener {

    private volatile boolean detected = false;
    private volatile boolean firstLogged = false;

    public boolean hasDetected() {
        return detected;
    }

    @Override
    public void nativeMouseClicked(NativeMouseEvent e) {
        Region region = getRegionOfInterest();
        int rx = region.getX();
        int ry = region.getY();
        int rw = region.getW();
        int rh = region.getH();
        int ex = e.getX();
        int ey = e.getY();
        boolean isInsideROI = (ex >= rx && ex <= rx + rw && ey >= ry && ey <= ry + rh);
        if (isInsideROI) {
            ClickEvent event = new ClickEvent();
            event.setX(ex - rx);
            event.setY(ey - ry);
            event.setButton(e.getButton());
            event.setClickCount(e.getClickCount());
            eventDetected(event);
            detected = true;
            if (!firstLogged) {
                firstLogged = true;
                System.out.println(" Mouse event detected inside ROI at (" + ex + "," + ey + ")");
            }
        }
    }

    @Override public void nativeMousePressed(NativeMouseEvent e) { }
    @Override public void nativeMouseReleased(NativeMouseEvent e) { }
    @Override public void nativeMouseMoved(NativeMouseEvent e) { }
    @Override public void nativeMouseDragged(NativeMouseEvent e) { }

    public void start(){
        // Do NOT register native hook here; Recorder manages the lifecycle.
        GlobalScreen.addNativeMouseListener(this);
        GlobalScreen.addNativeMouseMotionListener(this);
    }

    public void stop(){
        GlobalScreen.removeNativeMouseListener(this);
        GlobalScreen.removeNativeMouseMotionListener(this);
    }
}
