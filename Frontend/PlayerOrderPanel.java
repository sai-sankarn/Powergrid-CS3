package Frontend;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;

public class PlayerOrderPanel extends JPanel implements MouseListener {

    private BufferedImage background;
    private PowergridFrame frame;

    private JButton continueButton;

    public PlayerOrderPanel(PowergridFrame frame) {
        this.frame = frame;
        setLayout(null);

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
        continueButton.setBounds(1050, 730, 400, 150);
        continueButton.setFont(new Font("Arial", Font.BOLD, 50));
        continueButton.setBackground(new Color(125, 203, 178));
        continueButton.setForeground(Color.white);
        continueButton.setOpaque(true);
        continueButton.setBorderPainted(false);
        continueButton.addActionListener(e -> {
            endOrderPhase();
        });
        add(continueButton);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);

        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        g.drawString("STEP 1, PHASE 1: PLAYER ORDER", 915, 49);
    }

    private void endOrderPhase() {
        frame.showScreen("BIDDING");
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("(" + e.getX() + ", " + e.getY() + ")");
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
