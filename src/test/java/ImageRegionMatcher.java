import org.sikuli.script.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Test-side utility that waits, captures a region, matches a template image inside it, and returns the
 * center coordinates of the found match in absolute screen coordinates.
 *
 * This class is in the default package and can be run in isolation.
 */
public class ImageRegionMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(ImageRegionMatcher.class);

    public static Location waitCaptureMatchReturnCenter(String imagePath,
                                                        int x, int y, int w, int h,
                                                        double sleepSec,
                                                        double similarity) throws FindFailed {
        if (imagePath == null || imagePath.isEmpty()) {
            throw new IllegalArgumentException("imagePath is null or empty");
        }
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Region width/height must be positive");
        }
        if (sleepSec < 0) {
            throw new IllegalArgumentException("sleepSec must be >= 0");
        }

        try {
            Thread.sleep((long) (sleepSec * 1000));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        Screen screen = new Screen();
        Region region = new Region(x, y, w, h);

        ScreenImage shot = screen.capture(region);
        BufferedImage img = shot.getImage();

        Finder finder = new Finder(img);
        Pattern pattern = new Pattern(imagePath);
        if (similarity >= 0.0) {
            pattern = pattern.similar((float) similarity);
        } else {
            pattern = pattern.similar(0.7f);
        }

        finder.find(pattern);
        if (!finder.hasNext()) {
            throw new FindFailed("No match found for pattern: " + imagePath +
                    " in region [" + x + "," + y + "," + w + "," + h + "]");
        }
        Match m = finder.next();

        int centerX = x + m.getX() + m.getW() / 2;
        int centerY = y + m.getY() + m.getH() / 2;
        return new Location(centerX, centerY);
    }

    public static void main(String[] args) {
        // Hardcoded per request
        String imagePath = "/Users/gpetrov/mirror_src/sikuli-slides-PoC/target/failed-patterns/slide-1-pattern.png";
        int x = 6, y = 126, w = 830, h = 1830;
        double sleepSec = 3.0;
        double similarity = -1.0; // use default 0.7
        // Enhanced options
        int hintX = 166, hintY = 411, hintW = 325, hintH = 378;
        double timeoutSec = 15.0;
        int screenIndex = 2;

        try {
            if (!new File(imagePath).isFile()) {
                LOG.error("Image file does not exist: {}", imagePath);
                System.exit(2);
            }
            LOG.info("Calling basic method: waitCaptureMatchReturnCenter");
            long startBasicNs = System.nanoTime();
            Location center = waitCaptureMatchReturnCenter(imagePath, x, y, w, h, sleepSec, similarity);
            long elapsedBasicMs = (System.nanoTime() - startBasicNs) / 1_000_000L;
            LOG.info(String.format("Basic method result: %d,%d (%d ms)", center.getX(), center.getY(), elapsedBasicMs));

            LOG.info("Calling enhanced method: waitCaptureMatchReturnCenterWithOptions (hint, timeout, screen)");
            long startEnhNs = System.nanoTime();
            Location center2 = waitCaptureMatchReturnCenterWithOptions(
                    imagePath, x, y, w, h, sleepSec, similarity,
                    hintX, hintY, hintW, hintH,
                    timeoutSec, screenIndex
            );
            long elapsedEnhMs = (System.nanoTime() - startEnhNs) / 1_000_000L;
            LOG.info(String.format("Enhanced method result: %d,%d (%d ms)", center2.getX(), center2.getY(), elapsedEnhMs));
        } catch (FindFailed ff) {
            LOG.error("Match not found: {}", ff.getMessage());
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Enhanced variant that accepts an optional hint region, a timeout window, and selects a specific screen.
     * Attempts to locate the image within the hint region first, then falls back to the main region.
     * Repeats until timeout expires.
     */
    public static Location waitCaptureMatchReturnCenterWithOptions(String imagePath,
                                                                  int x, int y, int w, int h,
                                                                  double sleepSec,
                                                                  double similarity,
                                                                  int hintX, int hintY, int hintW, int hintH,
                                                                  double timeoutSec,
                                                                  int screenIndex) throws FindFailed {
        if (imagePath == null || imagePath.isEmpty()) {
            throw new IllegalArgumentException("imagePath is null or empty");
        }
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Region width/height must be positive");
        }
        if (sleepSec < 0 || timeoutSec < 0) {
            throw new IllegalArgumentException("sleepSec and timeoutSec must be >= 0");
        }

        // Optional initial sleep
        try {
            if (sleepSec > 0) Thread.sleep((long) (sleepSec * 1000));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        Screen screen;
        try {
            screen = new Screen(screenIndex);
        } catch (Exception e) {
            LOG.warn("Invalid screen index {} - defaulting to primary screen", screenIndex);
            screen = new Screen();
        }

        Region mainRegion = new Region(x, y, w, h);
        Region hintRegion = (hintW > 0 && hintH > 0) ? new Region(hintX, hintY, hintW, hintH) : null;

        Pattern pattern = new Pattern(imagePath).similar(similarity >= 0.0 ? (float) similarity : 0.7f);

        long deadline = System.currentTimeMillis() + (long) (timeoutSec * 1000);
        long intervalMs = 400; // polling interval

        while (true) {
            // 1) Try hint region first if provided
            if (hintRegion != null) {
                Location loc = tryFindInRegion(screen, hintRegion, pattern);
                if (loc != null) return loc;
            }

            // 2) Try main region
            Location loc = tryFindInRegion(screen, mainRegion, pattern);
            if (loc != null) return loc;

            if (System.currentTimeMillis() >= deadline) {
                break;
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new FindFailed("Timed out (" + timeoutSec + "s) without finding pattern in hint/main regions");
    }

    private static Location tryFindInRegion(Screen screen, Region region, Pattern pattern) {
        try {
            ScreenImage shot = screen.capture(region);
            BufferedImage img = shot.getImage();
            Finder finder = new Finder(img);
            finder.find(pattern);
            if (finder.hasNext()) {
                Match m = finder.next();
                int centerX = region.getX() + m.getX() + m.getW() / 2;
                int centerY = region.getY() + m.getY() + m.getH() / 2;
                return new Location(centerX, centerY);
            }
        } catch (Exception e) {
            LOG.debug(String.format("Search attempt failed in region [%d , %d , %d x %d ]: %s",
                    region.getX(), region.getY(), region.getW(), region.getH(), e.toString()));
        }
        return null;
    }
}
