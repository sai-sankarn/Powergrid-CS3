package Frontend;

import Backend.*;
import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;

public class PowergridFrame extends JFrame {

    private static final int WIDTH = 1600;
    private static final int HEIGHT = 1000;
    
    private CardLayout cardLayout;
    private JPanel mainContainer;

    private RoundManager roundManager;

    private BiddingPanel biddingPanel;

    public PowergridFrame(String name) {
        super(name);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH,HEIGHT);
        setResizable(false);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        ArrayList<Player> players = new ArrayList<>();
        players.add(new Player("Sai",50,"Yellow"));
        players.add(new Player("ChenXi",50,"Blue"));
        players.add(new Player("Catherine",50,"Green"));

        roundManager = new RoundManager(players, new Gameboard(), new ResourceMarket(), new PowerplantDeck());

        biddingPanel = new BiddingPanel(this);

        mainContainer.add(new StartPanel(this), "START");
        mainContainer.add(new SetupPanel(this), "SETUP");
        mainContainer.add(new PlayerOrderPanel(this), "ORDER");
        mainContainer.add(biddingPanel, "BIDDING");
        mainContainer.add(new ResourcePanel(this), "RESOURCE");
        mainContainer.add(new BuildingPanel(this), "BUILDING");
        mainContainer.add(new BureaucracyPanel(this), "BUREAUCRACY");
        mainContainer.add(new ResultsPanel(this), "RESULTS");

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

        if (name.equals("BIDDING")){
            biddingPanel.startAuctionPhase();
        }
    }

    public RoundManager getRoundManager(){
        return roundManager;
    }
}