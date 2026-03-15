package Frontend;

import java.awt.Color;
import java.awt.Font;
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
        coal = new JButton("Coal");
        coal.setFont(new Font("Arial",Font.BOLD,30));
        coal.setBackground(new Color(125,203,178));
        coal.setForeground(Color.white);
        coal.setBounds(1292, 217, 150, 50);        
        coal.setOpaque(true);
        coal.setBorderPainted(false);
        coal.setVisible(true);
        add(coal);

        oil = new JButton("Oil");
        oil.setFont(new Font("Arial",Font.BOLD,30));
        oil.setBackground(new Color(125,203,178));
        oil.setForeground(Color.white);
        oil.setBounds(1292, 280, 150, 50);
        oil.setOpaque(true);
        oil.setBorderPainted(false);
        oil.setVisible(true);
        add(oil);

        garbage = new JButton("Garbage");
        garbage.setFont(new Font("Arial",Font.BOLD,30));
        garbage.setBackground(new Color(125,203,178));
        garbage.setForeground(Color.white);
        garbage.setBounds(1292, 340, 150, 50);
        garbage.setOpaque(true);
        garbage.setBorderPainted(false);
        garbage.setVisible(true);

        add(garbage);

        uranium = new JButton("Uranium");
        uranium.setFont(new Font("Arial",Font.BOLD,30));
        uranium.setBackground(new Color(125,203,178));
        uranium.setForeground(Color.white);
        uranium.setBounds(1292, 400, 150, 50);
        uranium.setOpaque(true);
        uranium.setBorderPainted(false);
        uranium.setVisible(true);
        add(uranium);

        done = new JButton("Done");
        done.setFont(new Font("Arial",Font.BOLD,30));
        done.setBackground(new Color(237,174,175));
        done.setForeground(Color.white);
        done.setBounds(1066, 503, 200, 75);
        done.setOpaque(true);
        done.setBorderPainted(false);
        done.setVisible(true);
        add(done);
    }

    public void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
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
