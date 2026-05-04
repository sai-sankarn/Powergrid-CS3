package Frontend;

import Backend.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class BureaucracyPanel extends JPanel implements MouseListener {

    // ── Images ──────────────────────────────────────────────────────────────
    private BufferedImage background;
    private final Map<Integer, BufferedImage> plantImageCache = new HashMap<>();

    // ── References ──────────────────────────────────────────────────────────
    private final PowergridFrame frame;
    private final RoundManager   roundManager;

    // ── Per-player state ─────────────────────────────────────────────────────
    private Player           currentPlayer;
    private List<Powerplant> currentPlants;
    private boolean[]        plantSelected;
    private int              bureauPlayerIndex;

    /**
     * True when the victory threshold was already crossed before this Bureaucracy
     * phase started (i.e., a player built enough cities in Phase 4).  All players
     * still power their plants normally; after the last player clicks NEXT, the
     * winner is declared instead of starting a new round.
     */
    private boolean isFinalRound = false;

    /**
     * Tracks how many cities each player actually powered this Bureaucracy phase.
     * Used to determine the winner in the final round, because plant.fire() consumes
     * resources before determineWinner() runs — so getActualPowerableCount() would
     * return 0 for everyone after firing.
     */
    private Map<Player, Integer> citiesPoweredMap = new HashMap<>();

    // ── UI Components ────────────────────────────────────────────────────────
    private JLabel   statusLabel;
    private JLabel   resultLabel;
    private JButton  done;
    private JButton[] powerButtons;
    private JButton nextPlayer;

    public BureaucracyPanel(PowergridFrame frame) {
        this.frame        = frame;
        this.roundManager = frame.getRoundManager();

        currentPlants  = new ArrayList<>();
        plantSelected  = new boolean[3];

        setLayout(null);
        setFocusable(true);

        loadImages();
        initUI();
        addMouseListener(this);
    }

    private void loadImages() {
        try {
            background = ImageCache.getBackground();
        } catch (Exception e) {
            System.out.println("BureaucracyPanel: image load error — " + e.getMessage());
        }
    }

    private void initUI() {
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 26));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBounds(900, 415, 620, 38);
        add(statusLabel);

        resultLabel = new JLabel("", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 22));
        resultLabel.setForeground(new Color(250, 226, 120));
        resultLabel.setBounds(900, 455, 620, 34);
        add(resultLabel);

        done = new JButton("DONE");
        done.setFont(new Font("Arial", Font.BOLD, 35));
        done.setBackground(new Color(237, 174, 174));
        done.setForeground(Color.WHITE);
        done.setOpaque(true);
        done.setBorderPainted(true);
        done.setBorder(new LineBorder(Color.GRAY));
        done.setBounds(1140, 540, 200, 80);
        done.setToolTipText("Done powering plants");
        done.setVisible(true);
        done.addActionListener(e -> handleDone());
        add(done);

        powerButtons = new JButton[3];
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            JButton b = new JButton("POWER");
            b.setBounds(900 + (idx * 187), 715, 175, 45);
            b.setFont(new Font("Arial", Font.BOLD, 20));
            b.setOpaque(true);
            b.setBorderPainted(true);
            b.setBorder(new LineBorder(Color.GRAY));
            b.setToolTipText("Power/cancel power");
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
        nextPlayer.setBorderPainted(true);
        nextPlayer.setBorder(new LineBorder(Color.GRAY));
        nextPlayer.setBounds(1100, 630, 280, 70);
        nextPlayer.setToolTipText("Continue to next player");
        nextPlayer.setVisible(false);
        nextPlayer.addActionListener(e -> {
            bureauPlayerIndex++;
            loadCurrentPlayer();
        });
        add(nextPlayer);
    }

    private static final Color BTN_UNSELECTED = new Color(165, 175, 207);
    private static final Color BTN_SELECTED   = new Color(90,  175, 90);
    private static final Color BTN_NO_FUEL    = new Color(140, 140, 140);

    // ── Phase entry-point ────────────────────────────────────────────────────

    /**
     * Called by PowergridFrame whenever the BUREAUCRACY screen becomes visible
     * (i.e., after Phase 4 – Building – has just completed).
     *
     * Step / victory checks happen HERE, before any powering occurs:
     *   1. checkStepTransition() — may advance to Step 2 or activate Step 3.
     *   2. checkVictory()       — if the city threshold was reached in Phase 4,
     *      this is the "final round"; players still power normally, but afterward
     *      the winner is declared instead of starting a new round.
     */
    public void startBureaucracyPhase() {
        bureauPlayerIndex = 0;
        citiesPoweredMap  = new HashMap<>();

        // Step transitions are evaluated at the start of Bureaucracy (after Phase 4).
        roundManager.checkStepTransition();

        // Determine now whether this is the last round so we can show the correct
        // ending after all players have powered their plants.
        isFinalRound = roundManager.checkVictory();

        loadCurrentPlayer();
    }

    // ── Per-player loading ───────────────────────────────────────────────────

    private void loadCurrentPlayer() {
        List<Player> order = roundManager.getTurnOrder();
        if (bureauPlayerIndex >= order.size()) {
            finishBureaucracy();
            return;
        }

        currentPlayer = order.get(bureauPlayerIndex);
        currentPlants = new ArrayList<>(currentPlayer.getPowerplants());
        plantSelected = new boolean[3];

        statusLabel.setText(currentPlayer.getName() + " — select plants to fire");
        resultLabel.setText("");

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

    // ── Button handlers ──────────────────────────────────────────────────────

    private void handlePower(int idx) {
        if (idx >= currentPlants.size()) return;
        Powerplant plant = currentPlants.get(idx);
        if (!plant.canFire()) return;

        plantSelected[idx] = !plantSelected[idx];
        powerButtons[idx].setBackground(plantSelected[idx] ? BTN_SELECTED : BTN_UNSELECTED);
        repaint();
    }

    private void handleDone() {
        done.setEnabled(false);
        for (JButton b : powerButtons) b.setEnabled(false);

        int totalOutput = 0;
        for (int i = 0; i < currentPlants.size(); i++) {
            if (!plantSelected[i]) continue;
            Powerplant plant = currentPlants.get(i);
            Map<ResourceType, Integer> consumed = plant.fire();
            if (consumed != null) {
                totalOutput += plant.getHouseOutput();
            }
        }

        int cityCount     = currentPlayer.getCityCount();
        int citiesPowered = Math.min(totalOutput, cityCount);
        int payment       = roundManager.getPayment(citiesPowered);
        currentPlayer.addMoney(payment);

        // Store the actual powered count so the winner screen can use it.
        // (After fire() the resources are consumed, so getActualPowerableCount()
        // would return 0 — we must record the value now.)
        citiesPoweredMap.put(currentPlayer, citiesPowered);

        resultLabel.setText("Powered " + citiesPowered + " / " + cityCount
                + " cities  →  +" + payment + " Elektro  |  Total: $" + currentPlayer.getMoney());
        repaint();
        nextPlayer.setVisible(true);
    }

    // ── End-of-phase logic ───────────────────────────────────────────────────

    /**
     * Called after every player has powered their plants.
     *
     * If this was the final round (victory threshold crossed during Phase 4),
     * compute the final rankings via RoundManager and hand off to the Results
     * screen.  Otherwise run the normal end-of-round housekeeping:
     *   • Restock the resource market.
     *   • Update the power-plant market (discard highest in Steps 1-2, remove
     *     lowest in Step 3).
     *   • Advance to Phase 1 of the next round.
     */
    private void finishBureaucracy() {
        if (isFinalRound) {
            // Let RoundManager compute the winner and build the GameResult object.
            roundManager.determineWinner(citiesPoweredMap);
            // Hand off to the Results screen — no dialog, no blocking call.
            frame.showScreen("RESULTS");
            return;
        }

        roundManager.getResourceMarket().restock(
                roundManager.getPlayers().size(),
                roundManager.getCurrentStep()
        );

        // Power-plant market update:
        //   Steps 1 & 2: bury the highest future-market card above the Step 3 card.
        //   Step 3:      permanently remove the lowest-numbered plant.
        if (roundManager.getCurrentStep() < 3) {
            roundManager.getDeck().discardHighestPlant();
        } else {
            roundManager.getDeck().removeLowestPlant();
        }

        roundManager.nextPhase();
        roundManager.handleDetermineOrder();
        frame.showScreen("ORDER");
    }

    // ── Painting ─────────────────────────────────────────────────────────────

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) g.drawImage(background, 0, 0, getWidth(), getHeight(), null);

        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);
        g.drawString("STEP " + roundManager.getCurrentStep() + ", PHASE 5:", 1050, 49);
        g.drawString("BUREAUCRACY", 1070, 90);

        // Warn players if this is the final round.
        if (isFinalRound) {
            g.setColor(new Color(255, 80, 80));
            g.setFont(new Font("Arial", Font.BOLD, 22));
            g.drawString("⚠  FINAL ROUND — winner declared after all players power!", 900, 115);
        }

        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.setColor(Color.WHITE);
        g.drawString("Select plants to fire, then click DONE", 900, 395);

        // Always paint the cities/tracker on the map
        PanelUtils.paintCities(g, roundManager);
        PanelUtils.paintTurnOrderIndicators(g, roundManager);
        PanelUtils.paintResourceIcons(g, roundManager.getResourceMarket(), frame.getRoundManager());

        if (currentPlayer != null) paintHand(g);
    }

    private void paintHand(Graphics g) {
        g.setColor(PanelUtils.parseColor(currentPlayer.getColor()));
        g.fillOval(1460, 695, 120, 120);

        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.setColor(Color.WHITE);
        g.drawString("$" + currentPlayer.getMoney(), 1480, 770);

        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.setColor(Color.WHITE);
        g.drawString(currentPlayer.getName(), 1180, 660);

        g.setFont(new Font("Arial", Font.PLAIN, 22));
        g.drawString("Cities owned: " + currentPlayer.getCityCount(), 1180, 690);

        for (int i = 0; i < currentPlants.size(); i++) {
            int x = 900 + (i * 187);
            Powerplant p = currentPlants.get(i);

            BufferedImage img = PanelUtils.getPlantImage(p.getNumber(), plantImageCache);
            if (img != null) {
                g.drawImage(img, x, 770, 180, 180, null);
            } else {
                g.setColor(new Color(80, 80, 120));
                g.fillRect(x, 770, 180, 180);
            }

            if (plantSelected[i]) {
                g.setColor(new Color(90, 200, 90, 80));
                g.fillRect(x, 770, 180, 180);
            }

            g.setFont(new Font("Arial", Font.BOLD, 15));
            g.setColor(Color.WHITE);
            g.drawString("#" + p.getNumber(), x + 6, 788);
            g.drawString("Out: " + p.getHouseOutput(), x + 6, 806);
            g.drawString("Fuel: " + p.getStoredTotal() + "/" + p.getResourceIntake(), x + 6, 824);

            StringBuilder types = new StringBuilder();
            for (ResourceType rt : p.getAcceptedResources()) types.append(rt.name().charAt(0));
            String typeStr = p.isEcological() ? "ECO" : types.toString();
            g.drawString("Type: " + typeStr, x + 6, 842);
        }

        PanelUtils.paintInventory(g, currentPlayer);
    }

    // ── MouseListener ────────────────────────────────────────────────────────

    @Override public void mouseClicked(MouseEvent e)  { System.out.println("(" + e.getX() + ", " + e.getY() + ")"); }
    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}