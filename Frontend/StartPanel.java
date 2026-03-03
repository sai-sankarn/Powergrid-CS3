package Frontend;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class StartPanel extends JPanel{
    BufferedImage image;

    public StartPanel(){
        try {
            image = ImageIO.read(StartPanel.class.getResource("/Images/powergridTitle.png"));
        } catch (Exception e) {
            System.out.println("EXCEPTION ERRORRR");
        }

        repaint();
    }

    public void paint(Graphics g){
        super.paint(g);
        g.drawImage(image, 0, 0, 1600, 1000, null);
    }
}
