package org.sikuli.slides.api.actions;

import java.awt.Robot;
import java.awt.event.InputEvent;

import org.sikuli.script.Location;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;
import org.sikuli.slides.api.Context;

import com.google.common.base.Objects;

public class RightClickAction extends RobotAction {
    @Override
    protected void doExecute(Context context) {
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
        logger.info("mouse right-click at (" + c.getX() + ", " + c.getY() + ") on screen " + screenId + " within " + screenRegion + (context.isUseAwtRobot() ? " using AWT Robot" : " using SikuliX"));
        if (context.isUseAwtRobot()) {
            try {
                Robot r = new Robot();
                r.setAutoWaitForIdle(true);
                r.mouseMove(c.getX(), c.getY());
                r.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                r.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                return;
            } catch (Exception e) {
                logger.warn("AWT Robot right-click failed, falling back to SikuliX: " + e.getMessage());
            }
        }
        screenRegion.rightClick();
    }

    public String toString(){
        return Objects.toStringHelper(this).toString();
    }

}
