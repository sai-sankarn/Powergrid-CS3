package Frontend;

import java.awt.*;
import javax.swing.*;

public class PowergridFrame extends JFrame {

    private static final int WIDTH = 1600;
    private static final int HEIGHT = 1000;
    
    private CardLayout cardLayout;
    private JPanel mainContainer;

    public PowergridFrame(String name) {
        super(name);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH,HEIGHT);
        setResizable(false);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(new StartPanel(this), "START");
        mainContainer.add(new SetupPanel(this), "SETUP");
        mainContainer.add(new BiddingPanel(this), "BIDDING");
        mainContainer.add(new ResourcePanel(this), "RESOURCE");
        mainContainer.add(new BuildingPanel(this), "BUILDING");
        mainContainer.add(new BureaucracyPanel(this), "BUREAUCRACY");

        add(mainContainer);
        setVisible(true);
    }

    public void showScreen(String name) {
        cardLayout.show(mainContainer, name);
    
		for (Component comp : mainContainer.getComponents()) {
			if (comp.isVisible()) {
				comp.requestFocusInWindow();
			}
		}
    }
}