package Frontend;

import Backend.Player;
import Backend.RoundManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class PlayerOrderPanel extends JPanel implements MouseListener {

    private BufferedImage background;
    private PowergridFrame frame;

    private RoundManager rm;
    private List<Player> allPlayers;

    private JButton continueButton;

    public PlayerOrderPanel(PowergridFrame frame) {
        this.frame = frame;
        setLayout(null);
        rm = frame.getRoundManager();
        allPlayers = rm.getPlayers();

        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            System.err.println("Could not load background image.");
        }

        initUI();
        addMouseListener(this);
    }

    private void initUI() {
        continueButton = new JButton("CONTINUE");
        continueButton.setBounds(1030, 790, 400, 150);
        continueButton.setFont(new Font("Arial", Font.BOLD, 50));
        continueButton.setBackground(new Color(125, 203, 178));
        continueButton.setForeground(Color.white);
        continueButton.setOpaque(true);
        continueButton.setBorderPainted(false);
        continueButton.addActionListener(e -> {
            endOrderPhase();
        });
        add(continueButton);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);

        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        g.drawString("STEP " + rm.getCurrentStep() + ", PHASE 1: PLAYER ORDER", 900, 49);

        g.setColor(new Color(67, 126, 161));
        g.fillRect(980, 120, 500, 200);
        g.setColor(new Color(237, 174, 174));
        g.fillRect(980, 340, 500, 200);
        g.setColor(new Color(250, 226, 120));
        g.fillRect(980, 560, 500, 200);
        g.setColor(Color.BLACK);
        g.drawRect(980, 120, 500, 200);
        g.drawRect(980, 340, 500, 200);
        g.drawRect(980, 560, 500, 200);

        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Player 1: "+allPlayers.get(0).getCityCount() +" cities", 1000, 190);
        g.drawString("Biggest power plant: "+allPlayers.get(0).getHighestPlantNumber(), 1000, 260);
        g.drawString("Player 2: "+allPlayers.get(1).getCityCount()+" cities", 1000, 410);
        g.drawString("Biggest power plant: "+allPlayers.get(1).getHighestPlantNumber(), 1000, 480);
        g.drawString("Player 3: "+allPlayers.get(2).getCityCount()+" cities", 1000, 630);
        g.drawString("Biggest power plant: "+allPlayers.get(2).getHighestPlantNumber(), 1000, 700);
    }

    private void endOrderPhase() {
        frame.showScreen("BIDDING");
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
