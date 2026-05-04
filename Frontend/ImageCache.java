package Frontend;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Application-wide image cache.
 *
 * Every BufferedImage is loaded at most once per JVM session and then reused
 * by every panel that needs it.  This eliminates the primary memory and
 * performance problem of each panel holding its own copy of every powerplant
 * image and the background.
 *
 * Usage:
 *   BufferedImage bg    = ImageCache.getBackground();
 *   BufferedImage plant = ImageCache.getPlant(3);          // powerplant-3.png
 *   BufferedImage small = ImageCache.getPlantScaled(3, 100, 100); // pre-scaled copy
 *
 * All methods are safe to call from the Event Dispatch Thread.
 * Scaling uses BILINEAR (fast, looks good at game card sizes).
 */
public final class ImageCache {

    private ImageCache() {}

    // ── Backing stores ────────────────────────────────────────────────────────

    /** Full-size plant images, keyed by card number. */
    private static final Map<Integer, BufferedImage> PLANTS = new HashMap<>();

    /**
     * Pre-scaled plant images, keyed by "number_WxH".
     * Populated lazily the first time a particular size is requested.
     */
    private static final Map<String, BufferedImage> PLANTS_SCALED = new HashMap<>();

    /** Shared background image (loaded once). */
    private static BufferedImage background;

    /**STEP 3 CARD**/
    private static BufferedImage step3Card;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the shared background image, loading it the first time.
     * Returns null (and prints a warning) if the resource cannot be found.
     */
    public static BufferedImage getBackground() {
        if (background == null) {
            background = load("/Images/background.png");
        }
        return background;
    }

    /**
     * Returns the full-size image for the given powerplant card number.
     * Returns null if the image resource is missing.
     */
    public static BufferedImage getPlant(int number) {
        return PLANTS.computeIfAbsent(number, n -> load("/Images/powerplant-" + n + ".png"));
    }

    /**
     * Returns a pre-scaled copy of the plant image at exactly {@code w}×{@code h} pixels.
     *
     * The scaled copy is cached so repeated calls with the same arguments are free.
     * Scaling uses BILINEAR interpolation — fast and sharp enough for card-sized art.
     *
     * Returns null if the source image cannot be loaded.
     */
    public static BufferedImage getPlantScaled(int number, int w, int h) {
        String key = number + "_" + w + "x" + h;
        return PLANTS_SCALED.computeIfAbsent(key, k -> {
            BufferedImage src = getPlant(number);
            if (src == null) return null;
            BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = dst.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(src, 0, 0, w, h, null);
            g2.dispose();
            return dst;
        });
    }

    // ── Internal loader ───────────────────────────────────────────────────────

    /**
     * Loads a classpath resource into a BufferedImage.
     * Returns null and logs a warning on failure instead of throwing.
     */
    private static BufferedImage load(String path) {
        try {
            BufferedImage img = ImageIO.read(ImageCache.class.getResource(path));
            if (img == null) {
                System.err.println("ImageCache: ImageIO returned null for " + path);
            }
            return img;
        } catch (Exception e) {
            System.err.println("ImageCache: could not load " + path + " — " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the Step 3 card image.
     */
    public static BufferedImage getStep3Card() {
        if (step3Card == null) {
            step3Card = load("/Images/step3.png");
        }
        return step3Card;
    }
}