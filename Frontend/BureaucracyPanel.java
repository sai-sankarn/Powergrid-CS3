package Frontend;

import Backend.Player;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class BureaucracyPanel extends JPanel implements MouseListener {

    private BufferedImage background;
    private PowergridFrame frame;

    //hand
    private Player player;
    private List<BufferedImage> powerplants;
    private int elektro;
    private List<Integer> inventory;
    private JButton powerButton1;
    private JButton powerButton2;
    private JButton powerButton3;
    private List<JButton> powerButtons;
    private JButton done;

    public BureaucracyPanel(PowergridFrame frame) {
        this.frame = frame;
        setLayout(null);

        player = new Player("Player 1", 50, "red");
        powerplants = new ArrayList<>();
        elektro = 50;
        inventory = new ArrayList<>();
        inventory.add(10);
        inventory.add(20);
        inventory.add(30);
        inventory.add(40);

        this.frame = frame;
        setLayout(null);

        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
            powerplants.add(ImageIO.read(getClass().getResource("/Images/powerplant-3.png")));
            powerplants.add(ImageIO.read(getClass().getResource("/Images/powerplant-4.png")));
            powerplants.add(ImageIO.read(getClass().getResource("/Images/powerplant-5.png")));
        } catch (Exception e) {
            System.out.println("exception error");
        }

        this.setFocusable(true);
        initUI();
        addMouseListener(this);
    }

    public void initUI() {

        done = new JButton("DONE");
        done.setFont(new Font("Arial", Font.BOLD, 35));
        done.setBackground(new Color(237, 174, 174));
        done.setForeground(Color.WHITE);
        done.setOpaque(true);
        done.setBorderPainted(true);
        done.setBorder(new LineBorder(Color.GRAY));
        done.setBounds(1140, 540, 200, 80);
        done.setToolTipText("DONE");
        done.setVisible(true);
        done.addActionListener(e -> handleDone());
        add(done);

        //part of hand
        powerButton1 = new JButton("POWER");
        powerButton2 = new JButton("POWER");
        powerButton3 = new JButton("POWER");
        powerButtons = new ArrayList<>();
        powerButtons.add(powerButton1);
        powerButtons.add(powerButton2);
        powerButtons.add(powerButton3);

        for (int i = 0; i < powerButtons.size(); i++) {
            JButton b = powerButtons.get(i);
            b.setBounds(900 + (i * 187), 715, 175, 45);
            b.setFont(new Font("Arial", Font.BOLD, 20));
            b.setOpaque(true);
            b.setBorderPainted(true);
            b.setBorder(new LineBorder(Color.GRAY));
            b.setBackground(new Color(165, 175, 207));
            b.setForeground(Color.WHITE);
            b.setToolTipText("POWER");
            b.setVisible(true);
            b.addActionListener(e -> handlePower(b));
            add(b);
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);

        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        g.drawString("STEP 1, PHASE 5:", 1050, 49);
        g.drawString("BUREAUCRACY", 1070, 90);

        g.setFont(new Font("Arial", Font.PLAIN, 40));
        g.drawString("Choose power plants to power", 950, 470);

        paintHand(g);
    }

    private void paintHand(Graphics g) {
        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.setColor(new Color(250, 226, 120));
        g.fillOval(1460, 695, 120, 120);
        g.setColor(Color.WHITE);
        g.drawString(player.getName(), 1180, 700);

        //powerplants owned
        g.drawImage(powerplants.get(0), 900, 770, 180, 180, null);
        g.drawImage(powerplants.get(1), 900 + 180 + 5, 770, 180, 180, null);
        g.drawImage(powerplants.get(2), 900 + 2 * (180 + 5), 770, 180, 180, null);

        //money
        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.drawString("$" + Integer.toString(elektro), 1480, 770);

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
    }

    private void handlePower(JButton b) {
        //handle power
    }

    private void handleDone() {
        frame.showScreen("RESULTS");
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
