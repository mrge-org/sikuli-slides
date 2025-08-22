package org.sikuli.slides.api.actions;

import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import org.sikuli.script.Location;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.Match;
import org.sikuli.script.Finder;
import org.sikuli.slides.api.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class TargetAction extends ChainedAction {
    private static final Logger LOG = LoggerFactory.getLogger(TargetAction.class);
    
    private static class Result { int x, y; double ssd, ncc; }
    
    private static class NCCResult { int x, y; double ncc; }
    
    // NCC-based pattern matching using the same algorithm as VerifyPatternInRegion
    private NCCResult findWithNCC(BufferedImage region, BufferedImage pattern, double minScore) {
        if (pattern.getWidth() > region.getWidth() || pattern.getHeight() > region.getHeight()) {
            return null;
        }
        
        // Convert to grayscale double arrays for deterministic math
        double[][] R = toGrayscale(region);
        double[][] P = toGrayscale(pattern);
        
        int RH = R.length, RW = R[0].length;
        int PH = P.length, PW = P[0].length;
        
        // Precompute pattern statistics
        double pSum = 0, pSumSq = 0;
        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                double v = P[y][x];
                pSum += v;
                pSumSq += v * v;
            }
        }
        double pN = PW * PH;
        double pMean = pSum / pN;
        double pVar = Math.max(1e-12, (pSumSq / pN) - pMean * pMean);
        double pStd = Math.sqrt(pVar);
        
        double bestNCC = -1.0;
        int bestX = 0, bestY = 0;
        
        // Sliding window search
        for (int y = 0; y <= RH - PH; y++) {
            for (int x = 0; x <= RW - PW; x++) {
                double rSum = 0, rSumSq = 0;
                for (int j = 0; j < PH; j++) {
                    for (int i = 0; i < PW; i++) {
                        double rv = R[y + j][x + i];
                        rSum += rv;
                        rSumSq += rv * rv;
                    }
                }
                
                // NCC with mean/std normalization
                double rMean = rSum / pN;
                double rVar = Math.max(1e-12, (rSumSq / pN) - rMean * rMean);
                double rStd = Math.sqrt(rVar);
                
                double centeredCross = 0;
                for (int j = 0; j < PH; j++) {
                    for (int i = 0; i < PW; i++) {
                        double rv = R[y + j][x + i] - rMean;
                        double pv = P[j][i] - pMean;
                        centeredCross += rv * pv;
                    }
                }
                double denom = (pStd * rStd) * pN;
                double ncc = (denom > 0) ? (centeredCross / denom) : -1.0;
                
                if (ncc > bestNCC) {
                    bestNCC = ncc;
                    bestX = x; 
                    bestY = y;
                }
            }
        }
        
        if (bestNCC >= minScore) {
            NCCResult result = new NCCResult();
            result.x = bestX;
            result.y = bestY;
            result.ncc = bestNCC;
            return result;
        }
        
        return null;
    }
    
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
        // Prominent notice to avoid user interaction during automated run
        try {
            System.out.println();
            System.out.println("DO NOT USE KEYBOARD AND MOUSE UNTIL TEST EXECUTION END!");
            System.out.println();
        } catch (Throwable t) {
            // ignore console issues
        }
        LOG.info("DEBUG: TargetAction.execute() called - checking for NCC fallback integration");
        LOG.info("DEBUG: Context.getMinScore() = " + context.getMinScore());
        Pattern searchPattern = getPattern().similar(context.getMinScore());
        LOG.debug("searchPattern=" + String.valueOf(searchPattern));
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
                    // quick grayscale heuristic on the pattern
                    try {
                        boolean pg = isGrayscaleLike(patImg);
                        LOG.debug("pattern grayscale_like=" + pg + " awtType=" + patImg.getType());
                    } catch (Throwable ig) {}
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
            // dump SikuliX Settings once per invocation for transparency
            try {
                dumpSikuliSettings();
            } catch (Throwable ds) {
                LOG.debug("failed to dump SikuliX Settings: " + ds.getMessage());
            }
            // capture and save a pre-search snapshot for comparison with AutomationExecutor's failed-search image
            try {
                if (screenRegion.getScreen() != null) {
                    BufferedImage snap = screenRegion.getScreen().capture(screenRegion).getImage();
                    try {
                        boolean rg = isGrayscaleLike(snap);
                        LOG.debug("region grayscale_like=" + rg + " awtType=" + snap.getType());
                    } catch (Throwable ig2) {}
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
        // Enforce strict 1:1 single-scale matching only (no SikuliX internal multi-scale)
        // Always use our NCC implementation to avoid any implicit scaling.
        LOG.info("single-scale mode: using NCC-only search (no SikuliX find/wait)");
        targetMatch = tryNCCFallback(screenRegion, context.getMinScore());
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

    // Heuristic: sample pixels to decide if image is nearly grayscale (R≈G≈B)
    private boolean isGrayscaleLike(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int stepY = Math.max(1, h / 20);
        int stepX = Math.max(1, w / 20);
        int tol = 2; // small tolerance for compression/rounding
        int colored = 0;
        int total = 0;
        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                int argb = img.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                if (!(Math.abs(r - g) <= tol && Math.abs(r - b) <= tol && Math.abs(g - b) <= tol)) {
                    colored++;
                }
                total++;
            }
        }
        // consider grayscale-like if fewer than 5% of sampled pixels are colored
        return colored * 20 <= total;
    }

    // Reflectively dump SikuliX Settings public static fields without binding to version-specific names
    private void dumpSikuliSettings() {
        try {
            // SikuliX 2.x: org.sikuli.basics.Settings; older builds: org.sikuli.script.Settings
            Class<?> cls = null;
            try {
                cls = Class.forName("org.sikuli.basics.Settings");
            } catch (ClassNotFoundException e1) {
                try {
                    cls = Class.forName("org.sikuli.script.Settings");
                } catch (ClassNotFoundException e2) {
                    LOG.debug("SikuliX Settings class not found");
                    return;
                }
            }

            java.lang.reflect.Field[] fields = cls.getFields();
            StringBuilder sb = new StringBuilder();
            sb.append("SikuliX Settings: ");
            int count = 0;
            for (java.lang.reflect.Field f : fields) {
                int mod = f.getModifiers();
                if (java.lang.reflect.Modifier.isStatic(mod) && java.lang.reflect.Modifier.isPublic(mod)) {
                    try {
                        Object val = f.get(null);
                        // limit overly long values
                        String vs = String.valueOf(val);
                        if (vs.length() > 120) {
                            vs = vs.substring(0, 117) + "...";
                        }
                        if (count > 0) sb.append("; ");
                        sb.append(f.getName()).append("=").append(vs);
                        count++;
                    } catch (Throwable ig) {
                        // skip if not accessible
                    }
                }
            }
            if (count > 0) {
                LOG.info(sb.toString());
            } else {
                LOG.info("SikuliX Settings: <none>");
            }
        } catch (Throwable t) {
            LOG.debug("failed to dump SikuliX Settings: " + t.getMessage());
        }
    }

    private static double[][] toGrayscale(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        double[][] g = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int gg = (argb >> 8) & 0xFF;
                int b = (argb) & 0xFF;
                // standard luminance
                g[y][x] = 0.299 * r + 0.587 * gg + 0.114 * b;
            }
        }
        return g;
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

    private Match tryNCCFallback(Region screenRegion, double minScore) {
        try {
            LOG.info("NCC fallback: starting custom pattern matching");
            BufferedImage regionImage = screenRegion.getScreen().capture(screenRegion).getImage();
            BufferedImage patternImage = getPattern().getImage().get();
            
            NCCResult nccResult = findWithNCC(regionImage, patternImage, minScore);
            if (nccResult != null) {
                LOG.info("NCC fallback: found match with score=" + nccResult.ncc + " at (" + nccResult.x + "," + nccResult.y + ")");
                // Create a Match object from NCC result
                int centerX = screenRegion.getX() + nccResult.x + patternImage.getWidth() / 2;
                int centerY = screenRegion.getY() + nccResult.y + patternImage.getHeight() / 2;
                Match match = new Match(new Region(centerX - patternImage.getWidth()/2, centerY - patternImage.getHeight()/2, 
                                                  patternImage.getWidth(), patternImage.getHeight()), nccResult.ncc);
                match.setTarget(centerX, centerY);
                LOG.info("NCC fallback: created match at " + match.getTarget() + " score=" + match.getScore());
                return match;
            } else {
                LOG.info("NCC fallback: no match found above threshold " + minScore);
                return null;
            }
        } catch (Throwable ex) {
            LOG.warn("NCC fallback failed: " + ex.getMessage());
            return null;
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