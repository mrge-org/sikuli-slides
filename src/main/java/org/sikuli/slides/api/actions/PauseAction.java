package org.sikuli.slides.api.actions;

import org.sikuli.script.Region;
import org.sikuli.slides.api.Context;
import org.sikuli.slides.api.concurrent.Latch;
import org.sikuli.slides.api.concurrent.ScreenRegionHoverLatch;

public class PauseAction extends ChainedAction {

    private Latch latch;

    @Override
    public void execute(Context context) throws ActionExecutionException {

        Region r = context.getScreenRegion();
        // compute center region as 40%-60% box
        int rx = r.getX();
        int ry = r.getY();
        int rw = r.getW();
        int rh = r.getH();
        int cx = rx + (int) Math.round(0.4 * rw);
        int cy = ry + (int) Math.round(0.4 * rh);
        int cw = (int) Math.round(0.2 * rw);
        int ch = (int) Math.round(0.2 * rh);
        Region centerRegion = new Region(cx, cy, cw, ch);

        // wait for a hover event in the center box
        latch = new ScreenRegionHoverLatch(centerRegion);
        latch.await();

        if (getChild() != null)
            getChild().execute(context);
    }

    public void stop(){
        if (latch != null)
            latch.release();
        super.stop();
    }

}
