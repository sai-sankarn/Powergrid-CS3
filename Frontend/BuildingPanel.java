package Frontend;

import Backend.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BuildingPanel extends JPanel implements MouseListener {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final PowergridFrame frame;
    private BufferedImage background;

    /** Counts DOWN from turnOrder.size()-1 to -1 (worst player builds first). */
    private int buildingPlayerIndex;

    /** Suppresses the dropdown's ActionListener while we repopulate it. */
    private boolean refreshingDropdown = false;

    private JComboBox<String> cityDropdown;
    private JButton buildBtn;
    private JButton doneBtn;

    private final Map<Integer, BufferedImage> plantImageCache = new HashMap<>();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public BuildingPanel(PowergridFrame frame) {
        this.frame = frame;
        setLayout(null);
        setFocusable(true);

        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            System.err.println("BuildingPanel: could not load background – " + e.getMessage());
        }

        initUI();
        addMouseListener(this);
    }

    // -----------------------------------------------------------------------
    // Phase entry-point (called by PowergridFrame.showScreen)
    // -----------------------------------------------------------------------

    /**
     * Must be called every time the BUILDING screen becomes visible.
     * RoundManager.nextPhase() already sets currentPlayerIndex = turnOrder.size()-1
     * when BUYING → BUILDING, so we mirror that here.
     */
    public void startBuildingPhase() {
        buildingPlayerIndex = frame.getRoundManager().getTurnOrder().size() - 1;
        refreshDropdown();
        repaint();
    }

    // -----------------------------------------------------------------------
    // UI initialisation
    // -----------------------------------------------------------------------

    private void initUI() {
        cityDropdown = new JComboBox<>();
        cityDropdown.setMaximumRowCount(15);
        cityDropdown.setFont(new Font("Arial", Font.PLAIN, 20));
        cityDropdown.setBounds(900, 200, 400, 30);
        // Update the build button text whenever the selected city changes.
        cityDropdown.addActionListener(e -> {
            if (!refreshingDropdown) updateBuildButtonText();
        });
        add(cityDropdown);

        buildBtn = new JButton("BUILD");
        buildBtn.setFont(new Font("Arial", Font.BOLD, 35));
        buildBtn.setBackground(new Color(150, 196, 188));
        buildBtn.setForeground(Color.WHITE);
        buildBtn.setOpaque(true);
        buildBtn.setBorderPainted(false);
        buildBtn.setBounds(1330, 300, 200, 150);
        buildBtn.addActionListener(e -> handleBuild());
        add(buildBtn);

        doneBtn = new JButton("DONE");
        doneBtn.setFont(new Font("Arial", Font.BOLD, 35));
        doneBtn.setBackground(new Color(206, 127, 129));
        doneBtn.setForeground(Color.WHITE);
        doneBtn.setOpaque(true);
        doneBtn.setBorderPainted(false);
        doneBtn.setBounds(1330, 500, 200, 100);
        doneBtn.addActionListener(e -> advanceTurn());
        add(doneBtn);
    }

    // -----------------------------------------------------------------------
    // Turn logic
    // -----------------------------------------------------------------------

    /** Returns the player whose turn it currently is, or null if the phase is over. */
    private Player getCurrentBuildingPlayer() {
        List<Player> order = frame.getRoundManager().getTurnOrder();
        if (buildingPlayerIndex < 0 || buildingPlayerIndex >= order.size()) return null;
        return order.get(buildingPlayerIndex);
    }

    /**
     * Repopulates the dropdown with every city the current player may legally build in.
     * A city qualifies when all of the following hold:
     *   1. It exists on the board.
     *   2. It has an open slot for the current game step.
     *   3. The player does not already occupy it.
     *   4. The player can afford the total cost (connection + slot).
     */
    private void refreshDropdown() {
        refreshingDropdown = true;
        cityDropdown.removeAllItems();

        Player player = getCurrentBuildingPlayer();
        if (player != null) {
            RoundManager rm = frame.getRoundManager();
            Gameboard board  = rm.getBoard();
            int step         = rm.getCurrentStep();

            // after — sorted cheapest first
            List<String> affordable = new ArrayList<>();
            for (String cityName : Constants.CityCoordinates.coordinates.keySet()) {
                City city = board.getCityByName(cityName);
                if (city == null) continue;
                if (!city.hasOpenSlot(step)) continue;
                if (city.isOccupiedBy(player)) continue;
                if (calculateCostFor(player, city) >= 0) {
                    affordable.add(cityName);
                }
            }
            affordable.sort((a, b) -> {
                City cityA = board.getCityByName(a);
                City cityB = board.getCityByName(b);
                return Integer.compare(calculateCostFor(player, cityA),
                        calculateCostFor(player, cityB));
            });
            for (String cityName : affordable) {
                cityDropdown.addItem(cityName);
            }
        }

        refreshingDropdown = false;
        updateBuildButtonText();
    }

    /**
     * Calculates the total cost for the current player to build in {@code city}.
     * Returns the cost (≥ 0) if the build is legal and affordable, or -1 otherwise.
     */
    private int calculateCostFor(Player player, City city) {
        RoundManager rm = frame.getRoundManager();
        int step = rm.getCurrentStep();

        int slotCost = city.getNextSlotCost(step);
        if (slotCost < 0) return -1; // city is full for this step

        if (player.getOwnedCities().isEmpty()) {
            // First city ever: flat cost of 10, no connection needed.
            return player.canAfford(10) ? 10 : -1;
        }

        int connectionCost = rm.getBoard().calculateConnectionCost(
                player.getOwnedCities(), city);
        if (connectionCost == Integer.MAX_VALUE) return -1; // unreachable

        int total = connectionCost + slotCost;
        return player.canAfford(total) ? total : -1;
    }

    /** Updates the BUILD button label to show the cost of the currently selected city. */
    private void updateBuildButtonText() {
        Player player  = getCurrentBuildingPlayer();
        Object selected = cityDropdown.getSelectedItem();

        if (player == null || selected == null) {
            buildBtn.setText("BUILD");
            return;
        }

        City city = frame.getRoundManager().getBoard().getCityByName(selected.toString());
        if (city == null) {
            buildBtn.setText("BUILD");
            return;
        }

        int cost = calculateCostFor(player, city);
        buildBtn.setText(cost >= 0
                ? "<html>BUILD<br>FOR $" + cost + "</html>"
                : "<html>BUILD<br>(N/A)</html>");
    }

    /** Executes the build for the currently selected city. */
    private void handleBuild() {
        Player player = getCurrentBuildingPlayer();
        if (player == null) return;

        Object selected = cityDropdown.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a city first.",
                    "No City Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cityName = selected.toString();
        City city = frame.getRoundManager().getBoard().getCityByName(cityName);
        if (city == null) return;

        // Delegate to RoundManager — handles payment, occupant, city tracking,
        // and obsolete-plant removal.
        int cost = frame.getRoundManager().buildCity(player, city);
        if (cost < 0) {
            JOptionPane.showMessageDialog(this,
                    "Cannot build in " + cityName + ". Check funds or availability.",
                    "Build Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Refresh the dropdown; the city just built (and any now-unaffordable ones)
        // will be removed automatically.
        refreshDropdown();
        repaint();
    }

    /** Called when the current player presses DONE. Advances to the next player or ends the phase. */
    private void advanceTurn() {
        buildingPlayerIndex--;

        if (buildingPlayerIndex < 0) {
            // All players have built — run the full bureaucracy phase and start next round.
            RoundManager rm = frame.getRoundManager();
            rm.nextPhase();
            frame.showScreen("BUREAUCRACY");
        } else {
            refreshDropdown();
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
        paintHeader(g);
        paintCitiesOnBoard(g);
        paintHand(g);
    }

    private void paintHeader(Graphics g) {
        RoundManager rm = frame.getRoundManager();

        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);
        g.drawString("STEP " + rm.getCurrentStep() + ", PHASE 4:", 1050, 49);
        g.drawString("BUILD CITIES", 1091, 90);

        // Current-player banner
        Player player = getCurrentBuildingPlayer();
        if (player != null) {
            int total   = rm.getTurnOrder().size();
            int turnNum = total - buildingPlayerIndex; // 1-based
            g.setFont(new Font("Arial", Font.BOLD, 22));
            g.setColor(new Color(250, 226, 120));
            g.drawString("Turn " + turnNum + "/" + total + "  –  " + player.getName(),
                    1010, 135);
        }

        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.setColor(Color.BLACK);
        g.drawString("Select a city from the dropdown:", 1000, 180);
    }

    /**
     * Draws every player's occupied cities on the map using their player colour,
     * and renders the city-count tracker square for the current player.
     */
    private void paintCitiesOnBoard(Graphics g) {
        RoundManager rm = frame.getRoundManager();

        for (Player p : rm.getPlayers()) {
            Color c = parseColor(p.getColor());
            for (City city : p.getOwnedCities()) {
                Point pt = Constants.CityCoordinates.coordinates.get(city.getName());
                if (pt == null) continue;
                g.setColor(c);
                g.fillRect(pt.x - 10, pt.y - 25, 20, 20);
            }
        }

        // City-count tracker for the active player only
        Player player = getCurrentBuildingPlayer();
        if (player != null) {
            int count = player.getCityCount();
            g.setColor(parseColor(player.getColor()));
            if (count > 0 && count <= 7) {
                g.fillRect(410 + count * 65, 16, 20, 20);
            } else if (count > 7) {
                g.fillRect(410 + (count - 7) * 32, 47, 20, 20);
            }
        }
    }

    private void paintHand(Graphics g) {
        Player player = getCurrentBuildingPlayer();
        if (player == null) return;

        // Player colour avatar
        g.setColor(parseColor(player.getColor()));
        g.fillOval(1460, 695, 120, 120);

        // Player name
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString(player.getName(), 1180, 700);

        // Powerplants (up to 3, loaded by card number)
        List<Powerplant> plants = new ArrayList<>(player.getPowerplants());
        int px = 900;
        for (int i = 0; i < Math.min(plants.size(), 3); i++) {
            int plantNum = plants.get(i).getNumber();
            BufferedImage img = getPlantImage(plantNum);
            if (img != null) {
                g.drawImage(img, px, 770, 180, 180, null);
            } else {
                // Fallback: labelled placeholder rectangle
                g.setColor(new Color(60, 60, 80));
                g.fillRect(px, 770, 180, 180);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 28));
                g.drawString("#" + plantNum, px + 55, 870);
            }
            px += 185;
        }

        // Money
        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.setColor(Color.WHITE);
        g.drawString("$" + player.getMoney(), 1480, 770);

        // Resource inventory
        paintInventory(g, player);
    }

    private void paintInventory(Graphics g, Player player) {
        Map<ResourceType, Integer> stored;
        try {
            stored = player.getStoredResources();
        } catch (Exception ex) {
            stored = Collections.emptyMap();
        }

        int coal    = stored.getOrDefault(ResourceType.COAL,    0);
        int oil     = stored.getOrDefault(ResourceType.OIL,     0);
        int trash   = stored.getOrDefault(ResourceType.TRASH,   0);
        int uranium = stored.getOrDefault(ResourceType.URANIUM, 0);

        // Colour swatches
        g.setColor(new Color(92, 50, 5));  g.fillRect(1475, 832, 25, 25);
        g.setColor(Color.BLACK);           g.fillRect(1475, 862, 25, 25);
        g.setColor(Color.YELLOW);          g.fillRect(1475, 892, 25, 25);
        g.setColor(Color.RED);             g.fillRect(1475, 922, 25, 25);

        // Counts
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(coal),    1510, 850);
        g.drawString(String.valueOf(oil),     1510, 880);
        g.drawString(String.valueOf(trash),   1510, 910);
        g.drawString(String.valueOf(uranium), 1510, 940);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Loads a powerplant image by card number (e.g. 3 → /Images/powerplant-3.png).
     * Caches results so each image is only loaded once.
     */
    private BufferedImage getPlantImage(int number) {
        if (plantImageCache.containsKey(number)) return plantImageCache.get(number);
        BufferedImage img = null;
        try {
            img = ImageIO.read(getClass().getResource("/Images/powerplant-" + number + ".png"));
        } catch (Exception e) {
            System.err.println("BuildingPanel: no image for powerplant-" + number);
        }
        plantImageCache.put(number, img); // store null so we don't retry
        return img;
    }

    /** Converts a player colour name to an AWT Color. */
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

    // -----------------------------------------------------------------------
    // MouseListener (kept for coordinate debugging)
    // -----------------------------------------------------------------------

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("(" + e.getX() + ", " + e.getY() + ")");
    }
    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}