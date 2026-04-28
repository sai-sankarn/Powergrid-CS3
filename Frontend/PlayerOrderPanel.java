package Frontend;

import Backend.Player;
import Backend.RoundManager;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class PlayerOrderPanel extends JPanel implements MouseListener {

    private BufferedImage background;
    private PowergridFrame frame;
    private RoundManager rm;
    private JButton continueButton;

    private static final int[] BOX_Y = { 120, 340, 560 };

    public PlayerOrderPanel(PowergridFrame frame) {
        this.frame = frame;
        setLayout(null);
        rm = frame.getRoundManager();

        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            System.err.println("Could not load background image.");
        }

        initUI();
        addMouseListener(this);
    }

    private void initUI() {
        continueButton = new JButton("CONTINUE");
        continueButton.setBounds(1030, 790, 400, 150);
        continueButton.setFont(new Font("Arial", Font.BOLD, 50));
        continueButton.setBackground(new Color(125, 203, 178));
        continueButton.setForeground(Color.WHITE);
        continueButton.setOpaque(true);
        continueButton.setBorderPainted(false);

        // DYNAMIC TRANSITION LOGIC
        continueButton.addActionListener(e -> {
            // If we are in the Auction phase, the next step is Bidding
            if (rm.getCurrentState() == Backend.GameState.AUCTION) {
                frame.showScreen("BIDDING");
            }
            // If we are in the Buying phase (special Round 1 transition), go to Resources
            else if (rm.getCurrentState() == Backend.GameState.BUYING) {
                frame.showScreen("RESOURCE");
            }
        });

        add(continueButton);
    }

    public void startOrderPhase() {
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        }

        // Header
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);
        g.drawString("STEP " + rm.getCurrentStep() + ", PHASE 1: PLAYER ORDER", 900, 49);

        List<Player> order = rm.getTurnOrder();

        for (int i = 0; i < Math.min(order.size(), BOX_Y.length); i++) {
            Player p = order.get(i);
            int y = BOX_Y[i];

            // FIXED: Get the player's specific color from PanelUtils
            Color playerColor = PanelUtils.parseColor(p.getColor());

            // Draw Box Background
            g.setColor(playerColor);
            g.fillRect(980, y, 500, 200);

            // Draw Box Border
            g.setColor(Color.BLACK);
            g.drawRect(980, y, 500, 200);

            // Player info
            // Note: If the box is very dark, you might want to switch text to Color.WHITE
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(Color.BLACK);
            g.drawString((i + 1) + ". " + p.getName() + ": " + p.getCityCount() + " cities", 1000, y + 70);
            g.drawString("Biggest plant: " + p.getHighestPlantNumber(), 1000, y + 140);
        }

        // Utility overlays
        PanelUtils.paintResourceIcons(g, rm.getResourceMarket());
        PanelUtils.paintTurnOrderIndicators(g, rm);
        PanelUtils.paintCities(g, rm);
    }

    @Override public void mouseClicked(MouseEvent e)  { System.out.println("(" + e.getX() + ", " + e.getY() + ")"); }
    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}