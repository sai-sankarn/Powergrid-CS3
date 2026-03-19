package Frontend;

import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BiddingPanel extends JPanel implements MouseListener {
    BufferedImage background;
    PowergridFrame frame;
    ArrayList<BufferedImage> powerplants = new ArrayList<>();
    JComboBox<Integer> powerplantDropdown;
    JTextField bidInput;
    JButton bid;
    JButton pass;

    public BiddingPanel(PowergridFrame frame){
        this.frame = frame;

        try {
            background = ImageIO.read(this.getClass().getResource("Images/backgroud.png"));
            for (int i=3;i<11;i++){
                powerplants.add(ImageIO.read(this.getClass().getResource("Images/powerplant-" + i + ".png")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        initUI();
    }

    public void initUI(){
        powerplantDropdown = new JComboBox<>();
        
    }


    @Override
    public void mouseClicked(MouseEvent e) {
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