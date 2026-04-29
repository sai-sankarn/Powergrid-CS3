package Frontend;

import Backend.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Phase 3 – Buy Resources screen.
 *
 * Turn order: reverse of turnOrder (worst → best player), exactly as the
 * Power Grid rulebook specifies and as RoundManager.nextPhase() sets up when
 * transitioning from AUCTION → BUYING (currentPlayerIndex = turnOrder.size()-1).
 *
 * Each player takes their full turn (buy as many 1-unit purchases as they
 * like, then press Done).  After everyone has gone the panel calls
 * roundManager.nextPhase() and navigates to BUILDING.
 */
public class ResourcePanel extends JPanel {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final PowergridFrame frame;
    private BufferedImage background;

    /** Counts DOWN: starts at turnOrder.size()-1, hits -1 when phase is done. */
    private int buyingPlayerIndex = 0;

    /** Cache so we don't reload the same powerplant PNG repeatedly. */
    private final Map<Integer, BufferedImage> plantImageCache = new HashMap<>();

    // Buttons
    private JButton coalBtn;
    private JButton oilBtn;
    private JButton garbageBtn;
    private JButton uraniumBtn;
    private JButton doneBtn;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public ResourcePanel(PowergridFrame frame) {
        this.frame = frame;
        setLayout(null);

        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            System.err.println("ResourcePanel: could not load background – " + e.getMessage());
        }

        initUI();
    }

    // -----------------------------------------------------------------------
    // Phase entry-point (called by PowergridFrame.showScreen)
    // -----------------------------------------------------------------------

    /**
     * Must be called every time the RESOURCE screen becomes visible.
     * Resets the buying pointer to the last player in turn order (worst player
     * goes first in the buying phase).
     */
    public void startBuyingPhase() {
        RoundManager rm = frame.getRoundManager();
        buyingPlayerIndex = rm.getTurnOrder().size() - 1;
        repaint();
    }

    // -----------------------------------------------------------------------
    // UI init
    // -----------------------------------------------------------------------

    private void initUI() {
        coalBtn    = makeBuyButton(1292, 217);
        oilBtn     = makeBuyButton(1292, 280);
        garbageBtn = makeBuyButton(1292, 340);
        uraniumBtn = makeBuyButton(1292, 400);

        coalBtn   .addActionListener(e -> handleBuy(ResourceType.COAL));
        oilBtn    .addActionListener(e -> handleBuy(ResourceType.OIL));
        garbageBtn.addActionListener(e -> handleBuy(ResourceType.TRASH));
        uraniumBtn.addActionListener(e -> handleBuy(ResourceType.URANIUM));

        add(coalBtn);
        add(oilBtn);
        add(garbageBtn);
        add(uraniumBtn);

        doneBtn = new JButton("Done");
        doneBtn.setFont(new Font("Arial", Font.BOLD, 30));
        doneBtn.setBackground(new Color(237, 174, 175));
        doneBtn.setForeground(Color.WHITE);
        doneBtn.setBounds(1148, 503, 200, 75);
        doneBtn.setOpaque(true);
        doneBtn.setBorderPainted(false);
        doneBtn.addActionListener(e -> advanceTurn());
        add(doneBtn);
    }

    private JButton makeBuyButton(int x, int y) {
        JButton btn = new JButton("Buy 1");
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setBackground(new Color(125, 203, 178));
        btn.setForeground(Color.WHITE);
        btn.setBounds(x, y, 150, 50);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        return btn;
    }

    // -----------------------------------------------------------------------
    // Turn logic
    // -----------------------------------------------------------------------

    /** Returns the player whose turn it currently is, or null if the phase ended. */
    private Player getCurrentBuyingPlayer() {
        List<Player> order = frame.getRoundManager().getTurnOrder();
        if (buyingPlayerIndex < 0 || buyingPlayerIndex >= order.size()) return null;
        return order.get(buyingPlayerIndex);
    }

    /**
     * Attempts to buy 1 unit of {@code type} for the current player.
     * Automatically picks the first powerplant that can accept the resource.
     */
    private void handleBuy(ResourceType type) {
        Player player = getCurrentBuyingPlayer();
        if (player == null) return;

        RoundManager rm = frame.getRoundManager();

        // Find the first powerplant owned by the current player that can hold
        // one more unit of this resource type.
        Powerplant target = null;
        for (Powerplant p : player.getPowerplants()) {
            if (player.canStoreResource(p, type, 1)) {
                target = p;
                break;
            }
        }

        if (target == null) {
            JOptionPane.showMessageDialog(
                    this,
                    player.getName() + " has no powerplant that can store " + type + ".\n"
                            + "Make sure a plant of the correct fuel type has free capacity.",
                    "Cannot Store Resource",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Delegate the purchase to RoundManager (deducts money, updates market).
        int cost = rm.buyResource(player, target, type, 1);
        if (cost < 0) {
            // Determine a friendlier error reason
            int marketAvail = rm.getResourceMarket().getAvailableAmount(type);
            String reason = (marketAvail == 0)
                    ? "The market has no " + type + " left."
                    : "Insufficient funds ($" + player.getMoney() + " available).";
            JOptionPane.showMessageDialog(
                    this,
                    "Purchase failed: " + reason,
                    "Cannot Buy",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        repaint();
    }

    /**
     * Called when the current player presses "Done".
     * Moves to the next player (decrement) or ends the phase.
     */
    private void advanceTurn() {
        buyingPlayerIndex--;
        if (buyingPlayerIndex < 0) {
            // Every player has acted – advance the game phase and show building.
            frame.getRoundManager().nextPhase();
            frame.showScreen("BUILDING");
        } else {
            repaint();
        }
    }

    // -----------------------------------------------------------------------
    // Painting
    // -----------------------------------------------------------------------

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        }
        paintText(g);
        PanelUtils.paintResourceIcons(g, frame.getRoundManager().getResourceMarket(), frame.getRoundManager());
        PanelUtils.paintHand(g, getCurrentBuyingPlayer(), plantImageCache);
        PanelUtils.paintTurnOrderIndicators(g,frame.getRoundManager());
    }

    // --- Header / labels ---

    private void paintText(Graphics g) {
        // Phase title
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.setColor(Color.WHITE);
        g.drawString("PHASE 3: BUY RESOURCES", 1010, 37);

        // Current player banner
        Player player = getCurrentBuyingPlayer();
        if (player != null) {
            g.setFont(new Font("Arial", Font.BOLD, 22));
            g.setColor(new Color(250, 226, 120));
            RoundManager rm = frame.getRoundManager();
            int total = rm.getTurnOrder().size();
            int turnNum = total - buyingPlayerIndex; // 1-based
            g.drawString("Turn " + turnNum + "/" + total + "  –  " + player.getName(), 1010, 75);
        }

        // Section header
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.BLACK);
        g.drawString("Click to buy resources:", 1053, 154);

        // Resource labels
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("COAL",    1096, 249);
        g.drawString("OIL",     1096, 314);
        g.drawString("TRASH",   1096, 379);
        g.drawString("URANIUM", 1096, 444);

        // Market availability next to each label
        ResourceMarket market = frame.getRoundManager().getResourceMarket();
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(new Color(200, 240, 200));
        g.drawString("(" + market.getAvailableAmount(ResourceType.COAL)    + " left)", 1230, 249);
        g.drawString("(" + market.getAvailableAmount(ResourceType.OIL)     + " left)", 1230, 314);
        g.drawString("(" + market.getAvailableAmount(ResourceType.TRASH)   + " left)", 1230, 379);
        g.drawString("(" + market.getAvailableAmount(ResourceType.URANIUM) + " left)", 1230, 444);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Loads a powerplant image by its card number (e.g. 3 → /Images/powerplant-3.png).
     * Results are cached so each image is loaded at most once per session.
     */
    private BufferedImage getPlantImage(int number) {
        if (plantImageCache.containsKey(number)) {
            return plantImageCache.get(number);
        }
        BufferedImage img = null;
        try {
            img = ImageIO.read(getClass().getResource("/Images/powerplant-" + number + ".png"));
        } catch (Exception e) {
            System.err.println("ResourcePanel: no image for powerplant-" + number);
        }
        plantImageCache.put(number, img);  // store null too so we don't retry
        return img;
    }

    /**
     * Converts the colour name string stored in Player (e.g. "Yellow", "Blue")
     * into an AWT Color.  Falls back to light grey for unknown names.
     */
    private Color parseColor(String colorName) {
        if (colorName == null) return Color.LIGHT_GRAY;
        return switch (colorName.toLowerCase()) {
            case "red"    -> new Color(220, 60,  60);
            case "blue"   -> new Color(60,  100, 200);
            case "green"  -> new Color(60,  160, 80);
            case "yellow" -> new Color(220, 190, 50);
            case "purple" -> new Color(130, 60,  180);
            case "black"  -> Color.DARK_GRAY;
            default       -> Color.LIGHT_GRAY;
        };
    }
}