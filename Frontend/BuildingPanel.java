package Frontend;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BuildingPanel extends JPanel implements MouseListener {
    BufferedImage background;
    PowergridFrame frame;
    JComboBox<String> cityDropdown;

    public BuildingPanel(PowergridFrame frame){
        this.frame = frame;
        setLayout(null);

        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            System.out.println("background image error");
        }

        this.setFocusable(true);
        setLayout(null);
        initUI();
        addMouseListener(this);
    }

    public void initUI() {
        cityDropdown = new JComboBox<>();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setFont(new Font("Arial" ,Font.BOLD,40));
        g.setColor(Color.WHITE);

        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        g.drawString("STEP 1, PHASE 4:", 1050, 49);
        g.drawString("BUILD CITIES", 1091, 90);
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