package Frontend;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BuildingPanel extends JPanel implements MouseListener {

    BufferedImage background;
    PowergridFrame frame;

    int cityCost;
    Set<String> cities;
    Set<Point> pointsBuilt;

    JComboBox cityDropdown;
    JButton build;
    JButton done;

    public BuildingPanel(PowergridFrame frame) {
        // TEMPORARY VALUES
        cityCost = 10;
        cities = Constants.CityCoordinates.coordinates.keySet();
        pointsBuilt = new HashSet<>();

        this.frame = frame;
        setLayout(null);

        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            System.out.println("background image error");
        }

        this.setFocusable(true);
        setLayout(null);
        initUI();
        addMouseListener(this);
    }

    public void initUI() {

        cityDropdown = new JComboBox<>();
        for(String city : cities) {
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
        build.addActionListener(e -> handleBuild(cityDropdown.getSelectedItem().toString()));
        add(build);

        done = new JButton("DONE");
        done.setFont(new Font("Arial", Font.BOLD, 35));
        done.setBackground(new Color(206, 127, 129));
        done.setForeground(Color.WHITE);
        done.setOpaque(true);
        done.setBorderPainted(false);
        done.setBounds(1330, 500, 200, 100);
        done.setVisible(true);
        //done.addActionListener(e -> handleClick(done));
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

        g.setColor(Color.RED);
        for(Point p : pointsBuilt) {
            g.fillRect(p.x-10, p.y-25, 20, 20); // will change depending on step and currentplayer
        }
    }

    private void handleBuild(String city) {
        Point p = new Point(Constants.CityCoordinates.coordinates.get(city));
        pointsBuilt.add(p);
        repaint();
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
