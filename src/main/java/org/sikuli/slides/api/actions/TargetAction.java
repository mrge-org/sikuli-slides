package org.sikuli.slides.api.actions;

import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import org.sikuli.script.Location;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.Match;
import org.sikuli.slides.api.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class TargetAction extends ChainedAction {
    private static final Logger LOG = LoggerFactory.getLogger(TargetAction.class);
	
	private Pattern pattern;
	
	public TargetAction(Pattern pattern){
		this.setPattern(pattern);
	}
	
	public TargetAction(Pattern pattern, Action targetAction){
		this.setPattern(pattern);
		setChild(targetAction);
	}
	
	@Override
	public void execute(Context context) throws ActionExecutionException {
        Pattern searchPattern = getPattern().similar(context.getMinScore());
        Region screenRegion = context.getScreenRegion();
        boolean exhaustive = Boolean.TRUE.equals(context.getParameters().get("exhaustive"));
        // park mouse away from region to avoid cursor affecting match
        parkMouse(screenRegion);
        Match targetMatch = null;
        // log pattern info to help diagnose scaling issues
        try {
            if (getPattern() != null && getPattern().getImage() != null && getPattern().getImage().get() != null) {
                int pw = getPattern().getImage().get().getWidth();
                int ph = getPattern().getImage().get().getHeight();
                LOG.info("pattern pixel size=" + pw + "x" + ph + " min_score=" + context.getMinScore());
                if (exhaustive) {
                    int rw = screenRegion.getW();
                    int rh = screenRegion.getH();
                    long positions = Math.max(0L, (long)(rw - pw + 1)) * Math.max(0L, (long)(rh - ph + 1));
                    LOG.info("exhaustive=true region=" + rw + "x" + rh + " positions=" + positions);
                }
                try {
                    boolean hasAlpha = getPattern().getImage().get().getColorModel().hasAlpha();
                    LOG.debug("pattern hasAlpha=" + hasAlpha);
                    // Save the exact pattern used for matching
                    BufferedImage patImg = getPattern().getImage().get();
                    File outDir = new File("target/debug/presearch");
                    if (!outDir.exists()) { outDir.mkdirs(); }
                    File patFile = new File(outDir, String.format("pattern_%dx%d.png", pw, ph));
                    ImageIO.write(patImg, "png", patFile);
                    int p0 = patImg.getRGB(Math.min(0, pw-1), Math.min(0, ph-1));
                    long pchk = 0;
                    for (int y = 0; y < ph; y+=Math.max(1, ph/10)) {
                        for (int x = 0; x < pw; x+=Math.max(1, pw/10)) {
                            pchk = (pchk * 1315423911L) ^ patImg.getRGB(x, y);
                        }
                    }
                    LOG.debug("saved pattern to " + patFile.getAbsolutePath() + " firstPixelRGBA=0x" + Integer.toHexString(p0) + " checksum=" + Long.toUnsignedString(pchk));
                } catch (Throwable ignore2) {}
            }
        } catch (Throwable ignore) {}
        // log region/screen diagnostics and capture the live search area prior to matching
        try {
            int rx = screenRegion.getX();
            int ry = screenRegion.getY();
            int rw = screenRegion.getW();
            int rh = screenRegion.getH();
            Object scrObj = screenRegion.getScreen();
            String sdesc = (scrObj != null ? scrObj.toString() : "null");
            LOG.debug("region origin=(" + rx + "," + ry + ") size=" + rw + "x" + rh + " screen=" + sdesc);
            // capture and save a pre-search snapshot for comparison with AutomationExecutor's failed-search image
            try {
                if (screenRegion.getScreen() != null) {
                    BufferedImage snap = screenRegion.getScreen().capture(screenRegion).getImage();
                    File outDir = new File("target/debug/presearch");
                    if (!outDir.exists()) {
                        outDir.mkdirs();
                    }
                    File outFile = new File(outDir, String.format("region_%d_%d_%dx%d.png", rx, ry, rw, rh));
                    ImageIO.write(snap, "png", outFile);
                    int r0 = snap.getRGB(Math.min(0, rw-1), Math.min(0, rh-1));
                    long rchk = 0;
                    for (int y = 0; y < rh; y+=Math.max(1, rh/10)) {
                        for (int x = 0; x < rw; x+=Math.max(1, rw/10)) {
                            rchk = (rchk * 1315423911L) ^ snap.getRGB(x, y);
                        }
                    }
                    LOG.debug("saved pre-search region snapshot to " + outFile.getAbsolutePath() + " firstPixelRGBA=0x" + Integer.toHexString(r0) + " checksum=" + Long.toUnsignedString(rchk));
                }
            } catch (Throwable snapEx) {
                LOG.debug("failed to save pre-search snapshot: " + snapEx.getMessage());
            }
        } catch (Throwable ignore) {}
        long t0 = System.nanoTime();
        try {
            if (exhaustive) {
                // Single exhaustive scan without waiting/retries
                targetMatch = screenRegion.find(searchPattern);
            } else {
                // honor configured wait time (ms -> seconds)
                double timeout = Math.max(0, context.getWaitTime() / 1000.0);
                targetMatch = screenRegion.wait(searchPattern, timeout);
            }
        } catch (org.sikuli.script.FindFailed e) {
            // target not found
            LOG.debug("SikuliX FindFailed: " + e.getMessage());
        }
        // if not found and it's the first executed slide, try multi-scale fallbacks
        if (!exhaustive && targetMatch == null && Boolean.TRUE.equals(context.getParameters().get("firstExecutedSlide"))) {
            LOG.info("first slide fallback: starting multi-scale matching");
            // Cover common Retina/simulator factors: 2x, 3x and corresponding downsizes
            float[] scales = new float[] {2.0f, 3.0f, 1.5f, 0.75f, 0.5f, 0.33f, 1.25f, 0.8f};
            boolean scaledTried = false;
            for (float s : scales) {
                try {
                    Pattern scaled = new Pattern(getPattern().getImage()).resize(s).similar(context.getMinScore());
                    scaledTried = true;
                    LOG.info("retry match with scaled pattern factor=" + s);
                    try {
                        double timeout = Math.max(0, context.getWaitTime() / 1000.0);
                        targetMatch = screenRegion.wait(scaled, timeout);
                    } catch (org.sikuli.script.FindFailed e) {
                        // continue
                    }
                    if (targetMatch != null) {
                        LOG.info("scaled match succeeded with factor=" + s + " score=" + targetMatch.getScore());
                        break;
                    }
                } catch (Throwable t) {
                    LOG.warn("scaled pattern build failed for factor=" + s + ": " + t.getMessage());
                }
            }
            if (targetMatch == null) {
                if (scaledTried) {
                    LOG.info("multi-scale fallback did not find a match");
                } else {
                    LOG.warn("multi-scale fallback could not be attempted (no scaling support)");
                }
            }
        }
        long t1 = System.nanoTime();
        if (targetMatch != null){
            Location c = targetMatch.getTarget();
            LOG.info("match found at (" + c.getX() + ", " + c.getY() + ") size " + targetMatch.getW() + "x" + targetMatch.getH() + " score=" + targetMatch.getScore() + String.format(" elapsed=%.3fs", (t1 - t0)/1e9));
            Context childConext = new Context(context, targetMatch);
            Action child = getChild();
            if (child != null){
                child.execute(childConext);            
            }
        }else{
            LOG.info("no match in region " + screenRegion + " with min_score=" + context.getMinScore() + String.format(" elapsed=%.3fs", (t1 - t0)/1e9));
            throw new ActionExecutionException("Unable to locate the target on the screen", this);
        }
    }

    private void parkMouse(Region r){
        try {
            int parkX = Math.max(0, r.getX() - 30);
            int parkY = Math.max(0, r.getY() - 30);
            Robot robot = new Robot();
            robot.mouseMove(parkX, parkY);
        } catch (Throwable t) {
            // ignore
        }
    }

	public String toString(){
		return Objects.toStringHelper(this)				
				.add("pattern", getPattern()).toString();
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

}