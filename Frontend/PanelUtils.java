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

    private static final int[] IND_X = { 59,  93, 128 };
    private static final int[] IND_Y = { 22,  22,  22 };
    private static final int[] IND_W = { 19,  20,  18 };
    private static final int   IND_H = 12;
    private static final float IND_ALPHA = 0.55f;

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

    // ── Cities and Board Tracker ──────────────────────────────────────────────

    /**
     * Paints all owned cities on the map and the active player's city-count tracker.
     * The tracker is only drawn for the player currently in the building phase.
     */
    /**
     * Paints all owned cities on the map and the city-count tracker for EVERY player.
     * Markers on the tracker will overlap if players have the same number of cities.
     */
    public static void paintCities(Graphics g, RoundManager rm) {
        if (rm == null) return;

        for (Player p : rm.getPlayers()) {
            Color c = parseColor(p.getColor());

            // 1. Paint cities on the map for this player
            for (City city : p.getOwnedCities()) {
                Point pt = Constants.CityCoordinates.coordinates.get(city.getName());
                if (pt == null) continue;

                g.setColor(c);
                g.fillRect(pt.x - 10, pt.y - 25, 20, 20);
            }

            // 2. Paint this player's marker on the city-count tracker
            int count = p.getCityCount();
            g.setColor(c);

            if (count > 0 && count <= 7) {
                // First row of the tracker
                g.fillRect(410 + count * 65, 16, 20, 20);
            } else if (count > 7) {
                // Second row of the tracker
                g.fillRect(410 + (count - 7) * 32, 47, 20, 20);
            }
        }
    }

    // ── Hand area ─────────────────────────────────────────────────────────────

    public static void paintHand(Graphics g, Player player,
                                 Map<Integer, BufferedImage> plantImageCache) {
        if (player == null) return;
        paintPlayerInfo(g, player);
        paintPlants(g, player, plantImageCache);
        paintInventory(g, player);
    }

    public static void paintPlayerInfo(Graphics g, Player player) {
        if (player == null) return;

        g.setColor(parseColor(player.getColor()));
        g.fillOval(1460, 695, 120, 120);

        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString(player.getName(), 1180, 700);

        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.setColor(Color.WHITE);
        g.drawString("$" + player.getMoney(), 1480, 770);
    }

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

    public static void paintInventory(Graphics g, Player player) {
        if (player == null) return;
        Map<ResourceType, Integer> stored;
        try {
            stored = player.getStoredResources();
        } catch (Exception ex) {
            stored = Collections.emptyMap();
        }

        g.setColor(new Color(92, 50, 5));  g.fillRect(1475, 832, 25, 25);
        g.setColor(Color.BLACK);           g.fillRect(1475, 862, 25, 25);
        g.setColor(Color.YELLOW);          g.fillRect(1475, 892, 25, 25);
        g.setColor(Color.RED);             g.fillRect(1475, 922, 25, 25);

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(stored.getOrDefault(ResourceType.COAL,    0)), 1510, 850);
        g.drawString(String.valueOf(stored.getOrDefault(ResourceType.OIL,     0)), 1510, 880);
        g.drawString(String.valueOf(stored.getOrDefault(ResourceType.TRASH,   0)), 1510, 910);
        g.drawString(String.valueOf(stored.getOrDefault(ResourceType.URANIUM, 0)), 1510, 940);
    }

    // ── Resource market icons ─────────────────────────────────────────────────

    public static void paintResourceIcons(Graphics g, ResourceMarket market) {
        if (market == null) return;

        int coalCount    = market.getAvailableAmount(ResourceType.COAL);
        int oilCount     = market.getAvailableAmount(ResourceType.OIL);
        int trashCount   = market.getAvailableAmount(ResourceType.TRASH);
        int uraniumCount = market.getAvailableAmount(ResourceType.URANIUM);

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