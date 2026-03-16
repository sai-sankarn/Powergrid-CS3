package Frontend;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;

public class ResourcePanel extends JPanel implements MouseListener{
    PowergridFrame frame;
    BufferedImage background;
    JButton coal;
    JButton oil;
    JButton garbage;
    JButton uranium;
    JButton done;

    int[] list = {24,24,24,12};
    

    public ResourcePanel(PowergridFrame frame) {
        this.frame = frame;
        setLayout(null);
        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        initUI();
        addMouseListener(this);

    }

    public void initUI() {
        coal = new JButton("Buy 1");
        coal.setFont(new Font("Arial",Font.BOLD,20));
        coal.setBackground(new Color(125,203,178));
        coal.setForeground(Color.white);
        coal.setBounds(1292, 217, 150, 50);        
        coal.setOpaque(true);
        coal.setBorderPainted(false);
        coal.setVisible(true);
        coal.addActionListener(e -> handleBuy("COAL"));
        add(coal);

        oil = new JButton("Buy 1");
        oil.setFont(new Font("Arial",Font.BOLD,20));
        oil.setBackground(new Color(125,203,178));
        oil.setForeground(Color.white);
        oil.setBounds(1292, 280, 150, 50);
        oil.setOpaque(true);
        oil.setBorderPainted(false);
        oil.setVisible(true);
        oil.addActionListener(e -> handleBuy("OIL"));
        add(oil);

        garbage = new JButton("Buy 1");
        garbage.setFont(new Font("Arial",Font.BOLD,20));
        garbage.setBackground(new Color(125,203,178));
        garbage.setForeground(Color.white);
        garbage.setBounds(1292, 340, 150, 50);
        garbage.setOpaque(true);
        garbage.setBorderPainted(false);
        garbage.addActionListener(e -> handleBuy("TRASH"));
        garbage.setVisible(true);

        add(garbage);

        uranium = new JButton("Buy 1");
        uranium.setFont(new Font("Arial",Font.BOLD,20));
        uranium.setBackground(new Color(125,203,178));
        uranium.setForeground(Color.white);
        uranium.setBounds(1292, 400, 150, 50);
        uranium.setOpaque(true);
        uranium.setBorderPainted(false);
        uranium.setVisible(true);
        uranium.addActionListener(e -> handleBuy("URANIUM"));
        add(uranium);

        done = new JButton("Done");
        done.setFont(new Font("Arial",Font.BOLD,30));
        done.setBackground(new Color(237,174,175));
        done.setForeground(Color.white);
        done.setBounds(1148, 503, 200, 75);
        done.setOpaque(true);
        done.setBorderPainted(false);
        done.setVisible(true);
        done.addActionListener(e -> frame.showScreen("BIDDING"));
        add(done);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        paintText(g);
        paintResourceIcons(g);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("(" + e.getX() + ", " + e.getY() + ")");
    }

    public void handleBuy(String resource){
        switch (resource){
            case ("COAL") -> list[0]++;
            case ("OIL") -> list[1]++;
            case ("TRASH") -> list[2]++;
            case ("URANIUM") -> list[3]++;
        }
        repaint();
    }

    public void paintText(Graphics g){
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.setColor(Color.WHITE);
        g.drawString("PHASE 3: BUY RESOURCES", 1010,37);
        g.setColor(Color.BLACK);
        g.drawString("Click to buy resources: ", 1053, 154);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("COAL", 1096, 249);
        g.drawString("OIL", 1096,314);
        g.drawString("TRASH", 1096, 314+65);
        g.drawString("URANIUM", 1096, 314+65+65);
    }

    public void paintResourceIcons(Graphics g){
        //COAL
        int x=769;
        int y=902;
        for (int i=0;i<list[0];i++){
            g.setColor(new Color(99,68,38));
            g.fillRect(x, y, 18, 12);
            switch (i) {
                case 2 -> x=671;
                case 5 -> x=572;
                case 8 -> x=474;
                case 11 -> x=376;
                case 14 -> {x=276; y=906;}
                case 17 -> x=180;
                case 20 -> x=81;
                default -> x-=28;
            }
        }

        //OIL
        x=753;
        y=922;
        for (int i=0;i<list[1];i++){
            g.setColor(new Color(12, 37, 48));
            g.fillRect(x, y, 12, 11);
            switch (i){
                case 2 -> x=655;
                case 5 -> x=556;
                case 8 -> x=458;
                default -> x-=21;
            }
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
