package Frontend;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class BiddingPanel extends JPanel{
    BufferedImage[] powerplants;

    public BiddingPanel(){
        powerplants = new BufferedImage[8];
        try {
            for (int i=0;i<=powerplants.length;i++){
                powerplants[i] = ImageIO.read(BiddingPanel.class.getResource("/Images/powerplant-" + (i+3) + ".png"));
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION ERROR");
        }
    }

    public void paint(Graphics g){
        super.paint(g);
        int x = 350;
        int y= 100;
        for (int i=0;i<powerplants.length;i++){
            g.drawImage(powerplants[i], x, y, 200,200, null);
            x+=200+10;
            if (i==3){
                x=350;
                y+=200 + 20;
            }
        }
    }


}
