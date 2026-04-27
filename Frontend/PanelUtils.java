package Frontend;

import Backend.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Static helpers shared by every game panel.
 * Call these instead of duplicating painting/loading code.
 */
public final class PanelUtils {

    private PanelUtils() {}   // utility class, no instances

    // ── Turn-order indicators ─────────────────────────────────────────────────
    // Three small translucent rectangles over the scoring track in the top-left
    // of the board image.  Each is coloured with the matching player's colour.
    // Coordinates taken directly from the game board pixel positions supplied.

    private static final int[] IND_X = { 59,  93, 128 };
    private static final int[] IND_Y = { 22,  22,  22 };
    private static final int[] IND_W = { 19,  20,  18 };
    private static final int   IND_H = 12;
    private static final float IND_ALPHA = 0.55f;   // transparent enough to see numbers

    /**
     * Paints one small translucent coloured rectangle for each player,
     * positioned in turn order (index 0 = first player).
     */
    public static void paintTurnOrderIndicators(Graphics g, RoundManager rm) {
        if (rm == null) return;
        List<Player> order = rm.getTurnOrder();
        Graphics2D g2 = (Graphics2D) g;
        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, IND_ALPHA));

        for (int i = 0; i < Math.min(order.size(), IND_X.length); i++) {
            g2.setColor(parseColor(order.get(i).getColor()));
            g2.fillRect(IND_X[i], IND_Y[i], IND_W[i], IND_H);
        }

        g2.setComposite(original);
    }

    // ── Hand area ─────────────────────────────────────────────────────────────

    /**
     * Draws the full player hand: colour oval, name, money, powerplant cards,
     * and the resource inventory squares.
     * Panels that need a custom plant overlay (e.g. BureaucracyPanel) should call
     * {@link #paintPlayerInfo} + {@link #paintInventory} separately.
     */
    public static void paintHand(Graphics g, Player player,
                                 Map<Integer, BufferedImage> plantImageCache) {
        if (player == null) return;
        paintPlayerInfo(g, player);
        paintPlants(g, player, plantImageCache);
        paintInventory(g, player);
    }

    /**
     * Colour oval, player name, and money — no plants or inventory.
     */
    public static void paintPlayerInfo(Graphics g, Player player) {
        if (player == null) return;

        // Colour oval (bottom-right corner)
        g.setColor(parseColor(player.getColor()));
        g.fillOval(1460, 695, 120, 120);

        // Name
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString(player.getName(), 1180, 700);

        // Money
        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.setColor(Color.WHITE);
        g.drawString("$" + player.getMoney(), 1480, 770);
    }

    /**
     * Draws up to 3 powerplant cards for the player, using cached images.
     * Falls back to a labelled rectangle if the image is missing.
     */
    public static void paintPlants(Graphics g, Player player,
                                   Map<Integer, BufferedImage> plantImageCache) {
        if (player == null) return;
        List<Powerplant> plants = new ArrayList<>(player.getPowerplants());
        int px = 900;
        for (int i = 0; i < Math.min(plants.size(), 3); i++) {
            int plantNum = plants.get(i).getNumber();
            BufferedImage img = getPlantImage(plantNum, plantImageCache);
            if (img != null) {
                g.drawImage(img, px, 770, 180, 180, null);
            } else {
                g.setColor(new Color(60, 60, 80));
                g.fillRect(px, 770, 180, 180);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 28));
                g.drawString("#" + plantNum, px + 55, 870);
            }
            px += 185;
        }
    }

    /**
     * Draws the four resource-type colour squares and their counts,
     * pulled from the player's stored resources across all plants.
     */
    public static void paintInventory(Graphics g, Player player) {
        if (player == null) return;
        Map<ResourceType, Integer> stored;
        try {
            stored = player.getStoredResources();
        } catch (Exception ex) {
            stored = Collections.emptyMap();
        }

        // Colour swatches
        g.setColor(new Color(92, 50, 5));  g.fillRect(1475, 832, 25, 25); // coal
        g.setColor(Color.BLACK);           g.fillRect(1475, 862, 25, 25); // oil
        g.setColor(Color.YELLOW);          g.fillRect(1475, 892, 25, 25); // trash
        g.setColor(Color.RED);             g.fillRect(1475, 922, 25, 25); // uranium

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(stored.getOrDefault(ResourceType.COAL,    0)), 1510, 850);
        g.drawString(String.valueOf(stored.getOrDefault(ResourceType.OIL,     0)), 1510, 880);
        g.drawString(String.valueOf(stored.getOrDefault(ResourceType.TRASH,   0)), 1510, 910);
        g.drawString(String.valueOf(stored.getOrDefault(ResourceType.URANIUM, 0)), 1510, 940);
    }

    // ── Resource market icons ─────────────────────────────────────────────────

    /**
     * Draws the resource tokens on the board image.
     * Pixel positions match the game board exactly.
     */
    public static void paintResourceIcons(Graphics g, ResourceMarket market) {
        if (market == null) return;

        int coalCount    = market.getAvailableAmount(ResourceType.COAL);
        int oilCount     = market.getAvailableAmount(ResourceType.OIL);
        int trashCount   = market.getAvailableAmount(ResourceType.TRASH);
        int uraniumCount = market.getAvailableAmount(ResourceType.URANIUM);

        // --- COAL ---
        int x = 769, y = 902;
        for (int i = 0; i < coalCount; i++) {
            g.setColor(new Color(99, 68, 38));
            g.fillRect(x, y, 18, 12);
            switch (i) {
                case 2  -> x = 671;
                case 5  -> x = 572;
                case 8  -> x = 474;
                case 11 -> x = 376;
                case 14 -> { x = 276; y = 906; }
                case 17 -> x = 180;
                case 20 -> x = 81;
                default -> x -= 28;
            }
        }

        // --- OIL ---
        x = 753; y = 922;
        for (int i = 0; i < oilCount; i++) {
            g.setColor(new Color(12, 37, 48));
            g.fillRect(x, y, 12, 11);
            switch (i) {
                case 2  -> { x = 655; y = 924; }
                case 5  -> x = 556;
                case 8  -> x = 458;
                case 11 -> x = 359;
                case 14 -> x = 260;
                case 17 -> x = 160;
                case 20 -> x = 63;
                default -> x -= 21;
            }
        }

        // --- TRASH ---
        x = 769; y = 940;
        for (int i = 0; i < trashCount; i++) {
            g.setColor(new Color(245, 213, 84));
            g.fillRect(x, y, 18, 12);
            switch (i) {
                case 2  -> x = 671;
                case 5  -> x = 572;
                case 8  -> x = 474;
                case 11 -> x = 376;
                case 14 -> { x = 276; y = 945; }
                case 17 -> x = 180;
                case 20 -> x = 81;
                default -> x -= 28;
            }
        }

        // --- URANIUM ---
        x = 848; y = 936;
        int w = 16, h = 14;
        for (int i = 0; i < uraniumCount; i++) {
            g.setColor(new Color(213, 82, 68));
            g.fillRect(x, y, w, h);
            switch (i) {
                case 0 -> { x = 808; y = 937; }
                case 1 -> { x = 848; y = 909; }
                case 2 -> { x = 809; y = 902; }
                case 3 -> { x = 774; y = 925; w = 13; h = 10; }
                default -> x -= 98;
            }
        }
    }

    // ── Image loading ─────────────────────────────────────────────────────────

    /**
     * Loads and caches a powerplant image by card number.
     * Stores null on failure so the panel can fall back without retrying.
     */
    public static BufferedImage getPlantImage(int number, Map<Integer, BufferedImage> cache) {
        if (cache.containsKey(number)) return cache.get(number);
        BufferedImage img = null;
        try {
            img = ImageIO.read(PanelUtils.class.getResource("/Images/powerplant-" + number + ".png"));
        } catch (Exception e) {
            System.err.println("PanelUtils: no image for powerplant-" + number);
        }
        cache.put(number, img);
        return img;
    }

    // ── Colour helper ─────────────────────────────────────────────────────────

    public static Color parseColor(String colorName) {
        if (colorName == null) return Color.LIGHT_GRAY;
        return switch (colorName.toLowerCase()) {
            case "red"    -> new Color(220, 60,  60);
            case "blue"   -> new Color(60,  100, 200);
            case "green"  -> new Color(60,  160, 80);
            case "yellow" -> new Color(220, 190, 50);
            case "purple" -> new Color(130, 60,  180);
            case "teal"   -> new Color(150, 196, 188);
            case "brown"  -> new Color(182, 132, 105);
            case "black"  -> Color.DARK_GRAY;
            default       -> Color.LIGHT_GRAY;
        };
    }
}