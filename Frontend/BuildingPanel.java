package Frontend;

import Backend.City;
import Backend.Gameboard;
import Backend.Player;
import Backend.RoundManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BuildingPanel extends JPanel implements MouseListener {

    private BufferedImage background;
    private PowergridFrame frame;
    private RoundManager rm;
    private Gameboard gb;

    private int cityCost;
    private Set<String> cities;

    private JComboBox cityDropdown;
    private JButton build;
    private JButton done;

    //hand
    private Player player;
    private List<BufferedImage> powerplants;
    private int elektro;
    private List<Integer> inventory;

    public BuildingPanel(PowergridFrame frame) {
        rm = frame.getRoundManager();
        gb = rm.getBoard();

        // TEMPORARY VALUES
        cityCost = 10;
        cities = Constants.CityCoordinates.coordinates.keySet();

        player = rm.currentPlayer();
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

        cityDropdown = new JComboBox<>();
        for (String city : cities) {
            cityDropdown.addItem(city);
        }
        cityDropdown.setMaximumRowCount(15);
        cityDropdown.setFont(new Font("Arial", Font.PLAIN, 20));
        cityDropdown.setSelectedIndex(0);
        cityDropdown.setBounds(900, 200, 400, 30);
        cityDropdown.setVisible(true);
        add(cityDropdown);

        build = new JButton("<html>" + ("BUILD\nFOR $" + cityCost).replaceAll("\\n", "<br>") + "</html>");
        build.setFont(new Font("Arial", Font.BOLD, 35));
        build.setBackground(new Color(150, 196, 188));
        build.setForeground(Color.WHITE);
        build.setOpaque(true);
        build.setBorderPainted(false);
        build.setBounds(1330, 300, 200, 150);
        build.setVisible(true);
        build.addActionListener(e -> handleBuild(cityDropdown.getSelectedItem()));
        add(build);

        done = new JButton("DONE");
        done.setFont(new Font("Arial", Font.BOLD, 35));
        done.setBackground(new Color(237, 174, 174));
        done.setForeground(Color.WHITE);
        done.setOpaque(true);
        done.setBorderPainted(false);
        done.setBounds(1330, 500, 200, 100);
        done.setVisible(true);
        done.addActionListener(e -> handleDone());
        add(done);

    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);

        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        g.drawString("STEP 1, PHASE 4:", 1050, 49);
        g.drawString("BUILD CITIES", 1091, 90);

        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.setColor(Color.BLACK);
        g.drawString("Select the city from dropdown", 1030, 150);

        //draw cities
        g.setColor(Color.RED);
        for (City c : player.getOwnedCities()) {
            Point p = new Point(Constants.CityCoordinates.coordinates.get(c.toString()));
            g.fillRect(p.x - 10, p.y - 25, 20, 20);
        }

        //display num cities powered
        if (!player.getOwnedCities().isEmpty() && player.getOwnedCities().size() <= 7) {
            g.fillRect(410 + player.getOwnedCities().size() * 64, 16, 20, 20);
        } else if (player.getOwnedCities().size() > 7) {
            g.fillRect(410 + (player.getOwnedCities().size() - 7) * 32, 47, 20, 20);
        }

        paintHand(g);
    }

    private void startBuildingPhase() {
        player = rm.currentPlayer();
        System.out.println("building phase started, current player: "+player.getName());
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
        g.drawString("$" + player.getMoney(), 1480, 770);

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

    private void handleBuild(Object item) {
        if (item == null) {
            return;
        }

        String city = item.toString();
        cityCost = rm.buildCity(player, gb.getCityByName(city));
        System.out.println(player.getName()+" built "+city+" for "+cityCost);
        cityDropdown.removeItem(city);
        repaint();
    }

    private void handleDone() {
        frame.showScreen("BUREAUCRACY");
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
