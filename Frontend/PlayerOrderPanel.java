package Frontend;

import Backend.Player;
import Backend.RoundManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
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

    // Fixed Y positions for the three player slots
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
        continueButton.addActionListener(e -> frame.showScreen("BIDDING"));
        add(continueButton);
    }

    /**
     * Called by PowergridFrame.showScreen("ORDER").
     * Forces a repaint so the panel always reflects the latest turn order.
     */
    public void startOrderPhase() {
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);

        // Header
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);
        g.drawString("STEP " + rm.getCurrentStep() + ", PHASE 1: PLAYER ORDER", 900, 49);

        // Read the sorted order fresh on every paint — guaranteed to reflect
        // whatever RoundManager.updateTurnOrder() produced at end of bureaucracy.
        List<Player> order = rm.getTurnOrder();

        Color[] boxColors = {
                new Color(67,  126, 161),   // 1st place — blue
                new Color(237, 174, 174),   // 2nd place — pink
                new Color(250, 226, 120),   // 3rd place — yellow
        };

        g.setFont(new Font("Arial", Font.BOLD, 40));

        for (int i = 0; i < Math.min(order.size(), BOX_Y.length); i++) {
            Player p = order.get(i);
            int y = BOX_Y[i];

            // Box
            g.setColor(boxColors[i]);
            g.fillRect(980, y, 500, 200);
            g.setColor(Color.BLACK);
            g.drawRect(980, y, 500, 200);

            // Player info — name comes from the actual Player object
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(Color.BLACK);
            g.drawString((i + 1) + ". " + p.getName() + ": " + p.getCityCount() + " cities",       1000, y + 70);
            g.drawString("Biggest plant: " + p.getHighestPlantNumber(),                              1000, y + 140);
        }
    }

    private void endOrderPhase() {
        frame.showScreen("BIDDING");
    }

    @Override public void mouseClicked(MouseEvent e)  { System.out.println("(" + e.getX() + ", " + e.getY() + ")"); }
    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}