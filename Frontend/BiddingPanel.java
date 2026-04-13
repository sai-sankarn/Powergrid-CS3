package Frontend;

import Backend.Player;
import Backend.Powerplant;
import Backend.RoundManager;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        JOptionPane.showMessageDialog(this, winner.getName() + " won the plant!");

        isAuctionActive = false;
        currentBidders.clear();
        bidInput.setText("");

        // The initiator's turn starts again if someone ELSE won their plant.
        // If the initiator won, the loop in findNextInitiator will automatically skip them.
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

    private void refreshDropdown() {
        RoundManager rm = frame.getRoundManager();
        if (rm == null) return;

        powerplantDropdown.removeAllItems();
        powerplantDropdown.setVisible(true);

        // Show only the 4 plants in the CURRENT market
        for (Powerplant p : rm.getDeck().getCurrentMarket()) {
            powerplantDropdown.addItem(p);
        }
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

            // Draw all plants in the current market
            int x = 918;
            for (Powerplant p : rm.getDeck().getCurrentMarket()) {
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
            g.drawString("Highest Bidder: " + rm.getCurrentHighestBidder().getName(), 963, 140);
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
    }

    // Unused MouseListener methods
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}