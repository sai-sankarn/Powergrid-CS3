package Frontend;

import Backend.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BiddingPanel extends JPanel implements MouseListener {
    private BufferedImage background;
    private PowergridFrame frame;

    // UI Components
    private JComboBox<Powerplant> powerplantDropdown;
    private JTextField bidInput;
    private JButton bidButton;
    private JButton passButton;

    // Image Cache to load powerplant images dynamically
    private Map<Integer, BufferedImage> plantImages = new HashMap<>();

    // Auction State Tracking
    private boolean isAuctionActive = false;
    private int auctionInitiatorIndex = 0; // Index in the RoundManager turn order
    private List<Player> currentBidders = new ArrayList<>();
    private int activeBidderIndex = 0; // Index in the currentBidders list

    public BiddingPanel(PowergridFrame frame) {
        this.frame = frame;
        setLayout(null);

        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            System.err.println("Could not load background image.");
        }

        initUI();
        addMouseListener(this);
    }

    public void initUI() {
        powerplantDropdown = new JComboBox<>();
        powerplantDropdown.setBounds(1448, 219, 119, 20);
        add(powerplantDropdown);

        bidInput = new JTextField();
        bidInput.setBounds(1341, 407, 120, 20);
        add(bidInput);

        bidButton = new JButton("BID");
        bidButton.setBounds(1341, 475, 120, 40);
        bidButton.setFont(new Font("Arial", Font.BOLD, 20));
        bidButton.setBackground(new Color(125, 203, 178));
        bidButton.setForeground(Color.white);
        bidButton.setOpaque(true);
        bidButton.setBorderPainted(false);
        bidButton.setToolTipText("BID");
        bidButton.addActionListener(e -> {
            try {
                int amount = Integer.parseInt(bidInput.getText());
                handleBid(amount);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number.");
            }
        });
        add(bidButton);

        passButton = new JButton("PASS");
        passButton.setBounds(1341, 551, 120, 40);
        passButton.setFont(new Font("Arial", Font.BOLD, 20));
        passButton.setBackground(new Color(237, 174, 174));
        passButton.setForeground(Color.white);
        passButton.setOpaque(true);
        passButton.setBorderPainted(false);
        passButton.setToolTipText("PASS");
        passButton.addActionListener(e -> handlePass());
        add(passButton);
    }

    /**
     * Called by PowergridFrame when switching to this screen to reset state
     */
    public void startAuctionPhase() {
        isAuctionActive = false;
        auctionInitiatorIndex = 0;
        findNextInitiator();
    }

    /**
     * Finds the next player in turn order who needs to initiate an auction.
     * If all players are done, ends the phase.
     */
    private void findNextInitiator() {
        RoundManager rm = frame.getRoundManager();
        List<Player> turnOrder = rm.getTurnOrder();

        while (auctionInitiatorIndex < turnOrder.size()) {
            Player p = turnOrder.get(auctionInitiatorIndex);
            // Initiator is someone who hasn't bought a plant and hasn't passed out entirely
            if (!p.isBoughtThisRound() && !p.isPassedAuction()) {
                refreshDropdown();
                repaint();
                return; // Found the next initiator
            }
            auctionInitiatorIndex++;
        }

        // If we reach here, the auction phase is completely over
        endAuctionPhase();
    }

    private void endAuctionPhase() {
        RoundManager rm = frame.getRoundManager();
        rm.nextPhase();
        frame.showScreen("RESOURCE");
    }

    private void setupBiddersForAuction(Player initiator) {
        RoundManager rm = frame.getRoundManager();
        currentBidders.clear();

        // Everyone who hasn't bought a plant joins the bidding war
        for (Player p : rm.getTurnOrder()) {
            if (!p.isBoughtThisRound()) {
                currentBidders.add(p);
            }
        }

        // The initiator just bid, so it's the next eligible person's turn
        activeBidderIndex = currentBidders.indexOf(initiator);
        advanceBidder();
    }

    private void advanceBidder() {
        activeBidderIndex = (activeBidderIndex + 1) % currentBidders.size();

        // If everyone else passed and only 1 bidder remains, they win.
        if (currentBidders.size() == 1) {
            resolveAuction();
        }
    }

    private void resolveAuction() {
        RoundManager rm = frame.getRoundManager();
        Player winner = rm.resolveAuction();

        // FIX: Reset the state BEFORE showing the blocking JOptionPane
        isAuctionActive = false;
        currentBidders.clear();
        bidInput.setText("");

        if (winner != null) {
            JOptionPane.showMessageDialog(this, winner.getName() + " won the plant!");
        }

        // The initiator's turn starts again if someone ELSE won their plant.
        findNextInitiator();
    }

    public void handleBid(int amount) {
        RoundManager rm = frame.getRoundManager();

        if (!isAuctionActive) {
            // --- STARTING A NEW AUCTION ---
            Player initiator = rm.getTurnOrder().get(auctionInitiatorIndex);
            Powerplant selectedPlant = (Powerplant) powerplantDropdown.getSelectedItem();

            if (selectedPlant == null) return;

            if (amount < selectedPlant.getNumber()) {
                JOptionPane.showMessageDialog(this, "Starting bid must be at least the plant's face value.");
                return;
            }

            boolean success = rm.startAuction(initiator, selectedPlant);
            if (success) {
                // If they bid higher than face value, set that bid
                if (amount > selectedPlant.getNumber()) {
                    rm.placeBid(initiator, amount);
                }
                isAuctionActive = true;
                powerplantDropdown.setVisible(false); // Hide dropdown during bidding
                setupBiddersForAuction(initiator);
            } else {
                JOptionPane.showMessageDialog(this, "Cannot afford this plant.");
            }
        } else {
            // --- PLACING A BID IN AN ACTIVE AUCTION ---
            Player currentBidder = currentBidders.get(activeBidderIndex);
            boolean success = rm.placeBid(currentBidder, amount);

            if (success) {
                advanceBidder();
            } else {
                JOptionPane.showMessageDialog(this, "Bid must be higher than current bid and affordable.");
            }
        }
        repaint();
    }

    public void handlePass() {
        RoundManager rm = frame.getRoundManager();

        if (!isAuctionActive) {
            // --- PASSING OUT OF THE ENTIRE AUCTION PHASE ---

            // NEW RULE CHECK: Cannot pass out of the auction in round 1
            if (rm.isFirstRound()) {
                JOptionPane.showMessageDialog(this, "You cannot pass in the first round. You must select a plant to auction!");
                return; // Stop the pass action here
            }

            Player initiator = rm.getTurnOrder().get(auctionInitiatorIndex);
            initiator.setPassedAuction(true);
            findNextInitiator();
        } else {
            // --- PASSING ON THE CURRENT PLANT ---
            currentBidders.remove(activeBidderIndex);

            // Adjust index because the list shrank
            if (activeBidderIndex >= currentBidders.size()) {
                activeBidderIndex = 0;
            }

            // If only one person left, they win automatically
            if (currentBidders.size() == 1) {
                resolveAuction();
            }
        }
        repaint();
    }

    /**
     * Helper to load and cache plant images on demand
     */
    private BufferedImage getPlantImage(int plantNumber) {
        if (!plantImages.containsKey(plantNumber)) {
            try {
                BufferedImage img = ImageIO.read(BiddingPanel.class.getResource("/Images/powerplant-" + plantNumber + ".png"));
                plantImages.put(plantNumber, img);
            } catch (Exception e) {
                System.err.println("Missing image for plant: " + plantNumber);
                return null;
            }
        }
        return plantImages.get(plantNumber);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        }

        RoundManager rm = frame.getRoundManager();
        if (rm == null || rm.getTurnOrder().isEmpty()) return;

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.WHITE);

        if (!isAuctionActive) {
            // --- UI FOR SELECTION PHASE ---
            Player initiator = rm.getTurnOrder().get(auctionInitiatorIndex);
            g.drawString("Waiting for: " + initiator.getName() + " to pick a plant or Pass", 963, 100);

            // FIX: Copy and sort the market before drawing
            int x = 918;
            List<Powerplant> sortedMarket = new ArrayList<>(rm.getDeck().getCurrentMarket());
            sortedMarket.sort(Comparator.comparingInt(Powerplant::getNumber));

            for (Powerplant p : sortedMarket) {
                BufferedImage img = getPlantImage(p.getNumber());
                if (img != null) {
                    g.drawImage(img, x, 209, 122, 122, null);
                }
                else {
                    System.out.println("No image for " + p.getNumber());
                }
                x += 132;
            }

        } else {
            // --- UI FOR BIDDING PHASE ---
            Player activeBidder = currentBidders.get(activeBidderIndex);
            g.drawString("Current Turn: " + activeBidder.getName(), 963, 100);

            // FIX: Safely check if highest bidder is null to prevent NPE
            Player highestBidder = rm.getCurrentHighestBidder();
            String bidderName = (highestBidder != null) ? highestBidder.getName() : "None";
            g.drawString("Highest Bidder: " + bidderName, 963, 140);

            g.drawString("Current Bid: " + rm.getCurrentHighBid() + " Elektro", 963, 180);

            // Draw the plant currently being auctioned in the center
            Powerplant auctionPlant = rm.getCurrentAuctionPlant();
            if (auctionPlant != null) {
                BufferedImage img = getPlantImage(auctionPlant.getNumber());
                if (img != null) {
                    g.drawImage(img, 996, 400, 228, 228, null);
                }
            }
        }

        paintResourceIcons(g);
        paintHand(g);
    }

    private void refreshDropdown() {
        RoundManager rm = frame.getRoundManager();
        if (rm == null) return;

        powerplantDropdown.removeAllItems();
        powerplantDropdown.setVisible(true);

        // FIX: Copy to a List and explicitly sort it
        List<Powerplant> sortedMarket = new ArrayList<>(rm.getDeck().getCurrentMarket());
        sortedMarket.sort(Comparator.comparingInt(Powerplant::getNumber));

        for (Powerplant p : sortedMarket) {
            powerplantDropdown.addItem(p);
        }
    }

    private void paintHand(Graphics g) {
        RoundManager rm = frame.getRoundManager();
        if (rm == null || rm.getTurnOrder().isEmpty()) return;

        Player player;

        // Determine who the current player is based on the auction state
        if (isAuctionActive) {
            // An auction is happening: get the person whose turn it is to bid
            if (currentBidders.isEmpty() || activeBidderIndex >= currentBidders.size()) return;
            player = currentBidders.get(activeBidderIndex);
        } else {
            // No auction is happening: get the person whose turn it is to pick a plant
            if (auctionInitiatorIndex >= rm.getTurnOrder().size()) return;
            player = rm.getTurnOrder().get(auctionInitiatorIndex);
        }

        if (player == null) return;

        // Player colour oval
        Color playerColor = parseColor(player.getColor());
        g.setColor(playerColor);
        g.fillOval(1460, 695, 120, 120);

        // Player name
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString(player.getName(), 1180, 700);

        // Powerplants owned – load images dynamically by plant number
        List<Powerplant> plants = new ArrayList<>(player.getPowerplants());
        int px = 900;
        for (int i = 0; i < Math.min(plants.size(), 3); i++) {
            int plantNum = plants.get(i).getNumber();
            BufferedImage img = getPlantImage(plantNum);
            if (img != null) {
                g.drawImage(img, px, 770, 180, 180, null);
            } else {
                // Fallback: draw a labelled rectangle if image missing
                g.setColor(new Color(60, 60, 80));
                g.fillRect(px, 770, 180, 180);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 28));
                g.drawString("#" + plantNum, px + 55, 870);
            }
            px += 185;
        }

        // Money
        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.setColor(Color.WHITE);
        g.drawString("$" + player.getMoney(), 1480, 770);

        // Resource inventory (stored across all their powerplants)
        paintInventory(g, player);
    }

    private void paintInventory(Graphics g, Player player) {
        // Retrieve totals – assumes Player exposes getStoredResources()
        // returning Map<ResourceType, Integer> (sum across all powerplants).
        Map<ResourceType, Integer> stored;
        try {
            stored = player.getStoredResources();
        } catch (Exception ex) {
            stored = Collections.emptyMap();
        }

        int coal    = stored.getOrDefault(ResourceType.COAL,    0);
        int oil     = stored.getOrDefault(ResourceType.OIL,     0);
        int trash   = stored.getOrDefault(ResourceType.TRASH,   0);
        int uranium = stored.getOrDefault(ResourceType.URANIUM, 0);

        // Colour swatches
        g.setColor(new Color(92, 50, 5));
        g.fillRect(1475, 832, 25, 25);
        g.setColor(Color.BLACK);
        g.fillRect(1475, 862, 25, 25);
        g.setColor(Color.YELLOW);
        g.fillRect(1475, 892, 25, 25);
        g.setColor(Color.RED);
        g.fillRect(1475, 922, 25, 25);

        // Counts
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(coal),    1510, 850);
        g.drawString(String.valueOf(oil),     1510, 880);
        g.drawString(String.valueOf(trash),   1510, 910);
        g.drawString(String.valueOf(uranium), 1510, 940);
    }

    private void paintResourceIcons(Graphics g) {
        ResourceMarket market = frame.getRoundManager().getResourceMarket();

        int coalCount    = market.getAvailableAmount(ResourceType.COAL);
        int oilCount     = market.getAvailableAmount(ResourceType.OIL);
        int trashCount   = market.getAvailableAmount(ResourceType.TRASH);
        int uraniumCount = market.getAvailableAmount(ResourceType.URANIUM);

        // --- COAL (brown rectangles, up to 24) ---
        int x = 769, y = 902;
        for (int i = 0; i < coalCount; i++) {
            g.setColor(new Color(99, 68, 38));
            g.fillRect(x, y, 18, 12);
            switch (i) {
                case 2  -> x = 671;
                case 5  -> x = 572;
                case 8  -> x = 474;
                case 11 -> x = 376;
                case 14 -> { x = 276; y = 906; }
                case 17 -> x = 180;
                case 20 -> x = 81;
                default -> x -= 28;
            }
        }

        // --- OIL (dark rectangles, up to 24) ---
        x = 753; y = 922;
        for (int i = 0; i < oilCount; i++) {
            g.setColor(new Color(12, 37, 48));
            g.fillRect(x, y, 12, 11);
            switch (i) {
                case 2  -> { x = 655; y = 924; }
                case 5  -> x = 556;
                case 8  -> x = 458;
                case 11 -> x = 359;
                case 14 -> x = 260;
                case 17 -> x = 160;
                case 20 -> x = 63;
                default -> x -= 21;
            }
        }

        // --- TRASH (yellow rectangles, up to 24) ---
        x = 769; y = 940;
        for (int i = 0; i < trashCount; i++) {
            g.setColor(new Color(245, 213, 84));
            g.fillRect(x, y, 18, 12);
            switch (i) {
                case 2  -> x = 671;
                case 5  -> x = 572;
                case 8  -> x = 474;
                case 11 -> x = 376;
                case 14 -> { x = 276; y = 945; }
                case 17 -> x = 180;
                case 20 -> x = 81;
                default -> x -= 28;
            }
        }

        // --- URANIUM (red rectangles, up to 12) ---
        x = 848; y = 936;
        int w = 16, h = 14;
        for (int i = 0; i < uraniumCount; i++) {
            g.setColor(new Color(213, 82, 68));
            g.fillRect(x, y, w, h);
            switch (i) {
                case 0 -> { x = 808; y = 937; }
                case 1 -> { x = 848; y = 909; }
                case 2 -> { x = 809; y = 902; }
                case 3 -> { x = 774; y = 925; w = 13; h = 10; }
                default -> x -= 98;
            }
        }
    }

    private Color parseColor(String colorName) {
        if (colorName == null) return Color.LIGHT_GRAY;
        return switch (colorName.toLowerCase()) {
            case "red"    -> new Color(220, 60,  60);
            case "blue"   -> new Color(60,  100, 200);
            case "green"  -> new Color(60,  160, 80);
            case "yellow" -> new Color(220, 190, 50);
            case "purple" -> new Color(130, 60,  180);
            case "black"  -> Color.DARK_GRAY;
            default       -> Color.LIGHT_GRAY;
        };
    }



    // Unused MouseListener methods
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}