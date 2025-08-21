package org.sikuli.recorder.detector;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import org.sikuli.script.Region;
import org.sikuli.recorder.event.ClickEvent;

public class MouseEventDetector extends EventDetector 
implements NativeMouseInputListener {

    private volatile boolean detected = false;

    public void nativeMouseClicked(NativeMouseEvent e) {
        //            System.out.println("Mosue Clicked: " + e.getClickCount());

        
        Region region = getRegionOfInterest();
        int rx = region.getX();
        int ry = region.getY();
        int rw = region.getW();
        int rh = region.getH();
        int ex = e.getX();
        int ey = e.getY();
        boolean isInsideROI = (ex >= rx && ex <= rx + rw && ey >= ry && ey <= ry + rh);
        if (isInsideROI){
            detected = true;
            double scale = 1.0;
            try { scale = Math.max(0.01, getCaptureContext().getScale()); } catch (Throwable t) { }
            ClickEvent event = new ClickEvent();
            // Scale coordinates into screenshot pixel space so they align with saved images
            int relX = ex - rx;
            int relY = ey - ry;
            event.setX((int) Math.round(relX * scale));
            event.setY((int) Math.round(relY * scale));        
            event.setButton(e.getButton());
            event.setClickCount(e.getClickCount());
            eventDetected(event);
        }
    }

    public void nativeMousePressed(NativeMouseEvent e) {
        //System.out.println("Mosue Pressed: " + e.getButton());
    }

    public void nativeMouseReleased(NativeMouseEvent e) {
        //System.out.println("Mosue Released: " + e.getButton());
    }

    public void nativeMouseMoved(NativeMouseEvent e) {
        //System.out.println("Mosue Moved: " + e.getX() + ", " + e.getY());
    }

    public void nativeMouseDragged(NativeMouseEvent e) {
        //System.out.println("Mosue Dragged: " + e.getX() + ", " + e.getY());
    }

    public void start(){
        try {
            GlobalScreen.registerNativeHook();
        }
        catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());

            //            System.exit(1);
        }

        //Construct the example object.
        //GlobalMouseListenerExample example = new GlobalMouseListenerExample();

        //Add the appropriate listeners for the example object.
        GlobalScreen.addNativeMouseListener(this);
        GlobalScreen.addNativeMouseMotionListener(this);
    }
    
    public void stop(){
        GlobalScreen.removeNativeMouseListener(this);
        GlobalScreen.removeNativeMouseMotionListener(this);
    }
    
    public boolean hasDetected() {
        return detected;
    }
}