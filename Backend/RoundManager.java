package Backend;

import java.util.*;

public class RoundManager {
    private List<Player> players;       // full roster, never reordered
    private List<Player> turnOrder;     // re-sorted each Phase 1
    private int currentPlayerIndex;
    private GameState currentState;
    private int currentStep;
    private boolean isFirstRound;
    private Gameboard board;
    private ResourceMarket resourceMarket;
    private PowerplantDeck deck;
    private Powerplant currentAuctionPlant;
    private Player currentHighestBidder;
    private int currentHighBid;

    // Win thresholds: minimum cities to trigger end-game check, keyed by player count
    private static final Map<Integer, Integer> WIN_THRESHOLDS;
    // Step 2 unlock thresholds, keyed by player count
    private static final Map<Integer, Integer> STEP2_THRESHOLDS;

    static {
        WIN_THRESHOLDS = new HashMap<>();
        WIN_THRESHOLDS.put(2, 18);
        WIN_THRESHOLDS.put(3, 17);
        WIN_THRESHOLDS.put(4, 17);
        WIN_THRESHOLDS.put(5, 15);
        WIN_THRESHOLDS.put(6, 14);

        STEP2_THRESHOLDS = new HashMap<>();
        STEP2_THRESHOLDS.put(2, 7);
        STEP2_THRESHOLDS.put(3, 7);
        STEP2_THRESHOLDS.put(4, 7);
        STEP2_THRESHOLDS.put(5, 7);
        STEP2_THRESHOLDS.put(6, 6);
    }

    // Payment table from rulebook: index = number of cities powered
    private static final int[] PAYMENT_TABLE = {
        10, 22, 33, 44, 54, 64, 73, 82, 90, 98,
        105, 112, 118, 124, 129, 134, 138, 142, 145, 148,
        150  // 20 cities = 150
    };

    public RoundManager(List<Player> players, Gameboard board,
                        ResourceMarket resourceMarket, PowerplantDeck deck) {
        this.players = new ArrayList<>(players);
        this.turnOrder = new ArrayList<>(players);
        this.board = board;
        this.resourceMarket = resourceMarket;
        this.deck = deck;
        this.currentPlayerIndex = 0;
        this.currentStep = 1;
        this.isFirstRound = true;
        this.currentState = GameState.DETERMINE_ORDER;

        deck.setup(3);
    }

    // --- Getters ---

    public List<Player> getPlayers()            { return players; }
    public List<Player> getTurnOrder()          { return turnOrder; }
    public int getCurrentPlayerIndex()          { return currentPlayerIndex; }
    public GameState getCurrentState()          { return currentState; }
    public int getCurrentStep()                 { return currentStep; }
    public boolean isFirstRound()               { return isFirstRound; }
    public Gameboard getBoard()                 { return board; }
    public ResourceMarket getResourceMarket()   { return resourceMarket; }
    public PowerplantDeck getDeck()             { return deck; }
    public Powerplant getCurrentAuctionPlant()  { return currentAuctionPlant; }
    public Player getCurrentHighestBidder()     { return currentHighestBidder; }
    public int getCurrentHighBid()              { return currentHighBid; }

    // --- Setters ---

    public void setCurrentState(GameState state) { this.currentState = state; }
    public void setCurrentAuctionPlant(Powerplant p) { this.currentAuctionPlant = p; }
    public void setCurrentHighestBidder(Player p)    { this.currentHighestBidder = p; }
    public void setCurrentHighBid(int bid)           { this.currentHighBid = bid; }

    // --- Turn order ---

    /**
     * Sorts turnOrder:
     *   Primary: most cities owned (descending)
     *   Tiebreak: highest plant number (descending)
     * Resets currentPlayerIndex to 0.
     */
    public void updateTurnOrder() {
        turnOrder.sort((a, b) -> {
            int cityCmp = Integer.compare(b.getCityCount(), a.getCityCount());
            if (cityCmp != 0) return cityCmp;
            return Integer.compare(b.getHighestPlantNumber(), a.getHighestPlantNumber());
        });
        currentPlayerIndex = 0;
    }

    /**
     * Advances to the next player in turnOrder.
     * Returns the player whose turn it now is, or null if all players have acted.
     */
    public Player nextPlayer() {
        currentPlayerIndex++;
        if (currentPlayerIndex >= turnOrder.size()) return null;
        return turnOrder.get(currentPlayerIndex);
    }

    /**
     * Returns the player currently acting.
     */
    public Player currentPlayer() {
        if (currentPlayerIndex < 0 || currentPlayerIndex >= turnOrder.size()) return null;
        return turnOrder.get(currentPlayerIndex);
    }

    // --- Phase transitions ---

    /**
     * Advances the game to the next GameState in sequence.
     * Resets per-phase flags on all players.
     */
    public void nextPhase() {
        switch (currentState) {
            case DETERMINE_ORDER:
                currentState = GameState.AUCTION;
                resetAuctionFlags();
                break;
            case AUCTION:
                currentState = GameState.BUYING;
                currentPlayerIndex = turnOrder.size() - 1; // buying is reverse order
                break;
            case BUYING:
                currentState = GameState.BUILDING;
                currentPlayerIndex = turnOrder.size() - 1; // building is reverse order
                break;
            case BUILDING:
                currentState = GameState.BUREAUCRACY;
                currentPlayerIndex = 0;
                break;
            case BUREAUCRACY:
                currentState = GameState.DETERMINE_ORDER;
                currentPlayerIndex = 0;
                isFirstRound = false;
                break;
        }
    }

    private void resetAuctionFlags() {
        for (Player p : players) {
            p.setPassedAuction(false);
            p.setBoughtThisRound(false);
        }
        currentAuctionPlant = null;
        currentHighestBidder = null;
        currentHighBid = 0;
    }

    // --- Phase handlers ---

    /**
     * DETERMINE_ORDER phase.
     * Skipped on the first round (order was set randomly at setup).
     */
    public void handleDetermineOrder() {
        if (isFirstRound) {
            // skip — order was set randomly at setup
            nextPhase();
            return;
        }
        updateTurnOrder();
        nextPhase();
    }

    /**
     * AUCTION phase entry point.
     * Resets per-round flags and sets currentPlayerIndex to 0.
     * The actual bidding loop is driven by the frontend/UI calling
     * startAuction(), placeBid(), and passBid() below.
     */
    public void handleAuctionPhase() {
        resetAuctionFlags();
        currentPlayerIndex = 0;
    }

    /**
     * Called by the UI when the current auctioning player selects a plant to auction.
     * Sets up auction state. Returns false if the player can't afford the minimum bid.
     */
    public boolean startAuction(Player auctioningPlayer, Powerplant plant) {
        if (!deck.getCurrentMarket().contains(plant)) return false;
        if (!auctioningPlayer.canAfford(plant.getNumber())) return false;
        currentAuctionPlant = plant;
        currentHighBid = plant.getNumber();
        currentHighestBidder = auctioningPlayer;
        return true;
    }

    /**
     * Called when a player raises the bid.
     * Returns false if the bid is invalid (too low or player can't afford).
     */
    public boolean placeBid(Player bidder, int amount) {
        if (amount <= currentHighBid) return false;
        if (!bidder.canAfford(amount)) return false;
        currentHighBid = amount;
        currentHighestBidder = bidder;
        return true;
    }

    /**
     * Resolves the current auction: charges the winner, gives them the plant,
     * draws a replacement card, and handles Step 3 trigger if needed.
     * Returns the winning player.
     */
    public Player resolveAuction() {
        if (currentHighestBidder == null || currentAuctionPlant == null) return null;

        Player winner = currentHighestBidder;
        Powerplant plant = currentAuctionPlant;

        winner.spendMoney(currentHighBid);
        deck.removeFromCurrentMarket(plant);

        // draw replacement — check for Step 3 card
        Powerplant drawn = deck.draw();
        if (drawn != null && deck.isStep3Card(drawn)) {
            handleStep3Trigger();
        } else if (drawn != null) {
            // add to future market then resort
            if (!deck.isStepThreeActive()) {
                // goes into future market; updateMarket will slot it correctly
            }
            deck.updateMarket();
        } else {
            deck.updateMarket();
        }

        // remove any plants whose number <= leading player city count
        int leadingCities = getLeadingCityCount();
        deck.removeObsoletePlants(leadingCities);

        // give the plant to the winner
        boolean added = winner.addPowerplant(plant);
        if (!added) {
            // player already has 3 — they must discard one (UI prompts this)
            // for now, return without adding; UI must call resolvePlantDiscard()
        }

        winner.setBoughtThisRound(true);

        // reset auction state
        currentAuctionPlant = null;
        currentHighestBidder = null;
        currentHighBid = 0;

        return winner;
    }

    /**
     * Called after the UI has the player choose which plant to discard (when they have 3).
     * Handles resource redistribution and returns leftover tokens (caller returns to supply).
     */
    public Map<ResourceType, Integer> resolvePlantDiscard(Player player, Powerplant toDiscard,
                                                           Powerplant newPlant) {
        Map<ResourceType, Integer> leftover = player.discardPowerplant(toDiscard);
        // if the discarded plant IS the newly bought plant, it is removed from the game entirely
        if (toDiscard.equals(newPlant)) {
            // already removed by discardPowerplant; don't re-add newPlant
            return leftover;
        }
        player.addPowerplant(newPlant);
        return leftover;
    }

    /**
     * Last player in turn order special rule: pays minimum bid automatically.
     */
    public void autoResolveLast(Player lastPlayer) {
        if (lastPlayer.isBoughtThisRound() || lastPlayer.isPassedAuction()) return;
        // find cheapest plant in market
        Powerplant cheapest = deck.getCurrentMarket().peek();
        if (cheapest == null) return;
        if (!lastPlayer.canAfford(cheapest.getNumber())) return;

        currentAuctionPlant = cheapest;
        currentHighBid = cheapest.getNumber();
        currentHighestBidder = lastPlayer;
        resolveAuction();
    }

    /**
     * BUYING phase.
     * Iterates through turnOrder in reverse. Actual resource selection is UI-driven.
     * This method validates and executes a single resource purchase.
     */
    public void handleBuyResourcesPhase() {
        currentPlayerIndex = turnOrder.size() - 1;
    }

    /**
     * Executes a resource purchase for a player during the buying phase.
     * Returns the cost paid, or -1 if the purchase failed.
     */
    public int buyResource(Player player, Powerplant targetPlant,
                           ResourceType type, int amount) {
        int cost = resourceMarket.calculateCost(type, amount);
        if (cost < 0) return -1;
        if (!player.canAfford(cost)) return -1;
        if (!player.canStoreResource(targetPlant, type, amount)) return -1;

        int actual = resourceMarket.buyResource(type, amount);
        player.spendMoney(actual);
        player.addResource(targetPlant, type, amount);
        return actual;
    }

    /**
     * BUILDING phase.
     * Iterates in reverse. Actual city selection is UI-driven.
     * This executes a single city build.
     */
    public void handleBuildHousesPhase() {
        currentPlayerIndex = turnOrder.size() - 1;
    }

    /**
     * Executes a city build for a player.
     * Handles both first-city (flat 10, no connection cost) and subsequent cities.
     * Returns total cost paid, or -1 on failure.
     */
    public int buildCity(Player player, City targetCity) {
        // FIX: Ensure the city is in one of the 3 selected regions
        if (!board.getActiveCities().contains(targetCity)) return -1;

        if (player.getOwnedCities().isEmpty()) {
            // first city: flat 10, no connection cost
            if (!player.canAfford(10)) return -1;
            if (!targetCity.hasOpenSlot(currentStep)) return -1;
            if (targetCity.isOccupiedBy(player)) return -1;

            player.spendMoney(10);
            targetCity.addOccupant(player, currentStep);
            player.addCity(targetCity);
            return 10;
        }

        // subsequent cities: connection + slot cost
        int connectionCost = board.calculateConnectionCost(player.getOwnedCities(), targetCity);
        if (connectionCost == Integer.MAX_VALUE) return -1; // unreachable
        int slotCost = targetCity.getNextSlotCost(currentStep);
        if (slotCost < 0) return -1;
        int total = connectionCost + slotCost;
        if (!player.canAfford(total)) return -1;
        if (targetCity.isOccupiedBy(player)) return -1;

        player.spendMoney(total);
        targetCity.addOccupant(player, currentStep);
        player.addCity(targetCity);

        // remove obsolete plants immediately after build
        deck.removeObsoletePlants(getLeadingCityCount());

        return total;
    }

    /**
     * BUREAUCRACY phase — all four sub-phases in sequence.
     */
    public void handleBureaucracy() {
        // Phase 1: step transitions
        checkStepTransition();

        // Phase 2: earn cash (or determine winner if final round)
        boolean isFinalRound = checkVictory();
        if (!isFinalRound) {
            payPlayers();
        } else {
            determineWinner();
            return; // game over
        }

        // Phase 3: restock resource market
        resourceMarket.restock(players.size(), currentStep);

        // Phase 4: update power plant market
        if (currentStep == 1 || currentStep == 2) {
            deck.discardHighestPlant();
        } else {
            deck.removeLowestPlant();
        }

        // advance to next round
        nextPhase();
    }

    /**
     * Each player declares how many cities they power and earns money accordingly.
     * Fires plants and decrements their stored resources.
     * Minimum payout is always 10 Elektro.
     */
    private void payPlayers() {
        for (Player player : turnOrder) {
            int powerableCount = Math.min(player.getActualPowerableCount(), player.getCityCount());
            // fire the plants
            for (Powerplant p : player.getPowerplants()) {
                if (p.canFire() && powerableCount > 0) {
                    Map<ResourceType, Integer> consumed = p.fire();
                    powerableCount -= p.getHouseOutput();
                    // consumed resources go back to supply (not the market — tracked externally)
                }
            }
            // look up payment
            int citiesPowered = Math.min(player.getActualPowerableCount(), player.getCityCount());
            int payment = getPayment(citiesPowered);
            player.addMoney(payment);
        }
    }

    /**
     * Final round: no cash. Player who powers the most cities wins.
     * Tiebreak: most money.
     */
    private void determineWinner() {
        Player winner = null;
        int bestPower = -1;
        int bestMoney = -1;

        for (Player p : players) {
            int powered = Math.min(p.getActualPowerableCount(), p.getCityCount());
            if (powered > bestPower || (powered == bestPower && p.getMoney() > bestMoney)) {
                winner = p;
                bestPower = powered;
                bestMoney = p.getMoney();
            }
        }
        System.out.println("GAME OVER. Winner: " + (winner != null ? winner.getName() : "none"));
    }

    /**
     * Returns the payment amount for the given number of cities powered.
     */
    public int getPayment(int citiesPowered) {
        if (citiesPowered < 0) return 10;
        if (citiesPowered >= PAYMENT_TABLE.length) return PAYMENT_TABLE[PAYMENT_TABLE.length - 1];
        return PAYMENT_TABLE[citiesPowered];
    }

    /**
     * Checks if the end-game trigger has been hit.
     * Returns true if any player's city count >= WIN_THRESHOLDS for this player count.
     */
    public boolean checkVictory() {
        int threshold = WIN_THRESHOLDS.getOrDefault(players.size(), 17);
        for (Player p : players) {
            if (p.getCityCount() >= threshold) return true;
        }
        return false;
    }

    /**
     * Checks and handles Step 2 and Step 3 transitions.
     * Step 3 is triggered mid-auction via handleStep3Trigger(); this only checks Step 2.
     */
    public void checkStepTransition() {
        if (currentStep == 1) {
            int threshold = STEP2_THRESHOLDS.getOrDefault(players.size(), 7);
            for (Player p : players) {
                if (p.getCityCount() >= threshold) {
                    currentStep = 2;
                    deck.removeLowestPlant(); // Step 2 transition removes lowest
                    return;
                }
            }
        }
        // Step 3 is handled mid-auction, not here
    }

    /**
     * Called when the Step 3 card is drawn from the deck during auction.
     */
    public void handleStep3Trigger() {
        currentStep = 3;
        deck.activateStepThree();
    }

    // --- Helper ---

    private int getLeadingCityCount() {
        int max = 0;
        for (Player p : players) {
            if (p.getCityCount() > max) max = p.getCityCount();
        }
        return max;
    }
}