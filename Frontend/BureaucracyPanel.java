package Frontend;

import Backend.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BureaucracyPanel extends JPanel implements MouseListener {

    // ── Images ──────────────────────────────────────────────────────────────
    private BufferedImage background;
    private List<BufferedImage> powerplantImages;   // index 0-2 used in order

    // ── References ──────────────────────────────────────────────────────────
    private final PowergridFrame frame;
    private final RoundManager   roundManager;

    // ── Per-player state ─────────────────────────────────────────────────────
    private Player         currentPlayer;
    private List<Powerplant> currentPlants;   // snapshot of player's plants this turn
    private boolean[]      plantSelected;     // which plants the player toggled ON

    /** Index into RoundManager.getTurnOrder() — increments each time DONE is confirmed. */
    private int bureauPlayerIndex;

    // ── UI Components ────────────────────────────────────────────────────────
    private JLabel   statusLabel;   // "Alice's Turn — select plants to fire"
    private JLabel   resultLabel;   // "+88 Elektro (4 cities powered)"  shown briefly
    private JButton  done;
    private JButton[] powerButtons; // one per plant slot (max 3), hidden when unused
    private JButton nextPlayer;

    // ── Constructor ──────────────────────────────────────────────────────────

    public BureaucracyPanel(PowergridFrame frame) {
        this.frame        = frame;
        this.roundManager = frame.getRoundManager();

        currentPlants  = new ArrayList<>();
        plantSelected  = new boolean[3];
        powerplantImages = new ArrayList<>();

        setLayout(null);
        setFocusable(true);

        loadImages();
        initUI();
        addMouseListener(this);
    }

    // ── Image loading ─────────────────────────────────────────────────────────

    private void loadImages() {
        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
            powerplantImages.add(ImageIO.read(getClass().getResource("/Images/powerplant-3.png")));
            powerplantImages.add(ImageIO.read(getClass().getResource("/Images/powerplant-4.png")));
            powerplantImages.add(ImageIO.read(getClass().getResource("/Images/powerplant-5.png")));
        } catch (Exception e) {
            System.out.println("BureaucracyPanel: image load error — " + e.getMessage());
        }
    }

    // ── Static UI construction ────────────────────────────────────────────────

    private void initUI() {

        // "Alice's Turn — select plants to fire"
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 26));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBounds(900, 415, 620, 38);
        add(statusLabel);

        // "+88 Elektro (4 cities powered)" shown after DONE, before advancing
        resultLabel = new JLabel("", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 22));
        resultLabel.setForeground(new Color(250, 226, 120));
        resultLabel.setBounds(900, 455, 620, 34);
        add(resultLabel);

        // DONE button — same position / style as original
        done = new JButton("DONE");
        done.setFont(new Font("Arial", Font.BOLD, 35));
        done.setBackground(new Color(237, 174, 174));
        done.setForeground(Color.WHITE);
        done.setOpaque(true);
        done.setBorderPainted(false);
        done.setBounds(1140, 540, 200, 80);
        done.addActionListener(e -> handleDone());
        add(done);

        // Three plant-power buttons (hidden until a player with that many plants acts)
        powerButtons = new JButton[3];
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            JButton b = new JButton("POWER");
            b.setBounds(900 + (idx * 187), 715, 175, 45);
            b.setFont(new Font("Arial", Font.BOLD, 20));
            b.setOpaque(true);
            b.setBorderPainted(false);
            b.setBackground(BTN_UNSELECTED);
            b.setForeground(Color.WHITE);
            b.setVisible(false);
            b.addActionListener(e -> handlePower(idx));
            powerButtons[i] = b;
            add(b);
        }

        nextPlayer = new JButton("NEXT PLAYER ▶");
        nextPlayer.setFont(new Font("Arial", Font.BOLD, 28));
        nextPlayer.setBackground(new Color(100, 180, 100));
        nextPlayer.setForeground(Color.WHITE);
        nextPlayer.setOpaque(true);
        nextPlayer.setBorderPainted(false);
        nextPlayer.setBounds(1100, 640, 280, 70);
        nextPlayer.setVisible(false);
        nextPlayer.addActionListener(e -> {
            bureauPlayerIndex++;
            loadCurrentPlayer();
        });
        add(nextPlayer);
    }

    // ── Colours ───────────────────────────────────────────────────────────────

    private static final Color BTN_UNSELECTED = new Color(165, 175, 207);   // blue-grey
    private static final Color BTN_SELECTED   = new Color(90,  175, 90);    // green
    private static final Color BTN_NO_FUEL    = new Color(140, 140, 140);   // grey

    // ── Phase entry point (called by PowergridFrame.showScreen) ──────────────

    /**
     * Called once when the BUREAUCRACY screen becomes visible.
     * Checks the step transition, then loads the first player.
     */
    public void startBureaucracyPhase() {
        bureauPlayerIndex = 0;
        roundManager.checkStepTransition();   // Step 1→2 gate at start of bureaucracy
        loadCurrentPlayer();
    }

    // ── Per-player loading ────────────────────────────────────────────────────

    private void loadCurrentPlayer() {
        List<Player> order = roundManager.getTurnOrder();

        if (bureauPlayerIndex >= order.size()) {
            finishBureaucracy();
            return;
        }

        currentPlayer = order.get(bureauPlayerIndex);
        currentPlants = new ArrayList<>(currentPlayer.getPowerplants());   // safe copy
        plantSelected = new boolean[3];   // reset toggles

        // Header label
        statusLabel.setText(currentPlayer.getName() + " — select plants to fire");
        resultLabel.setText("");

        // Show/hide and configure power buttons
        for (int i = 0; i < 3; i++) {
            if (i < currentPlants.size()) {
                Powerplant p = currentPlants.get(i);
                boolean fireable = p.canFire();
                powerButtons[i].setVisible(true);
                powerButtons[i].setEnabled(fireable);
                powerButtons[i].setText(fireable ? "POWER" : "NO FUEL");
                powerButtons[i].setBackground(fireable ? BTN_UNSELECTED : BTN_NO_FUEL);
            } else {
                powerButtons[i].setVisible(false);
            }
        }

        done.setEnabled(true);

        nextPlayer.setVisible(false);

        repaint();
    }

    // ── Button handlers ───────────────────────────────────────────────────────

    /**
     * Toggles a plant's fire-selection state and reflects it in the button colour.
     */
    private void handlePower(int idx) {
        if (idx >= currentPlants.size()) return;
        Powerplant plant = currentPlants.get(idx);
        if (!plant.canFire()) return;

        plantSelected[idx] = !plantSelected[idx];
        powerButtons[idx].setBackground(plantSelected[idx] ? BTN_SELECTED : BTN_UNSELECTED);
    }

    /**
     * Fired when the current player clicks DONE.
     * <ol>
     *   <li>Fires every selected plant (consuming its stored resources).</li>
     *   <li>Counts houses powered, capped by cities owned.</li>
     *   <li>Looks up the payment from the rulebook table and credits it to the player.</li>
     *   <li>Shows a brief result label, then advances to the next player after 1.8 s.</li>
     * </ol>
     */
    private void handleDone() {
        // Lock UI while result is shown
        done.setEnabled(false);
        for (JButton b : powerButtons) b.setEnabled(false);

        // ── 1. Fire selected plants ──────────────────────────────────────────
        int totalOutput = 0;
        for (int i = 0; i < currentPlants.size(); i++) {
            if (!plantSelected[i]) continue;

            Powerplant plant = currentPlants.get(i);
            Map<ResourceType, Integer> consumed = plant.fire();
            if (consumed != null) {
                // Resources are consumed; in a full implementation the caller would
                // iterate consumed and call resourceMarket.returnToSupply(type, amount).
                totalOutput += plant.getHouseOutput();
            }
        }

        // ── 2. Cap output by cities owned ────────────────────────────────────
        int cityCount      = currentPlayer.getCityCount();
        int citiesPowered  = Math.min(totalOutput, cityCount);

        // ── 3. Look up payment and credit player ─────────────────────────────
        int payment = roundManager.getPayment(citiesPowered);
        currentPlayer.addMoney(payment);

        // ── 4. Show result, then advance ─────────────────────────────────────
        resultLabel.setText(
                "Powered " + citiesPowered + " / " + cityCount + " cities  →  +"
                        + payment + " Elektro  |  Total: $" + currentPlayer.getMoney()
        );
        repaint();

        nextPlayer.setVisible(true);

    }

    // ── End-of-phase cleanup ──────────────────────────────────────────────────

    /**
     * Called after every player has acted.
     * Runs the four post-bureaucracy sub-phases that don't require per-player UI:
     * <ol>
     *   <li>Victory check — if triggered, shows a winner dialog and stops.</li>
     *   <li>Restock the resource market.</li>
     *   <li>Update the power-plant market (discard highest or remove lowest by step).</li>
     *   <li>Advance game state and re-sort turn order for the next round.</li>
     * </ol>
     */
    private void finishBureaucracy() {

        // ── 1. Victory check ─────────────────────────────────────────────────
        if (roundManager.checkVictory()) {
            showWinnerDialog();
            return;
        }

        // ── 2. Restock resource market ────────────────────────────────────────
        roundManager.getResourceMarket().restock(
                roundManager.getPlayers().size(),
                roundManager.getCurrentStep()
        );

        // ── 3. Update power plant market ─────────────────────────────────────
        if (roundManager.getCurrentStep() < 3) {
            roundManager.getDeck().discardHighestPlant();
        } else {
            roundManager.getDeck().removeLowestPlant();
        }

        // ── 4. Advance to next round ──────────────────────────────────────────
        // BUREAUCRACY → DETERMINE_ORDER
        roundManager.nextPhase();
        // DETERMINE_ORDER → AUCTION (re-sorts turn order, resets flags)
        roundManager.handleDetermineOrder();

        frame.showScreen("ORDER");
    }

    // ── Winner dialog ─────────────────────────────────────────────────────────

    private void showWinnerDialog() {
        // Find the player who powers the most cities; tiebreak by most money
        List<Player> all = roundManager.getPlayers();
        Player winner = null;
        int bestPowered = -1, bestMoney = -1;

        for (Player p : all) {
            int powered = Math.min(p.getActualPowerableCount(), p.getCityCount());
            if (powered > bestPowered
                    || (powered == bestPowered && p.getMoney() > bestMoney)) {
                winner   = p;
                bestPowered = powered;
                bestMoney   = p.getMoney();
            }
        }

        String msg = winner != null
                ? "🏆  " + winner.getName() + " wins!\n"
                  + "Cities powered: " + bestPowered + "  |  Elektro: $" + bestMoney
                : "No winner could be determined.";

        JOptionPane.showMessageDialog(this, msg, "Game Over — Powergrid", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Painting ──────────────────────────────────────────────────────────────

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        }

        // Header
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);
        g.drawString("STEP " + roundManager.getCurrentStep() + ", PHASE 5:", 1050, 49);
        g.drawString("BUREAUCRACY", 1070, 90);

        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.drawString("Select plants to fire, then click DONE", 900, 395);

        if (currentPlayer != null) {
            paintHand(g);
        }
    }

    /**
     * Draws the current player's hand area: name, money, powerplant cards, and inventory.
     */
    private void paintHand(Graphics g) {
        // ── Money coin ────────────────────────────────────────────────────────
        g.setColor(new Color(250, 226, 120));
        g.fillOval(1460, 695, 120, 120);

        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(Color.WHITE);
        g.drawString("$" + currentPlayer.getMoney(), 1468, 762);

        // ── Player name & city count ──────────────────────────────────────────
        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.setColor(Color.WHITE);
        g.drawString(currentPlayer.getName(), 1180, 700);

        g.setFont(new Font("Arial", Font.PLAIN, 22));
        g.drawString("Cities owned: " + currentPlayer.getCityCount(), 1180, 730);

        // ── Powerplant cards ──────────────────────────────────────────────────
        for (int i = 0; i < currentPlants.size(); i++) {
            int x = 900 + (i * 187);
            Powerplant p = currentPlants.get(i);

            // Plant image (falls back gracefully if there are fewer images than plants)
            BufferedImage img = (i < powerplantImages.size()) ? powerplantImages.get(i) : null;
            if (img != null) {
                g.drawImage(img, x, 770, 180, 180, null);
            } else {
                g.setColor(new Color(80, 80, 120));
                g.fillRect(x, 770, 180, 180);
            }

            // Overlay selection highlight
            if (plantSelected[i]) {
                g.setColor(new Color(90, 200, 90, 80));   // translucent green
                g.fillRect(x, 770, 180, 180);
            }

            // Plant stats inset (top-left of card)
            g.setFont(new Font("Arial", Font.BOLD, 15));
            g.setColor(Color.WHITE);
            g.drawString("#" + p.getNumber(),                          x + 6, 788);
            g.drawString("Out: " + p.getHouseOutput(),                 x + 6, 806);
            g.drawString("Fuel: " + p.getStoredTotal()
                    + "/" + p.getResourceIntake(),                x + 6, 824);

            // Accepted resource types
            StringBuilder types = new StringBuilder();
            for (ResourceType rt : p.getAcceptedResources()) {
                types.append(rt.name().charAt(0));    // C / O / T / U
            }
            String typeStr = p.isEcological() ? "ECO" : types.toString();
            g.drawString("Type: " + typeStr, x + 6, 842);
        }

        // ── Resource inventory (aggregate across all plants) ──────────────────
        Map<ResourceType, Integer> stored = currentPlayer.getStoredResources();

        // Colour squares
        g.setColor(new Color(92, 50, 5));   g.fillRect(1475, 832, 25, 25); // coal
        g.setColor(Color.BLACK);            g.fillRect(1475, 862, 25, 25); // oil
        g.setColor(Color.YELLOW);           g.fillRect(1475, 892, 25, 25); // trash
        g.setColor(Color.RED);              g.fillRect(1475, 922, 25, 25); // uranium

        // Amounts
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.setColor(Color.WHITE);
        g.drawString(stored.getOrDefault(ResourceType.COAL,    0).toString(), 1510, 852);
        g.drawString(stored.getOrDefault(ResourceType.OIL,     0).toString(), 1510, 882);
        g.drawString(stored.getOrDefault(ResourceType.TRASH,   0).toString(), 1510, 912);
        g.drawString(stored.getOrDefault(ResourceType.URANIUM, 0).toString(), 1510, 942);
    }

    // ── MouseListener (debug) ─────────────────────────────────────────────────

    @Override public void mouseClicked(MouseEvent e)  { System.out.println("(" + e.getX() + ", " + e.getY() + ")"); }
    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}