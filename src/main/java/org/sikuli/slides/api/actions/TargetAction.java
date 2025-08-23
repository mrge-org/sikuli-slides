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
import org.sikuli.slides.api.models.Slide;
import org.sikuli.slides.api.models.SlideElement;
import org.sikuli.slides.api.models.Selector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import java.util.List;

public class TargetAction extends ChainedAction {
    private static final Logger LOG = LoggerFactory.getLogger(TargetAction.class);
    
    private static class Result { int x, y; double ssd, ncc; }
    
    private static class NCCResult { int x, y; double ncc; }

    // Debug: current slide number used to prefix hint image filenames
    private volatile int debugSlideNum = -1;
    
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
        LOG.debug("TargetAction.execute() called - checking for NCC fallback integration");
        LOG.debug("Context.getMinScore() = " + context.getMinScore());
        // Record slide number for diagnostics
        try {
            Slide s = context.getSlide();
            debugSlideNum = (s != null ? Math.max(1, s.getNumber()) : -1);
        } catch (Throwable ig) { debugSlideNum = -1; }
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
                LOG.debug("pattern pixel size=" + pw + "x" + ph + " min_score=" + context.getMinScore());
                if (exhaustive) {
                    int rw = screenRegion.getW();
                    int rh = screenRegion.getH();
                    long positions = Math.max(0L, (long)(rw - pw + 1)) * Math.max(0L, (long)(rh - ph + 1));
                    LOG.debug("exhaustive=true region=" + rw + "x" + rh + " positions=" + positions);
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
        LOG.debug("single-scale mode: using NCC-only search (no SikuliX find/wait)");
        // Compute a hidden prioritized hint region. We always attempt both sources
        // and then select according to toggle preference with fallback:
        // - default (disable_slide_hint=false): prefer slide-based, fallback to image-driven
        // - disable_slide_hint=true: prefer image-driven, fallback to slide-based
        // When both exist, prefer the smaller region as a proxy for "fastest".
        Region hintRegion = null;
        boolean disableSlideHint = isTruthy(context.getParameters().get("disable_slide_hint"));
        try {
            BufferedImage patImg = getPattern() != null && getPattern().getImage() != null ? getPattern().getImage().get() : null;
            Region slideHint = null;
            Region imageHint = null;
            try {
                slideHint = computeAutoHintRegion(context, patImg);
                if (slideHint != null) {
                    LOG.debug("hint (slide-based) computed: " + slideHint);
                } else {
                    LOG.debug("hint (slide-based) not available");
                }
            } catch (Throwable igSlide) {}
            try {
                imageHint = coarseScanForHint(screenRegion, patImg, context.getMinScore());
                if (imageHint != null) {
                    LOG.debug("hint (image-driven) computed: " + imageHint);
                } else {
                    LOG.debug("hint (image-driven) not available");
                }
            } catch (Throwable igImg) {}

            // Selection logic
            Region first = disableSlideHint ? imageHint : slideHint;
            Region second = disableSlideHint ? slideHint : imageHint;
            if (first != null && second != null) {
                long a1 = (long) first.getW() * (long) first.getH();
                long a2 = (long) second.getW() * (long) second.getH();
                hintRegion = (a1 <= a2) ? first : second;
                LOG.debug("hint selected (preferred order " + (disableSlideHint ? "image,slide" : "slide,image") + ") by smaller area: " + hintRegion);
            } else if (first != null) {
                hintRegion = first;
                LOG.debug("hint selected (preferred): " + hintRegion);
            } else if (second != null) {
                hintRegion = second;
                LOG.debug("hint selected (fallback): " + hintRegion);
            } else {
                LOG.debug("no hint available from either source; proceeding without hint");
            }
        } catch (Throwable ignored) {}
        targetMatch = tryNCCFallback(screenRegion, context.getMinScore(), hintRegion);
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
                LOG.debug(sb.toString());
            } else {
                LOG.debug("SikuliX Settings: <none>");
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

    // Coarse image-driven prescan: downscale both screen and pattern and run a sparse NCC to find a candidate window.
    // Returns a region expanded around the best coarse point where the full-res pattern could fit, or null on failure.
    private Region coarseScanForHint(Region screenRegion, BufferedImage patternImage, double minScore) {
        try {
            if (patternImage == null) return null;
            BufferedImage regionImage = screenRegion.getScreen().capture(screenRegion).getImage();
            int scale = 4; // downscale factor
            int stride = 8; // coarse step in downscaled pixels

            // Downscale region and pattern (nearest-neighbor for speed)
            int rw = Math.max(1, regionImage.getWidth() / scale);
            int rh = Math.max(1, regionImage.getHeight() / scale);
            int pw = Math.max(1, patternImage.getWidth() / scale);
            int ph = Math.max(1, patternImage.getHeight() / scale);
            if (pw >= rw || ph >= rh) return null;

            double[][] R = toGrayscale(resizeNN(regionImage, rw, rh));
            double[][] P = toGrayscale(resizeNN(patternImage, pw, ph));

            // Precompute pattern stats
            double pSum = 0, pSumSq = 0; int pN = pw * ph;
            for (int y = 0; y < ph; y++) {
                for (int x = 0; x < pw; x++) { double v = P[y][x]; pSum += v; pSumSq += v*v; }
            }
            double pMean = pSum / pN; double pVar = Math.max(1e-12, (pSumSq / pN) - pMean * pMean); double pStd = Math.sqrt(pVar);

            double best = -1; int bx = 0, by = 0;
            for (int y = 0; y <= rh - ph; y += stride) {
                for (int x = 0; x <= rw - pw; x += stride) {
                    double rSum = 0, rSumSq = 0;
                    for (int j = 0; j < ph; j++) {
                        for (int i = 0; i < pw; i++) {
                            double rv = R[y + j][x + i];
                            rSum += rv; rSumSq += rv*rv;
                        }
                    }
                    double rMean = rSum / pN; double rVar = Math.max(1e-12, (rSumSq / pN) - rMean*rMean); double rStd = Math.sqrt(rVar);
                    double centeredCross = 0;
                    for (int j = 0; j < ph; j++) {
                        for (int i = 0; i < pw; i++) { centeredCross += (R[y + j][x + i] - rMean) * (P[j][i] - pMean); }
                    }
                    double denom = (pStd * rStd) * pN; double ncc = (denom > 0) ? (centeredCross / denom) : -1.0;
                    if (ncc > best) { best = ncc; bx = x; by = y; }
                }
            }
            // Require a relaxed threshold to accept the coarse hint
            if (best < Math.max(0.0, Math.min(1.0, minScore * 0.8))) return null;

            // Map back to full-res coordinates and expand by pattern size + margin
            int fx = screenRegion.getX() + bx * scale;
            int fy = screenRegion.getY() + by * scale;
            int fw = Math.min(screenRegion.getW(), patternImage.getWidth() * 2 + 200);
            int fh = Math.min(screenRegion.getH(), patternImage.getHeight() * 2 + 200);
            int nx = Math.max(screenRegion.getX(), fx - patternImage.getWidth() / 2 - 100);
            int ny = Math.max(screenRegion.getY(), fy - patternImage.getHeight() / 2 - 100);
            nx = Math.min(nx, screenRegion.getX() + screenRegion.getW() - fw);
            ny = Math.min(ny, screenRegion.getY() + screenRegion.getH() - fh);
            return new Region(nx, ny, fw, fh);
        } catch (Throwable t) {
            return null;
        }
    }

    private static BufferedImage resizeNN(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int sw = src.getWidth(), sh = src.getHeight();
        for (int y = 0; y < h; y++) {
            int sy = Math.min(sh - 1, (int)Math.round(((double)y / Math.max(1, h - 1)) * (sh - 1)));
            for (int x = 0; x < w; x++) {
                int sx = Math.min(sw - 1, (int)Math.round(((double)x / Math.max(1, w - 1)) * (sw - 1)));
                dst.setRGB(x, y, src.getRGB(sx, sy));
            }
        }
        return dst;
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

    // Heuristic: detect red-ish outline color from hex (e.g., FF0000 or #FF0000)
    private boolean isRedLike(String hex) {
        if (hex == null) return false;
        try {
            String s = hex.trim();
            if (s.isEmpty()) return false;
            if (s.startsWith("#")) s = s.substring(1);
            // support short hex like F00
            if (s.length() == 3) {
                s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
            }
            int rgb = (int) Long.parseLong(s, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            // red dominant, green/blue low
            return r >= 180 && g <= 90 && b <= 90 && (r - Math.max(g, b)) >= 60;
        } catch (Throwable t) {
            return false;
        }
    }

    // Derive a prioritized hint region from the slide's target element.
    // Maps slide element bounds (EMU) to screen coordinates, expands with padding, and validates size vs. pattern.
    private Region computeAutoHintRegion(Context context, BufferedImage patternImage) {
        try {
            Slide slide = context.getSlide();
            Region screenRegion = context.getScreenRegion();
            if (slide == null || screenRegion == null) return null;

            // There may be multiple target boxes overlapping the image: prefer a red-outlined box.
            List<SlideElement> targets = Selector.select(slide.getElements()).isTarget().all();
            if (targets == null || targets.isEmpty()) return null;
            SlideElement targetElement = targets.get(0);
            for (SlideElement e : targets) {
                if (isRedLike(e.getLineColor())) {
                    targetElement = e;
                    break;
                }
            }

            int slideW = Math.max(1, slide.getWidth());
            int slideH = Math.max(1, slide.getHeight());
            // If a background picture exists that does not span the full slide, compute
            // mapping relative to that picture's bounds to avoid horizontal/vertical bias.
            SlideElement basis = null;
            try {
                List<SlideElement> imgs = Selector.select(slide.getElements()).isImage().all();
                if (imgs != null && !imgs.isEmpty()) {
                    // pick the largest image by area as the likely background
                    SlideElement best = imgs.get(0);
                    long bestArea = 1L * Math.max(1, best.getCx()) * Math.max(1, best.getCy());
                    for (SlideElement im : imgs) {
                        long area = 1L * Math.max(1, im.getCx()) * Math.max(1, im.getCy());
                        if (area > bestArea) { best = im; bestArea = area; }
                    }
                    basis = best;
                }
            } catch (Throwable igb) {}

            // Log slide mapping inputs to diagnose portrait/landscape mismatches
            try {
                if (basis != null) {
                    LOG.debug(String.format("slide mapping: basis=picture(offx=%d,offy=%d,cx=%d,cy=%d) target(offx=%d,offy=%d,cx=%d,cy=%d) lineColor=%s",
                        basis.getOffx(), basis.getOffy(), basis.getCx(), basis.getCy(),
                        targetElement.getOffx(), targetElement.getOffy(), targetElement.getCx(), targetElement.getCy(),
                        String.valueOf(targetElement.getLineColor())));
                } else {
                    LOG.debug(String.format("slide mapping: slideW=%d slideH=%d elem(offx=%d, offy=%d, cx=%d, cy=%d) lineColor=%s",
                        slideW, slideH, targetElement.getOffx(), targetElement.getOffy(), targetElement.getCx(), targetElement.getCy(), String.valueOf(targetElement.getLineColor())));
                }
                if (isRedLike(targetElement.getLineColor())) {
                    LOG.debug("auto hint: selected RED-outlined target element for hint region");
                }
            } catch (Throwable ig) {}

            double xmin, ymin, xmax, ymax;
            if (basis != null && basis.getCx() > 0 && basis.getCy() > 0) {
                // Map target rect relative to the basis picture rectangle
                double bx = basis.getOffx();
                double by = basis.getOffy();
                double bw = basis.getCx();
                double bh = basis.getCy();
                xmin = (targetElement.getOffx() - bx) / bw;
                ymin = (targetElement.getOffy() - by) / bh;
                xmax = ((targetElement.getOffx() + targetElement.getCx()) - bx) / bw;
                ymax = ((targetElement.getOffy() + targetElement.getCy()) - by) / bh;
                // Clamp
                xmin = Math.max(0.0, Math.min(1.0, xmin));
                ymin = Math.max(0.0, Math.min(1.0, ymin));
                xmax = Math.max(0.0, Math.min(1.0, xmax));
                ymax = Math.max(0.0, Math.min(1.0, ymax));
                LOG.debug(String.format("slide mapping basis=picture -> fractions xmin=%.4f ymin=%.4f xmax=%.4f ymax=%.4f", xmin, ymin, xmax, ymax));
            } else {
                // Fallback: map relative to entire slide canvas
                xmin = Math.max(0.0, Math.min(1.0, (double) targetElement.getOffx() / slideW));
                ymin = Math.max(0.0, Math.min(1.0, (double) targetElement.getOffy() / slideH));
                xmax = Math.max(0.0, Math.min(1.0, (double) (targetElement.getOffx() + targetElement.getCx()) / slideW));
                ymax = Math.max(0.0, Math.min(1.0, (double) (targetElement.getOffy() + targetElement.getCy()) / slideH));
            }

            int baseX = screenRegion.getX() + (int) Math.round(xmin * screenRegion.getW());
            int baseY = screenRegion.getY() + (int) Math.round(ymin * screenRegion.getH());
            int baseW = Math.max(0, (int) Math.round((xmax - xmin) * screenRegion.getW()));
            int baseH = Math.max(0, (int) Math.round((ymax - ymin) * screenRegion.getH()));

            if (baseW <= 0 || baseH <= 0) return null;

            // Padding: expand by 10% of base size, at least 20px, and at least half the pattern size (if available)
            int padX = Math.max(20, (int) Math.round(0.1 * baseW));
            int padY = Math.max(20, (int) Math.round(0.1 * baseH));
            if (patternImage != null) {
                padX = Math.max(padX, patternImage.getWidth() / 2);
                padY = Math.max(padY, patternImage.getHeight() / 2);
            }

            int hx = baseX - padX;
            int hy = baseY - padY;
            int hw = baseW + 2 * padX;
            int hh = baseH + 2 * padY;

            // Constrain to the current screenRegion bounds
            int ix = Math.max(screenRegion.getX(), hx);
            int iy = Math.max(screenRegion.getY(), hy);
            int ix2 = Math.min(screenRegion.getX() + screenRegion.getW(), hx + hw);
            int iy2 = Math.min(screenRegion.getY() + screenRegion.getH(), hy + hh);
            int iw = Math.max(0, ix2 - ix);
            int ih = Math.max(0, iy2 - iy);
            if (iw <= 0 || ih <= 0) return null;

            // Ensure the pattern can fit in the hint region
            if (patternImage != null && (patternImage.getWidth() > iw || patternImage.getHeight() > ih)) {
                // Try to minimally expand within the screenRegion to fit the pattern centered on the base rect
                int needW = Math.max(iw, patternImage.getWidth());
                int needH = Math.max(ih, patternImage.getHeight());
                int cx = baseX + baseW / 2;
                int cy = baseY + baseH / 2;
                int nx = Math.max(screenRegion.getX(), Math.min(cx - needW / 2, screenRegion.getX() + screenRegion.getW() - needW));
                int ny = Math.max(screenRegion.getY(), Math.min(cy - needH / 2, screenRegion.getY() + screenRegion.getH() - needH));
                iw = Math.max(0, Math.min(needW, screenRegion.getX() + screenRegion.getW() - nx));
                ih = Math.max(0, Math.min(needH, screenRegion.getY() + screenRegion.getH() - ny));
                ix = nx;
                iy = ny;
                if (patternImage.getWidth() > iw || patternImage.getHeight() > ih) {
                    // still cannot fit
                    return null;
                }
            }
            // Save a visual of the final slide-derived hint region for diagnostics
            try {
                File outDir = new File("target/debug/hints");
                if (!outDir.exists()) outDir.mkdirs();
                BufferedImage cap = screenRegion.getScreen().capture(new Region(ix, iy, iw, ih)).getImage();
                String pref = (debugSlideNum > 0 ? String.format("s%03d_", debugSlideNum) : "");
                File f = new File(outDir, String.format(pref + "hint_slide_%d_%d_%dx%d.png", ix, iy, iw, ih));
                ImageIO.write(cap, "png", f);
            } catch (Throwable ig2) {}
            return new Region(ix, iy, iw, ih);
        } catch (Throwable t) {
            return null;
        }
    }

    private Match tryNCCFallback(Region screenRegion, double minScore, Region hintRegion) {
        try {
            LOG.debug("NCC fallback: starting custom pattern matching");
            BufferedImage patternImage = getPattern().getImage().get();

            // If provided, intersect hint with screenRegion and ensure it can contain the pattern
            Region prioritized = null;
            if (hintRegion != null) {
                int ix = Math.max(screenRegion.getX(), hintRegion.getX());
                int iy = Math.max(screenRegion.getY(), hintRegion.getY());
                int ix2 = Math.min(screenRegion.getX() + screenRegion.getW(), hintRegion.getX() + hintRegion.getW());
                int iy2 = Math.min(screenRegion.getY() + screenRegion.getH(), hintRegion.getY() + hintRegion.getH());
                int iw = Math.max(0, ix2 - ix);
                int ih = Math.max(0, iy2 - iy);
                if (iw > 0 && ih > 0) {
                    prioritized = new Region(ix, iy, iw, ih);
                    // pattern must fit
                    if (patternImage.getWidth() > iw || patternImage.getHeight() > ih) {
                        LOG.info("hint region too small for pattern; skipping hint");
                        prioritized = null;
                    }
                } else {
                    LOG.info("hint region does not intersect search region; skipping hint");
                }
            }

            // 1) Try prioritized hint region first
            if (prioritized != null) {
                LOG.debug("NCC fallback: trying hint region first: " + prioritized);
                // Save prioritized (intersected) hint capture
                try {
                    File outDir = new File("target/debug/hints");
                    if (!outDir.exists()) outDir.mkdirs();
                    BufferedImage cap = prioritized.getScreen().capture(prioritized).getImage();
                    String pref = (debugSlideNum > 0 ? String.format("s%03d_", debugSlideNum) : "");
                    File f = new File(outDir, String.format(pref + "hint_prioritized_%d_%d_%dx%d.png", prioritized.getX(), prioritized.getY(), prioritized.getW(), prioritized.getH()));
                    ImageIO.write(cap, "png", f);
                } catch (Throwable ig0) {}

                // Pre-wait loop: keep trying within the prioritized hint region before any expansion
                long budgetMs = getLongProp("slides.hint.prewait.ms", 3000L);
                long intervalMs = Math.max(100L, Math.min(2000L, getLongProp("slides.hint.prewait.interval.ms", 300L)));
                long start = System.nanoTime();
                int attempt = 0;
                while (true) {
                    attempt++;
                    BufferedImage hintImage = prioritized.getScreen().capture(prioritized).getImage();
                    NCCResult r = findWithNCC(hintImage, patternImage, minScore);
                    if (r != null) {
                        int centerX = prioritized.getX() + r.x + patternImage.getWidth() / 2;
                        int centerY = prioritized.getY() + r.y + patternImage.getHeight() / 2;
                        Match match = new Match(new Region(centerX - patternImage.getWidth()/2, centerY - patternImage.getHeight()/2,
                                                           patternImage.getWidth(), patternImage.getHeight()), r.ncc);
                        match.setTarget(centerX, centerY);
                        LOG.info("NCC fallback: found in hint region at " + match.getTarget() + " score=" + match.getScore());
                        return match;
                    }
                    long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                    LOG.debug("NCC fallback: hint pre-wait attempt " + attempt + " no match (>= " + minScore + ") elapsed=" + elapsedMs + "ms of " + budgetMs + "ms");
                    if (elapsedMs >= budgetMs) {
                        LOG.debug("NCC fallback: no match in hint region after pre-wait (attempts=" + attempt + ", elapsed=" + elapsedMs + "ms); proceeding to expansion");
                        break;
                    }
                    sleepMs(intervalMs);
                }

                // 1b) Expand hint region once and retry, to tolerate slight mapping errors
                try {
                    int growX = Math.max(patternImage.getWidth(), 100);
                    int growY = Math.max(patternImage.getHeight(), 100);
                    int nx = Math.max(screenRegion.getX(), prioritized.getX() - growX);
                    int ny = Math.max(screenRegion.getY(), prioritized.getY() - growY);
                    int nx2 = Math.min(screenRegion.getX() + screenRegion.getW(), prioritized.getX() + prioritized.getW() + growX);
                    int ny2 = Math.min(screenRegion.getY() + screenRegion.getH(), prioritized.getY() + prioritized.getH() + growY);
                    int nw = Math.max(0, nx2 - nx);
                    int nh = Math.max(0, ny2 - ny);
                    boolean fits = nw >= patternImage.getWidth() && nh >= patternImage.getHeight();
                    if (nw > 0 && nh > 0 && fits) {
                        Region expanded = new Region(nx, ny, nw, nh);
                        LOG.debug("NCC fallback: expanding hint region and retrying: " + expanded);
                        try {
                            File outDir = new File("target/debug/hints");
                            if (!outDir.exists()) outDir.mkdirs();
                            BufferedImage cap2 = expanded.getScreen().capture(expanded).getImage();
                            String pref = (debugSlideNum > 0 ? String.format("s%03d_", debugSlideNum) : "");
                            File f2 = new File(outDir, String.format(pref + "hint_expanded_%d_%d_%dx%d.png", expanded.getX(), expanded.getY(), expanded.getW(), expanded.getH()));
                            ImageIO.write(cap2, "png", f2);
                        } catch (Throwable ig1) {}
                        BufferedImage hintImage2 = expanded.getScreen().capture(expanded).getImage();
                        NCCResult r2 = findWithNCC(hintImage2, patternImage, minScore);
                        if (r2 != null) {
                            int centerX = expanded.getX() + r2.x + patternImage.getWidth() / 2;
                            int centerY = expanded.getY() + r2.y + patternImage.getHeight() / 2;
                            Match match = new Match(new Region(centerX - patternImage.getWidth()/2, centerY - patternImage.getHeight()/2,
                                                               patternImage.getWidth(), patternImage.getHeight()), r2.ncc);
                            match.setTarget(centerX, centerY);
                            LOG.info("NCC fallback: found in expanded hint region at " + match.getTarget() + " score=" + match.getScore());
                            return match;
                        }
                        LOG.debug("NCC fallback: no match in expanded hint region");
                    }
                } catch (Throwable igExp) {
                    // best effort expansion
                }
            }

            // 2) If no hint or hint failed, try an image-driven coarse scan to propose a candidate region
            try {
                Region coarse = coarseScanForHint(screenRegion, patternImage, minScore);
                if (coarse != null) {
                    LOG.info("NCC fallback: trying coarse image-driven hint: " + coarse);
                    try {
                        File outDir = new File("target/debug/hints");
                        if (!outDir.exists()) outDir.mkdirs();
                        BufferedImage cap3 = coarse.getScreen().capture(coarse).getImage();
                        String pref = (debugSlideNum > 0 ? String.format("s%03d_", debugSlideNum) : "");
                        File f3 = new File(outDir, String.format(pref + "hint_coarse_%d_%d_%dx%d.png", coarse.getX(), coarse.getY(), coarse.getW(), coarse.getH()));
                        ImageIO.write(cap3, "png", f3);
                    } catch (Throwable ig3) {}
                    BufferedImage hintImg3 = coarse.getScreen().capture(coarse).getImage();
                    NCCResult r3 = findWithNCC(hintImg3, patternImage, minScore);
                    if (r3 != null) {
                        int centerX = coarse.getX() + r3.x + patternImage.getWidth() / 2;
                        int centerY = coarse.getY() + r3.y + patternImage.getHeight() / 2;
                        Match match = new Match(new Region(centerX - patternImage.getWidth()/2, centerY - patternImage.getHeight()/2,
                                                           patternImage.getWidth(), patternImage.getHeight()), r3.ncc);
                        match.setTarget(centerX, centerY);
                        LOG.info("NCC fallback: found in coarse image-driven hint at " + match.getTarget() + " score=" + match.getScore());
                        return match;
                    }
                    LOG.debug("NCC fallback: no match in coarse image-driven hint region");
                }
            } catch (Throwable igCoarse) {
                // best effort
            }

            // 3) Fallback to full region
            LOG.info("NCC fallback: switching to full-region normal match");
            BufferedImage regionImage = screenRegion.getScreen().capture(screenRegion).getImage();
            NCCResult nccResult = findWithNCC(regionImage, patternImage, minScore);
            if (nccResult != null) {
                LOG.info("NCC fallback: found match with score=" + nccResult.ncc + " at (" + nccResult.x + "," + nccResult.y + ")");
                int centerX = screenRegion.getX() + nccResult.x + patternImage.getWidth() / 2;
                int centerY = screenRegion.getY() + nccResult.y + patternImage.getHeight() / 2;
                Match match = new Match(new Region(centerX - patternImage.getWidth()/2, centerY - patternImage.getHeight()/2,
                                                  patternImage.getWidth(), patternImage.getHeight()), nccResult.ncc);
                match.setTarget(centerX, centerY);
                LOG.info("NCC fallback: created match at " + match.getTarget() + " score=" + match.getScore());
                return match;
            }

            LOG.debug("NCC fallback: no match found above threshold " + minScore);
            return null;
        } catch (Throwable ex) {
            LOG.warn("NCC fallback failed: " + ex.getMessage());
            return null;
        }
    }

    // Accepts Boolean TRUE, or strings: true/1/yes/y (case-insensitive)
    private boolean isTruthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        String s = String.valueOf(v).trim().toLowerCase();
        return s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("y");
    }

    private long getLongProp(String key, long defVal) {
        try {
            String s = System.getProperty(key);
            if (s == null || s.trim().isEmpty()) return defVal;
            long v = Long.parseLong(s.trim());
            return v > 0 ? v : defVal;
        } catch (Throwable t) {
            return defVal;
        }
    }

    private void sleepMs(long ms) {
        try {
            Thread.sleep(Math.max(0L, ms));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
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