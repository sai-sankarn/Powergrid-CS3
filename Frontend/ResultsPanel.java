package Frontend;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ResultsPanel extends JPanel implements MouseListener {
    private BufferedImage background;
    private PowergridFrame frame;

    public ResultsPanel(PowergridFrame frame) {
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
        
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        super.paintComponent(g);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);

        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        g.drawString("RESULTS", 1150, 49);

        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(5));
        g2.setColor(new Color(79, 53, 16));
        g2.drawLine(950, 120, 1510, 120);
        g2.drawLine(950, 270, 1510, 270);
        g2.drawLine(950, 420, 1510, 420);
        g2.drawLine(950, 570, 1510, 570);
        g2.drawLine(950, 720, 1510, 720);
        g2.drawLine(950, 120, 950, 720);
        g2.drawLine(1137, 120, 1137, 720);
        g2.drawLine(1324, 120, 1324, 720);
        g2.drawLine(1510, 120, 1510, 720);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("CITIES", 1185, 175);
        g.drawString("POWERED", 1150, 230);
        g.drawString("MONEY", 1360, 175);
        g.drawString("EARNED", 1350, 230);
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