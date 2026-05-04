package Frontend;

import Backend.*;

import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;

public class PowergridFrame extends JFrame {

    private static final int WIDTH  = 1600;
    private static final int HEIGHT = 1000;

    private CardLayout cardLayout;
    private JPanel mainContainer;

    private RoundManager roundManager;

    private BiddingPanel      biddingPanel;
    private ResourcePanel     resourcePanel;
    private BuildingPanel     buildingPanel;
    private BureaucracyPanel  bureaucracyPanel;
    private PlayerOrderPanel  playerOrderPanel;
    private ResultsPanel      resultsPanel;

    public PowergridFrame(String name) {
        super(name);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setResizable(false);

        cardLayout    = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        ArrayList<Player> players = new ArrayList<>();
        players.add(new Player("Sai",       50, "Yellow"));
        players.add(new Player("ChenXi",    50, "Blue"));
        players.add(new Player("Catherine", 50, "Green"));

        Gameboard board = new Gameboard();
        board.initializeGermanMap();

        ResourceMarket market = new ResourceMarket();
        market.initializeStartingResources();

        roundManager = new RoundManager(players, board, market, new PowerplantDeck());
        roundManager.handleDetermineOrder();

        biddingPanel     = new BiddingPanel(this);
        resourcePanel    = new ResourcePanel(this);
        buildingPanel    = new BuildingPanel(this);
        bureaucracyPanel = new BureaucracyPanel(this);
        playerOrderPanel = new PlayerOrderPanel(this);
        resultsPanel = new ResultsPanel(this);

        mainContainer.add(new StartPanel(this),  "START");
        mainContainer.add(new SetupPanel(this),  "SETUP");
        mainContainer.add(playerOrderPanel,      "ORDER");
        mainContainer.add(biddingPanel,          "BIDDING");
        mainContainer.add(resourcePanel,         "RESOURCE");
        mainContainer.add(buildingPanel,         "BUILDING");
        mainContainer.add(bureaucracyPanel,      "BUREAUCRACY");
        mainContainer.add(resultsPanel, "RESULTS");

        add(mainContainer);
        setVisible(true);
    }

    public void showScreen(String name) {
        cardLayout.show(mainContainer, name);

        // Give keyboard focus to whichever panel just became visible.
        for (Component comp : mainContainer.getComponents()) {
            if (comp.isVisible()) {
                comp.requestFocusInWindow();
            }
        }

        // Phase-specific initialisation hooks.
        switch (name) {
            case "ORDER"        -> playerOrderPanel.startOrderPhase();
            case "BIDDING"      -> biddingPanel.startAuctionPhase();
            case "RESOURCE"     -> resourcePanel.startBuyingPhase();
            case "BUILDING"     -> buildingPanel.startBuildingPhase();
            case "BUREAUCRACY"  -> bureaucracyPanel.startBureaucracyPhase();   // ← added
        }
    }

    public RoundManager getRoundManager() {
        return roundManager;
    }
}