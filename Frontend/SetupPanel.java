package Frontend;

import Backend.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

public class SetupPanel extends JPanel implements MouseListener {
    private PowergridFrame frame;
    private BufferedImage background;

    private JButton teal, brown, red, yellow, blue, purple;
    private Map<JButton, String> buttonToRegion = new HashMap<>();
    private List<String> selectedRegions = new ArrayList<>();

    // Adjacency and City mappings
    private Map<String, Set<String>> regionAdjacency = new HashMap<>();
    private Map<String, List<String>> regionCities = new HashMap<>();

    private int count = 0;

    public SetupPanel(PowergridFrame frame) {
        this.frame = frame;
        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            System.err.println("SetupPanel: Could not load background image.");
        }

        initAdjacency();
        initRegionCities();

        this.setFocusable(true);
        setLayout(null);
        initUI();
        addMouseListener(this);
    }

    /**
     * Adjacency rules as specified:
     * Teal -> Red, Brown, Yellow
     * Red -> Blue, Teal, Yellow
     * Brown -> Teal, Yellow
     * Blue -> Red, Yellow, Purple
     * Yellow -> Teal, Brown, Red, Blue, Purple (Everything)
     * Purple -> Blue, Yellow
     */
    private void initAdjacency() {
        regionAdjacency.put("Teal", new HashSet<>(Arrays.asList("Red", "Brown", "Yellow")));
        regionAdjacency.put("Red", new HashSet<>(Arrays.asList("Blue", "Teal", "Yellow")));
        regionAdjacency.put("Brown", new HashSet<>(Arrays.asList("Teal", "Yellow")));
        regionAdjacency.put("Blue", new HashSet<>(Arrays.asList("Red", "Yellow", "Purple")));
        regionAdjacency.put("Yellow", new HashSet<>(Arrays.asList("Teal", "Brown", "Red", "Blue", "Purple")));
        regionAdjacency.put("Purple", new HashSet<>(Arrays.asList("Blue", "Yellow")));
    }

    /**
     * City names mapped EXACTLY to the Strings in Gameboard.java's initializeGermanMap()
     */
    private void initRegionCities() {
        // Teal (North)
        regionCities.put("Teal", Arrays.asList("Flensburg", "Kiel", "Cuxhaven", "Wilhelmshaven", "Hamburg", "Bremen", "Hannover"));

        // Brown (Northeast) - Note: Gameboard uses "Lubeck" (no umlaut)
        regionCities.put("Brown", Arrays.asList("Lubeck", "Rostock", "Schwerin", "Torgelow", "Magdeburg", "Berlin", "Frankfurt-O"));

        // Red (West)
        regionCities.put("Red", Arrays.asList("Osnabrück", "Münster", "Duisburg", "Essen", "Dortmund", "Düsseldorf", "Kassel"));

        // Yellow (East/Central)
        regionCities.put("Yellow", Arrays.asList("Halle", "Leipzig", "Dresden", "Erfurt", "Fulda", "Würzburg", "Nürnberg"));

        // Blue (Southwest)
        regionCities.put("Blue", Arrays.asList("Aachen", "Köln", "Trier", "Wiesbaden", "Frankfurt-M", "Saarbrücken", "Mannheim"));

        // Purple (South)
        regionCities.put("Purple", Arrays.asList("Stuttgart", "Freiburg", "Konstanz", "Augsburg", "Regensburg", "München", "Passau"));
    }

    public void initUI() {
        teal = createButton("Teal", new Color(150, 196, 188), 1036, 321);
        brown = createButton("Brown", new Color(182, 132, 105), 1313, 321);
        red = createButton("Red", new Color(206, 127, 129), 1036, 450);
        yellow = createButton("Yellow", new Color(222, 203, 93), 1313, 450);
        blue = createButton("Blue", new Color(132, 157, 181), 1036, 580);
        purple = createButton("Purple", new Color(149, 125, 143), 1313, 580);
    }

    private JButton createButton(String name, Color color, int x, int y) {
        JButton btn = new JButton(name);
        btn.setFont(new Font("Arial", Font.BOLD, 40));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(true);
        btn.setBorder(new LineBorder(Color.GRAY));
        btn.setBounds(x, y, 200, 100);
        btn.addActionListener(e -> handleClick(btn));
        add(btn);
        buttonToRegion.put(btn, name);
        return btn;
    }

    public void handleClick(JButton button) {
        String region = buttonToRegion.get(button);

        // Ensure subsequent picks are touching the previous ones
        if (count > 0 && !isTouchingExisting(region)) {
            return;
        }

        selectedRegions.add(region);
        button.setVisible(false);
        count++;

        if (count == 3) {
            finalizeActiveCities();
            frame.showScreen("ORDER");
        } else {
            refreshButtonStates();
        }
        repaint();
    }

    private boolean isTouchingExisting(String region) {
        for (String selected : selectedRegions) {
            if (regionAdjacency.get(region).contains(selected)) return true;
        }
        return false;
    }

    private void refreshButtonStates() {
        for (Map.Entry<JButton, String> entry : buttonToRegion.entrySet()) {
            JButton btn = entry.getKey();
            String name = entry.getValue();
            if (btn.isVisible()) {
                // Button is only enabled if it touches the selection group
                btn.setEnabled(isTouchingExisting(name));
            }
        }
    }

    private void finalizeActiveCities() {
        RoundManager rm = frame.getRoundManager();
        Gameboard board = rm.getBoard();
        Set<City> activeSet = new HashSet<>();

        for (String regionName : selectedRegions) {
            List<String> names = regionCities.get(regionName);
            for (String cityName : names) {
                City c = board.getCityByName(cityName);
                if (c != null) {
                    activeSet.add(c);
                } else {
                    System.err.println("SetupPanel Error: Backend could not find city: " + cityName);
                }
            }
        }
        board.setActiveCities(activeSet);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) g.drawImage(background, 0, 0, getWidth(), getHeight(), null);

        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);
        g.drawString("SETUP: CHOOSE AREA", 1050, 49);

        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("Player " + (count + 1) + ": Select A Touching Region", 969, 174);
        g.drawString("Selected: " + selectedRegions.toString(), 1036, 250);

        PanelUtils.paintResourceIcons(g, frame.getRoundManager().getResourceMarket(), frame.getRoundManager());
        PanelUtils.paintTurnOrderIndicators(g, frame.getRoundManager());
    }

    @Override public void mouseClicked(MouseEvent e) { System.out.println("(" + e.getX() + ", " + e.getY() + ")"); }
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}