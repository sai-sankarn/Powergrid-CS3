package Frontend;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;

public class MainPanel extends JPanel implements MouseListener{
    
    StartPanel startPanel;

    public MainPanel(){
        setLayout(null);
        startPanel = new StartPanel();
        add(startPanel);
        startPanel.setBounds(0,0, 1600, 1800);
        startPanel.setVisible(true);
        addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("(" + e.getX() + ", " + e.getY() + ")");
        if ()
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
