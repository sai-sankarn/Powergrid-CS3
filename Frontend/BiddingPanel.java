package Frontend;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BiddingPanel extends JPanel implements MouseListener, KeyListener {
    private PowergridFrame frame;
    private BufferedImage[] powerplants;
    private String state = "BIG";
    private int selection = -1;


    private JTextField bidInput;
    private JButton bidButton;
    private JButton passButton;
    private JLabel promptLabel;

    public BiddingPanel(PowergridFrame frame) {
        this.frame = frame;
        this.powerplants = new BufferedImage[8];
        this.setLayout(null);
        this.setBackground(new Color(45, 45, 45));

        loadImages();
        initUI();

        this.setFocusable(true);
        this.addMouseListener(this);
        this.addKeyListener(this);
    }

    private void loadImages() {
        try {
            for (int i = 0; i < powerplants.length; i++) {
                powerplants[i] = ImageIO.read(getClass().getResource("/Images/powerplant-" + (i + 3) + ".png"));
            }
        } catch (Exception e) {
            System.out.println("Error loading images: " + e.getMessage());
        }
    }

    private void initUI() {
        promptLabel = new JLabel("Type the index for the Powerplant you want from the current market (1-4)");
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setFont(new Font("Arial", Font.BOLD, 20));
        promptLabel.setBounds(350, 40, 1000, 30);
        add(promptLabel);

        bidInput = new JTextField();
        bidInput.setBounds(1150, 420, 140, 40);
        bidInput.setFont(new Font("Arial", Font.PLAIN, 24));
        bidInput.setVisible(false);
        add(bidInput);

        bidButton = new JButton("Place Bid");
        bidButton.setBounds(1300, 420, 120, 40);
        bidButton.setVisible(false);
        bidButton.addActionListener(e -> handleBid());
        add(bidButton);

        passButton = new JButton("Pass");
        passButton.setBounds(1150, 470, 270, 40);
        passButton.setBackground(new Color(200, 50, 50));
        passButton.setForeground(Color.WHITE);
        passButton.setVisible(false);
        passButton.addActionListener(e -> handlePass());
        add(passButton);
    }

    private void handleBid() {
        try {
            int amount = Integer.parseInt(bidInput.getText());
            System.out.println("Bid of " + amount + " placed on Powerplant " + selection);
            resetToSelection();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
        }
    }

    private void handlePass() {
        System.out.println("Player passed on Powerplant " + selection);
        resetToSelection();
    }

    private void resetToSelection() {
        state = "BIG";
        selection = -1;
        bidInput.setVisible(false);
        bidButton.setVisible(false);
        passButton.setVisible(false);
        bidInput.setText("");
        promptLabel.setText("Type the index for the Powerplant you want from the current market (1-4)");
        this.requestFocusInWindow();
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (state.equals("BIG")) {
            paintBig(g);
        } else if (state.equals("SELECTION")) {
            paintSelection(g);
        }
    }

    private void paintBig(Graphics g) {
        int x = 350, y = 100;
        for (int i = 0; i < powerplants.length; i++) {
            if (powerplants[i] != null) {
                g.drawImage(powerplants[i], x, y, 200, 200, null);
                g.setColor(Color.WHITE);
                g.drawString("ID: " + (i + 3), x + 5, y + 15);
            }
            x += 210;
            if (i == 3) { x = 350; y += 220; }
        }
    }

    private void paintSelection(Graphics g) {
        int x = 100, y = 100;
        for (int i = 0; i < powerplants.length; i++) {
            if (powerplants[i] == null) continue;

            if (i == selection-1) {
                g.drawImage(powerplants[i], 1150, 100, 300, 300, null);
                g.setColor(Color.YELLOW);
                g.drawRect(1150, 100, 300, 300);
            } else {
                g.drawImage(powerplants[i], x, y, 120, 120, null);
                x += 130;
                if (x > 800) { x = 100; y += 130; }
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (state.equals("BIG")) {
            char c = e.getKeyChar();
            if (Character.isDigit(c)) {
                int val = Character.getNumericValue(c);
                if (val>0&&val<5){
                    selection = (c == '0') ? 10 : val;
                    state = "SELECTION";
                    showBiddingUI();
                    repaint();
                }
            }
        }
    }

    private void showBiddingUI() {
        bidInput.setVisible(true);
        bidButton.setVisible(true);
        passButton.setVisible(true);
        promptLabel.setText("Bidding on Powerplant: " + selection);
        bidInput.requestFocusInWindow();
    }

    @Override public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE) resetToSelection();
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) { this.requestFocusInWindow(); }
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}