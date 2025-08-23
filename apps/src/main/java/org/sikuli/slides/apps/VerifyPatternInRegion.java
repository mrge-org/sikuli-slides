package org.sikuli.slides.apps;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Minimal, dependency-free verifier for checking if a saved pattern exists inside a saved region image.
 *
 * Constraints:
 * - No scaling, rotation, or fancy preprocessing.
 * - Strict 1:1 sliding-window search.
 * - Reports both SSD (lower is better) and NCC ([-1..1], higher is better) for the best location.
 *
 * Usage:
 *   java -cp apps/target/classes org.sikuli.slides.apps.VerifyPatternInRegion \
 *        /path/to/region.png /path/to/pattern.png [out-visual.png]
 *
 * Notes:
 * - If an output path is provided, the tool saves a copy of the region with a red rectangle where the best match was found.
 */
public class VerifyPatternInRegion {

    private static final Logger LOG = Logger.getLogger(VerifyPatternInRegion.class);

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            LOG.info("Usage: VerifyPatternInRegion <region.png> <pattern.png> [out-visual.png]");
            System.exit(2);
        }

        File regionFile = new File(args[0]);
        File patternFile = new File(args[1]);
        String outPath = args.length == 3 ? args[2] : null;

        BufferedImage region = readImage(regionFile);
        BufferedImage pattern = readImage(patternFile);

        if (pattern.getWidth() > region.getWidth() || pattern.getHeight() > region.getHeight()) {
            LOG.error("Pattern larger than region. Aborting.");
            System.exit(1);
        }

        // Basic info
        LOG.info(String.format("Starting strict 1:1 match...%n  region=%s (%dx%d)%n  pattern=%s (%dx%d)",
                regionFile.getAbsolutePath(), region.getWidth(), region.getHeight(),
                patternFile.getAbsolutePath(), pattern.getWidth(), pattern.getHeight()));

        // Convert to grayscale double arrays for simple, deterministic math
        double[][] R = toGrayscale(region);
        double[][] P = toGrayscale(pattern);

        long t0 = System.nanoTime();
        Result best = search(R, P);
        long t1 = System.nanoTime();

        LOG.info(String.format("Region: %s (%dx%d)", regionFile.getAbsolutePath(), region.getWidth(), region.getHeight()));
        LOG.info(String.format("Pattern: %s (%dx%d)", patternFile.getAbsolutePath(), pattern.getWidth(), pattern.getHeight()));
        LOG.info(String.format("Best @ (%d,%d)", best.x, best.y));
        LOG.info(String.format("  SSD: %f (lower is better)", best.ssd));
        LOG.info(String.format("  NCC: %f (higher is better, max=1.0)", best.ncc));
        LOG.info(String.format("Elapsed: %.3f s", (t1 - t0) / 1e9));

        if (outPath != null) {
            BufferedImage vis = deepCopy(region);
            Graphics2D g = vis.createGraphics();
            try {
                g.setColor(Color.YELLOW);
                g.setStroke(new BasicStroke(2f));
                g.drawRect(best.x, best.y, pattern.getWidth() - 1, pattern.getHeight() - 1);
            } finally {
                g.dispose();
            }
            ImageIO.write(vis, extFrom(outPath), new File(outPath));
            LOG.info("Visualization written: " + outPath);
        }
    }

    private static BufferedImage readImage(File f) throws IOException {
        BufferedImage img = ImageIO.read(f);
        if (img == null) throw new IOException("Unable to read image: " + f);
        if (img.getType() != BufferedImage.TYPE_INT_ARGB && img.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = copy.createGraphics();
            try { g.drawImage(img, 0, 0, null); } finally { g.dispose(); }
            return copy;
        }
        return img;
    }

    private static String extFrom(String path) {
        int i = path.lastIndexOf('.') ;
        return (i > 0 && i < path.length() - 1) ? path.substring(i + 1) : "png";
    }

    private static BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage copy = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        try { g.drawImage(bi, 0, 0, null); } finally { g.dispose(); }
        return copy;
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

    private static Result search(double[][] R, double[][] P) {
        int RH = R.length, RW = R[0].length;
        int PH = P.length, PW = P[0].length;

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

        double bestSSD = Double.POSITIVE_INFINITY;
        double bestNCC = -2.0; // NCC in [-1,1]
        int bestX = 0, bestY = 0;

        int totalRows = RH - PH + 1;
        int progressStep = Math.max(1, totalRows / 20); // ~5% steps
        long lastLog = System.nanoTime();
        for (int y = 0; y <= RH - PH; y++) {
            for (int x = 0; x <= RW - PW; x++) {
                double rSum = 0, rSumSq = 0;
                double ssd = 0;
                for (int j = 0; j < PH; j++) {
                    for (int i = 0; i < PW; i++) {
                        double rv = R[y + j][x + i];
                        double pv = P[j][i];
                        double d = rv - pv;
                        ssd += d * d;
                        rSum += rv;
                        rSumSq += rv * rv;
                    }
                }
                if (ssd < bestSSD) {
                    bestSSD = ssd;
                    bestX = x; bestY = y;
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
                    // prefer NCC for location if it conflicts with SSD
                    bestX = x; bestY = y;
                }
            }
        }

        Result r = new Result();
        r.x = bestX; r.y = bestY; r.ssd = bestSSD; r.ncc = bestNCC;
        return r;
    }

    private static class Result { int x, y; double ssd, ncc; }
}
