package Frontend;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
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
    JButton bidButton;
    JButton passButton;
    private int selectedPlantIndex = -1;
    private boolean selectionLocked = false;

    public BiddingPanel(PowergridFrame frame){
        this.frame = frame;
        setLayout(null);

        try {
            background = ImageIO.read(BiddingPanel.class.getResource("/Images/background.png"));
            powerplants.add(ImageIO.read(BiddingPanel.class.getResource("/Images/powerplant-3.png")));
            powerplants.add(ImageIO.read(BiddingPanel.class.getResource("/Images/powerplant-4.png")));
            powerplants.add(ImageIO.read(BiddingPanel.class.getResource("/Images/powerplant-5.png")));
            powerplants.add(ImageIO.read(BiddingPanel.class.getResource("/Images/powerplant-6.png")));
            powerplants.add(ImageIO.read(BiddingPanel.class.getResource("/Images/powerplant-7.png")));
            powerplants.add(ImageIO.read(BiddingPanel.class.getResource("/Images/powerplant-8.png")));
            powerplants.add(ImageIO.read(BiddingPanel.class.getResource("/Images/powerplant-9.png")));
            powerplants.add(ImageIO.read(BiddingPanel.class.getResource("/Images/powerplant-10.png")));

        } catch (Exception e) {
            e.printStackTrace();
        }

        initUI();
        addMouseListener(this);
    }

    public void initUI(){
        powerplantDropdown = new JComboBox<>();
        powerplantDropdown.setBounds(1448, 219, 119, 20);
        powerplantDropdown.setVisible(true);
        for (int i=3;i<7;i++){
            powerplantDropdown.addItem(i);
        }
        powerplantDropdown.addActionListener(e -> {
        if (!selectionLocked) {
            int selectedValue = (int) powerplantDropdown.getSelectedItem();
            
            selectedPlantIndex = selectedValue - 3; 
            
            powerplantDropdown.setEnabled(false);
            selectionLocked = true;
            
            repaint();
        }
    });
        add(powerplantDropdown);
        //----------------------------------------------------------
        bidInput = new JTextField();
        bidInput.setBounds(1341,407,120,20);
        bidInput.setVisible(true);
        add(bidInput);
        //-------------------------------------------------------
        bidButton = new JButton("BID");
        bidButton.setBounds(1341,475,120,40);
        bidButton.setFont(new Font("Arial",Font.BOLD,20));
        bidButton.setOpaque(true);
        bidButton.setBorderPainted(false);
        bidButton.setBackground(new Color(125,203,178));
        bidButton.setForeground(Color.white);
        bidButton.setVisible(true);
        add(bidButton);
        //-----------------------------------------------------
        passButton = new JButton("PASS");
        passButton.setBounds(1341,551,120,40);
        passButton.setFont(new Font("Arial",Font.BOLD,20));
        passButton.setOpaque(true);
        passButton.setBorderPainted(false);
        passButton.setBackground(new Color(237,174,174));
        passButton.setForeground(Color.white);
        passButton.setVisible(true);
        add(passButton);
        
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        g.drawImage(background, 0,0, this.getWidth(), this.getHeight(), null);
        int x=918;
        for (int i=0;i<4;i++){
            if (i!=selectedPlantIndex){
                g.drawImage(powerplants.get(i), x, 209, 122, 122, null);
            }
            x+=132;
        }
        if (selectionLocked){
            g.drawImage(powerplants.get(selectedPlantIndex),996,400,228,228,null);
        }

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