package Frontend;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;

public class MainPanel extends JPanel implements MouseListener{
    
    StartPanel startPanel;
    BiddingPanel biddingPanel;

    public MainPanel(){
        setLayout(null);
        startPanel = new StartPanel();
        biddingPanel = new BiddingPanel();
        add(startPanel);
        add(biddingPanel);
        startPanel.setBounds(0,0, 1600, 1000);
        biddingPanel.setBounds(0,0,1600,1000);
        startPanel.setVisible(true);
        addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("(" + e.getX() + ", " + e.getY() + ")");
        if (e.getX()>1244 && e.getX()< 1467 && e.getY()>421 && e.getY() < 495){
            startPanel.setVisible(false);
            biddingPanel.setVisible(true);
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
