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
        bidInput.setBounds(1341, 492, 120, 20);
        add(bidInput);

        bidButton = new JButton("BID");
        bidButton.setBounds(1341, 560, 120, 40);
        bidButton.setFont(new Font("Arial", Font.BOLD, 20));
        bidButton.setBackground(new Color(125, 203, 178));
        bidButton.setForeground(Color.white);
        bidButton.setOpaque(true);
        bidButton.setBorderPainted(false);
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
        passButton.setBounds(1341, 636, 120, 40);
        passButton.setFont(new Font("Arial", Font.BOLD, 20));
        passButton.setBackground(new Color(237, 174, 174));
        passButton.setForeground(Color.white);
        passButton.setOpaque(true);
        passButton.setBorderPainted(false);
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

        if (rm.isFirstRound()) {
            // 1. Redetermine order based on the plants just purchased
            rm.updateTurnOrder();
            // 2. Move state to BUYING (nextPhase handles reverse order index)
            rm.nextPhase();
            // 3. Show the new order to players
            frame.showScreen("ORDER");
        } else {
            // Normal flow for subsequent rounds
            rm.nextPhase();
            frame.showScreen("RESOURCE");
        }
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

        // Capture the plant being auctioned before resolution resets it
        Powerplant wonPlant = rm.getCurrentAuctionPlant();
        int wonPlantNumber = (wonPlant != null) ? wonPlant.getNumber() : -1;

        Player winner = rm.resolveAuction();

        // Reset the UI state
        isAuctionActive = false;
        currentBidders.clear();
        bidInput.setText("");

        if (winner != null) {
            // FIX: If the player has 3 plants and the one they won ISN'T one of them,
            // it means addPowerplant() returned false and they need to replace one.
            if (winner.getPowerplants().size() == 3 && !winner.getPowerplants().contains(wonPlant)) {
                handlePlantReplacement(winner, wonPlant);
            } else {
                JOptionPane.showMessageDialog(this, winner.getName() + " won the plant!");
            }
        }

        findNextInitiator();
    }

    /**
     * Triggered when a player wins a 4th plant. Shows a popup to select which
     * of the existing 3 plants to discard to make room for the new one.
     */
    private void handlePlantReplacement(Player winner, Powerplant wonPlant) {
        RoundManager rm = frame.getRoundManager();
        List<Powerplant> plants = winner.getPowerplants();

        // Create buttons for the 3 plants currently owned
        String[] options = new String[3];
        for (int i = 0; i < 3; i++) {
            options[i] = "Discard #" + plants.get(i).getNumber();
        }

        int choice = JOptionPane.showOptionDialog(
                this,
                winner.getName() + " already owns 3 plants.\nChoose an existing plant to replace with #" + wonPlant.getNumber() + ":",
                "Powerplant Limit Reached",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        );

        // Default to discarding the first plant if they close the window
        if (choice == JOptionPane.CLOSED_OPTION) choice = 0;

        Powerplant toDiscard = plants.get(choice);

        // USE BACKEND LOGIC: This handles removing the old, adding the new,
        // and moving resources.
        rm.resolvePlantDiscard(winner, toDiscard, wonPlant);

        JOptionPane.showMessageDialog(this,
                winner.getName() + " discarded #" + toDiscard.getNumber() + " and kept #" + wonPlant.getNumber());

        repaint();
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

            x=918;
            List<Powerplant> sortedFuture = new ArrayList<>(rm.getDeck().getFutureMarket());
            sortedFuture.sort(Comparator.comparingInt(Powerplant::getNumber));
            for (Powerplant p : sortedFuture) {
                BufferedImage img = getPlantImage(p.getNumber());
                if (img != null) {
                    // Draw plant image at reduced opacity by painting grey on top
                    g.drawImage(img, x, 354, 122, 122, null);
                } else {
                    g.setColor(new Color(60, 60, 80));
                    g.fillRect(x, 209, 122, 122);
                }
                // Dark translucent overlay to show it's not buyable
                g.setColor(new Color(0, 0, 0, 140));
                g.fillRect(x, 354, 122, 122);
                // "FUTURE" label
                g.setColor(Color.LIGHT_GRAY);
                g.setFont(new Font("Arial", Font.BOLD, 13));
                g.drawString("FUTURE", x + 28, 354+68);
                g.drawString("#" + p.getNumber(), x + 42, 295);
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

        PanelUtils.paintResourceIcons(g, frame.getRoundManager().getResourceMarket(), frame.getRoundManager());
        PanelUtils.paintHand(g,player,plantImages);
        PanelUtils.paintCities(g, frame.getRoundManager());
        PanelUtils.paintTurnOrderIndicators(g,frame.getRoundManager());
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
    @Override public void mouseClicked(MouseEvent e) {
        System.out.println("(" + e.getX() + ", " + e.getY() + ")");
    }
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}