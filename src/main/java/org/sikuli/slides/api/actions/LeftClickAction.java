package org.sikuli.slides.api.actions;

import java.awt.Robot;
import java.awt.event.InputEvent;

import org.sikuli.script.Location;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;
import org.sikuli.slides.api.Context;

import com.google.common.base.Objects;

public class LeftClickAction extends RobotAction {
    
    @Override
    protected void doExecute(Context context) {
        // save pre-click region before moving the mouse (no cursor in capture)
        savePreClickRegion(context, "left-click");
        Region screenRegion = context.getScreenRegion();
        Location c = screenRegion.getCenter();
        int screenId = -1;
        try {
            int n = Screen.getNumberScreens();
            for (int i = 0; i < n; i++) {
                if (new Screen(i).contains(c)) { screenId = i; break; }
            }
        } catch (Throwable t) {
            // ignore
        }
        logger.info("mouse left-click at (" + c.getX() + ", " + c.getY() + ") on screen " + screenId + " within " + screenRegion + (context.isUseAwtRobot() ? " using AWT Robot" : " using SikuliX"));
        if (context.isUseAwtRobot()) {
            try {
                Robot r = new Robot();
                r.setAutoWaitForIdle(true);
                r.mouseMove(c.getX(), c.getY());
                r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                return;
            } catch (Exception e) {
                logger.warn("AWT Robot left-click failed, falling back to SikuliX: " + e.getMessage());
            }
        }
        screenRegion.click();
    }
    
    public String toString(){
        return Objects.toStringHelper(this).toString();
    }
}
