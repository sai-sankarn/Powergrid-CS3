package Frontend;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;

public class SetupPanel extends JPanel implements MouseListener{
    PowergridFrame frame;
    BufferedImage background;
    JButton teal;
    JButton brown;
    JButton red;
    JButton yellow;
    JButton blue;
    JButton purple;
    int count = 0;

    public SetupPanel(PowergridFrame frame) {
        this.frame = frame;
        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            System.out.println("EXCEPTION ERRORRR");
        }

        this.setFocusable(true);
        setLayout(null);
        initUI();
        addMouseListener(this);
    }

    public void initUI(){
        teal = new JButton("Teal");
        teal.setFont(new Font("Arial", Font.BOLD, 40));
        teal.setBackground(new Color(150, 196, 188));
        teal.setForeground(Color.WHITE);
        teal.setOpaque(true);
        teal.setBorderPainted(false);
        teal.setBounds(1036, 271+50, 200, 100);
        teal.setVisible(true);
        teal.addActionListener(e -> handleClick(teal));
        add(teal);

        brown = new JButton("Brown");
        brown.setFont(new Font("Arial", Font.BOLD, 40));
        brown.setBackground(new Color(182,132,105));
        brown.setForeground(Color.WHITE);
        brown.setOpaque(true);
        brown.setBorderPainted(false);
        brown.setBounds(1313, 271+50, 200, 100);
        brown.setVisible(true);
        brown.addActionListener(e -> handleClick(brown));
        add(brown);

        red = new JButton("Red");
        red.setFont(new Font("Arial", Font.BOLD, 40));
        red.setBackground(new Color(206,127,129));
        red.setForeground(Color.WHITE);
        red.setOpaque(true);
        red.setBorderPainted(false);
        red.setBounds(1036, 400+50, 200, 100);
        red.setVisible(true);
        red.addActionListener(e -> handleClick(red));
        add(red);

        yellow = new JButton("Yellow");
        yellow.setBackground(new Color(222,203,93));
        yellow.setForeground(Color.WHITE);
        yellow.setFont(new Font("Arial", Font.BOLD, 40));
        yellow.setOpaque(true);
        yellow.setBorderPainted(false);
        yellow.setBounds(1313, 400+50, 200, 100);
        yellow.setVisible(true);
        yellow.addActionListener(e -> handleClick(yellow));
        add(yellow);

        blue = new JButton("Blue");
        blue.setBackground(new Color(132,157,181));
        blue.setForeground(Color.WHITE);
        blue.setFont(new Font("Arial", Font.BOLD, 40));
        blue.setOpaque(true);
        blue.setBorderPainted(false);
        blue.setBounds(1036, 530+50, 200, 100);
        blue.setVisible(true);
        blue.addActionListener(e -> handleClick(blue));
        add(blue);

        purple = new JButton("Purple");
        purple.setBackground(new Color(149,125,143));
        purple.setForeground(Color.WHITE);
        purple.setFont(new Font("Arial", Font.BOLD, 40));
        purple.setOpaque(true);
        purple.setBorderPainted(false);
        purple.setBounds(1313, 530+50, 200, 100);
        purple.setVisible(true);
        purple.addActionListener(e -> handleClick(purple));
        add(purple);
    }

    public void paintComponent(java.awt.Graphics g){
        super.paintComponent(g);
        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        g.setFont(new Font("Arial" ,Font.BOLD,40));
        g.setColor(Color.WHITE);
        g.drawString("SETUP: CHOOSE AREA", 1050, 49);
        g.setFont(new Font("ARIAL",Font.BOLD,30));
        g.drawString("Player 1: Select A Color That Corresponds", 969,174);
        g.drawString("To The Area You Want",1093,201);

        for (Map.Entry<String, Point> entry : Constants.CityCoordinates.coordinates.entrySet()){
            double topLeftX = entry.getValue().getX()-(53/2);
            double topLeftY = entry.getValue().getY()-(53/2);

            g.fillOval((int) topLeftX, (int) topLeftY,53,53);
        }
    }

    public void handleClick(JButton button) {
        button.setVisible(false);
        count++;
        if (count == 4){
            frame.showScreen("BIDDING");
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
