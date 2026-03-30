package Frontend;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class StartPanel extends JPanel implements MouseListener{
    BufferedImage image;
    PowergridFrame frame;

    public StartPanel(PowergridFrame frame){
        try {
            image = ImageIO.read(StartPanel.class.getResource("/Images/powergridTitle.png"));
        } catch (Exception e) {
            System.out.println("EXCEPTION ERRORRR");
        }
        this.frame = frame;
        addMouseListener(this);
        repaint();
    }

    public void paint(Graphics g){
        super.paint(g);
        g.drawImage(image, 0, 0, 1600, 1000, null);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("(" + e.getX() + ", " + e.getY() + ")");
        if (e.getX()>1244 && e.getX()< 1467 && e.getY()>421 && e.getY() < 495){
            frame.showScreen("BIDDING");
        }
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
