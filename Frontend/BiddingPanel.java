package Frontend;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
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
    JButton replaceButton1;
    JButton replaceButton2;
    JButton replaceButton3;
    List<JButton> replaceButtons;
    private int selectedPlantIndex = -1;
    private boolean selectionLocked = false;
    int player = 1;
    int firstPlayer = 1;
    ArrayList<Integer> bids = new ArrayList<>();
    ArrayList<Integer> powerplantsBought = new ArrayList<>();
    List<Integer> inventory;

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

        bids.add(0);
        bids.add(0);
        bids.add(0);

        powerplantsBought.add(-1);
        powerplantsBought.add(-1);
        powerplantsBought.add(-1);

        inventory = new ArrayList<>();
        inventory.add(10);
        inventory.add(20);
        inventory.add(30);
        inventory.add(40);

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
        bidButton.addActionListener(e -> handleBid(Integer.parseInt(bidInput.getText())));
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
        passButton.addActionListener(e -> handlePass());
        add(passButton);

        //part of hand
        replaceButton1 = new JButton("REPLACE");
        replaceButton2 = new JButton("REPLACE");
        replaceButton3 = new JButton("REPLACE");
        replaceButtons = new ArrayList<>();
        replaceButtons.add(replaceButton1);
        replaceButtons.add(replaceButton2);
        replaceButtons.add(replaceButton3);

        for(int i=0; i<replaceButtons.size(); i++) {
            JButton b = replaceButtons.get(i);
            b.setBounds(900 + (i * 187), 715, 175, 45);
            b.setFont(new Font("Arial",Font.BOLD,20));
            b.setOpaque(true);
            b.setBorderPainted(false);
            b.setBackground(new Color(165, 175, 207));
            b.setForeground(Color.WHITE);
            b.setVisible(true);
            b.addActionListener(e -> handleReplace());
            add(b);
        }
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

        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("Player: " + player, 963,150);
        System.out.println(bids);

        paintHand(g);
    }

    private void paintHand(Graphics g) {
        g.setColor(new Color(250, 226, 120));
        g.fillOval(1460, 695, 120, 120);
        g.setColor(Color.WHITE);
        g.drawString("Player 1", 1180, 700);

        //powerplants owned
        g.drawImage(powerplants.get(0), 900, 770, 180, 180, null);
        g.drawImage(powerplants.get(1), 900 + 180 + 5, 770, 180, 180, null);
        g.drawImage(powerplants.get(2), 900 + 2 * (180 + 5), 770, 180, 180, null);

        //money
        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.drawString("$" + Integer.toString(50), 1480, 770);

        //inventory/resources
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString(inventory.get(0).toString(), 1510, 850);
        g.drawString(inventory.get(1).toString(), 1510, 880);
        g.drawString(inventory.get(2).toString(), 1510, 910);
        g.drawString(inventory.get(3).toString(), 1510, 940);
        g.setColor(new Color(92, 50, 5));
        g.fillRect(1475, 832, 25, 25);
        g.setColor(Color.BLACK);
        g.fillRect(1475, 862, 25, 25);
        g.setColor(Color.YELLOW);
        g.fillRect(1475, 892, 25, 25);
        g.setColor(Color.RED);
        g.fillRect(1475, 922, 25, 25);

        //replace buttons
        g.setColor(new Color(164, 177, 219));
        
    }

    public void handleBid(int amount){
        bids.set(player-1, amount);
        int numberOfNulls = 0;
        for (int i=0;i<bids.size();i++){
            if (bids.get(i)==-1){
                numberOfNulls++;
            }
        }
        if (numberOfNulls==2){
            frame.showScreen("RESOURCE");
        }
        else {
            if (player==3){
                player=1;
            }
            else {player++;}
        }
        repaint();
    }

    public void handlePass(){
        bids.set(player-1,-1);
        int numberOfNulls = 0;
        for (int i=0;i<bids.size();i++){
            if (bids.get(i)==-1){
                numberOfNulls++;
            }
        }
        if (numberOfNulls==2){
            frame.showScreen("RESOURCE");
        }
        else {
            if (player==3){
                player=1;
            }
            else {player++;}
        }
        repaint();
    }

    public void handleReplace() {
        //handle replacing powerplants in hand
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