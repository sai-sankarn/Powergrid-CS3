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

    /**
     * True when the Step 3 card has been drawn during the auction phase but Step 3
     * has not yet officially started.  Per the rulebook the card is treated as the
     * highest-valued plant on the market until the end of the auction; Step 3 then
     * begins after Phase 4 (building) is completed.
     */
    private boolean step3CardPending = false;

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

    // -----------------------------------------------------------------------
    // GameResult — immutable snapshot of the final game state
    // -----------------------------------------------------------------------

    /**
     * Snapshot of all player stats at the end of the game, plus the determined winner.
     * Built once by determineWinner(Map) and retrieved by the UI via getGameResult().
     */
    public static class GameResult {

        /** Per-player final stats. */
        public static class PlayerResult {
            public final Player player;
            public final int citiesPowered;  // cities actually powered in the final round
            public final int money;          // Elektro at end of game
            public final int citiesOwned;    // cities on the board
            public final boolean isWinner;

            public PlayerResult(Player player, int citiesPowered,
                                int money, int citiesOwned, boolean isWinner) {
                this.player        = player;
                this.citiesPowered = citiesPowered;
                this.money         = money;
                this.citiesOwned   = citiesOwned;
                this.isWinner      = isWinner;
            }
        }

        public final Player winner;
        public final List<PlayerResult> rankedResults; // sorted: best → worst

        public GameResult(Player winner, List<PlayerResult> rankedResults) {
            this.winner        = winner;
            this.rankedResults = Collections.unmodifiableList(rankedResults);
        }
    }

    /** Set at the end of the final Bureaucracy phase; null until then. */
    private GameResult gameResult = null;

    /** Returns the final game result, or null if the game is not yet over. */
    public GameResult getGameResult() { return gameResult; }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

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

    /**
     * Returns true if the Step 3 card was drawn during the auction phase but Step 3
     * has not yet officially activated (i.e., it is being held as the "highest plant"
     * in the market for the remainder of the auction).
     */
    public boolean isStep3CardPending()         { return step3CardPending; }

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
     * draws a replacement card, and handles the Step 3 card if drawn.
     * Returns the winning player.
     */
    /**
     * Resolves the current auction: charges the winner, removes the plant from
     * the market (which triggers an automatic rebalance inside PowerplantDeck),
     * checks for the Step 3 card, removes obsolete plants, and gives the plant
     * to the winner.
     *
     * NOTE: we no longer call deck.draw() or deck.updateMarket() manually here.
     * PowerplantDeck.removeFromCurrentMarket() already calls rebalanceMarkets()
     * internally, which fills the gap via drawSafe() — doing so again would
     * double-draw from the pile and corrupt the market.
     *
     * Step 3 detection is delegated to the deck: after rebalancing, if drawSafe()
     * encountered the Step 3 card it sets an internal flag that we check here.
     */
    public Player resolveAuction() {
        if (currentHighestBidder == null || currentAuctionPlant == null) return null;

        Player winner = currentHighestBidder;
        Powerplant plant = currentAuctionPlant;

        winner.spendMoney(currentHighBid);

        // removeFromCurrentMarket triggers rebalanceMarkets() inside the deck,
        // which fills the market gap via drawSafe(). If drawSafe() hit the Step 3
        // card during that refill, the deck records it and we pick it up below.
        deck.removeFromCurrentMarket(plant);

        // Check whether rebalancing just encountered the Step 3 card.
        if (deck.wasStep3CardEncountered()) {
            handleStep3Trigger();
        }

        // Remove any plants the leading player has already surpassed.
        deck.removeObsoletePlants(getLeadingCityCount());

        boolean added = winner.addPowerplant(plant);
        if (!added) {
            // Player already has 3 plants — UI must call resolvePlantDiscard().
        }

        winner.setBoughtThisRound(true);

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
        if (toDiscard.equals(newPlant)) {
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
     */
    public void handleBuyResourcesPhase() {
        currentPlayerIndex = turnOrder.size() - 1;
    }

    /**
     * Executes a resource purchase of 1 unit of {@code type} for {@code player}.
     *
     * Distribution strategy — "fill minimums first":
     *   Pass 1: find the first compatible plant whose stored amount of this resource
     *           is still below its resourceIntake (the minimum required to fire once).
     *           This ensures every plant reaches its firing minimum before any plant
     *           starts accumulating surplus tokens.
     *   Pass 2: if all compatible plants already meet their firing minimum, fall back
     *           to the first plant that still has any remaining free capacity.
     *
     * Example: player has a coal-2 plant (needs 2 coal) and a coal-3 plant (needs 3 coal).
     *   Purchases 1–2 go to the coal-2 plant (filling its minimum).
     *   Purchases 3–5 go to the coal-3 plant (filling its minimum).
     *   Purchases 6+ overflow into whichever plant still has free capacity.
     *
     * The {@code targetPlant} parameter is retained for API compatibility but is
     * ignored; the distribution logic selects the target automatically.
     *
     * Returns the cost paid (≥ 0) on success, or -1 on failure (no market supply,
     * insufficient funds, or no compatible plant has capacity).
     */
    public int buyResource(Player player, Powerplant targetPlant,
                           ResourceType type, int amount) {
        // Validate cost and funds before touching any plant state.
        int cost = resourceMarket.calculateCost(type, amount);
        if (cost < 0) return -1;
        if (!player.canAfford(cost)) return -1;

        // Collect all plants that accept this resource type and still have room.
        List<Powerplant> compatible = new ArrayList<>();
        for (Powerplant p : player.getPowerplants()) {
            if (player.canStoreResource(p, type, 1)) {
                compatible.add(p);
            }
        }
        if (compatible.isEmpty()) return -1;

        // Pass 1: prefer a plant that hasn't yet reached its firing minimum.
        Powerplant chosen = null;
        for (Powerplant p : compatible) {
            if (p.getStoredAmount(type) < p.getResourceIntake()) {
                chosen = p;
                break;
            }
        }

        // Pass 2: all plants are at or above their minimum — use first plant with space.
        if (chosen == null) {
            chosen = compatible.get(0);
        }

        // Commit: deduct from market and wallet, then place token on chosen plant.
        int actual = resourceMarket.buyResource(type, amount);
        player.spendMoney(actual);
        player.addResource(chosen, type, amount);
        return actual;
    }

    /**
     * BUILDING phase.
     * Iterates in reverse. Actual city selection is UI-driven.
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
        if (!board.getActiveCities().contains(targetCity)) return -1;

        if (player.getOwnedCities().isEmpty()) {
            if (!player.canAfford(10)) return -1;
            if (!targetCity.hasOpenSlot(currentStep)) return -1;
            if (targetCity.isOccupiedBy(player)) return -1;

            player.spendMoney(10);
            targetCity.addOccupant(player, currentStep);
            player.addCity(targetCity);
            return 10;
        }

        int connectionCost = board.calculateConnectionCost(player.getOwnedCities(), targetCity);
        if (connectionCost == Integer.MAX_VALUE) return -1;
        int slotCost = targetCity.getNextSlotCost(currentStep);
        if (slotCost < 0) return -1;
        int total = connectionCost + slotCost;
        if (!player.canAfford(total)) return -1;
        if (targetCity.isOccupiedBy(player)) return -1;

        player.spendMoney(total);
        targetCity.addOccupant(player, currentStep);
        player.addCity(targetCity);

        deck.removeObsoletePlants(getLeadingCityCount());

        return total;
    }

    /**
     * BUREAUCRACY phase — all four sub-phases in sequence.
     */
    public void handleBureaucracy() {
        checkStepTransition();

        boolean isFinalRound = checkVictory();
        if (!isFinalRound) {
            payPlayers();
        } else {
            determineWinner(Collections.emptyMap());
            return;
        }

        resourceMarket.restock(players.size(), currentStep);

        if (currentStep == 1 || currentStep == 2) {
            deck.discardHighestPlant();
        } else {
            deck.removeLowestPlant();
        }

        // The deck's rebalance (called internally by the above) may have encountered
        // the Step 3 card for the first time during Bureaucracy (Phase 5). The rulebook
        // explicitly covers this case: the card is removed, the deck reshuffled, and
        // Step 3 begins at the start of the next round (Determine Player Order).
        // Without this check the flag is silently cleared on the next rebalance call,
        // Step 3 never activates, and the game can become unwinnable.
        if (deck.wasStep3CardEncountered()) {
            step3CardPending = true;
            System.out.println("Step 3 card drawn during Bureaucracy — will activate next round.");
        }

        nextPhase();
    }

    /**
     * Each player declares how many cities they power and earns money accordingly.
     */
    private void payPlayers() {
        for (Player player : turnOrder) {
            int powerableCount = Math.min(player.getActualPowerableCount(), player.getCityCount());
            for (Powerplant p : player.getPowerplants()) {
                if (p.canFire() && powerableCount > 0) {
                    Map<ResourceType, Integer> consumed = p.fire();
                    powerableCount -= p.getHouseOutput();
                }
            }
            int citiesPowered = Math.min(player.getActualPowerableCount(), player.getCityCount());
            int payment = getPayment(citiesPowered);
            player.addMoney(payment);
        }
    }

    /**
     * Determines the winner and builds a {@link GameResult} that the UI can display.
     *
     * {@code citiesPoweredMap} must contain each player's final powered-city count
     * (recorded by BureaucracyPanel before plant.fire() consumes resources).
     * Pass an empty map when calling from the non-interactive handleBureaucracy() path.
     *
     * Tiebreak order: powered cities → most money → most cities built.
     */
    public void determineWinner(Map<Player, Integer> citiesPoweredMap) {
        Player winner   = null;
        int bestPowered = -1;
        int bestMoney   = -1;
        int bestCities  = -1;

        for (Player p : players) {
            int powered = citiesPoweredMap.containsKey(p)
                    ? citiesPoweredMap.get(p)
                    : Math.min(p.getActualPowerableCount(), p.getCityCount());
            int money  = p.getMoney();
            int cities = p.getCityCount();

            boolean winsOnPower  = powered > bestPowered;
            boolean tiesOnPower  = powered == bestPowered;
            boolean winsOnMoney  = tiesOnPower && money > bestMoney;
            boolean tiesOnMoney  = tiesOnPower && money == bestMoney;
            boolean winsOnCities = tiesOnMoney && cities > bestCities;

            if (winsOnPower || winsOnMoney || winsOnCities) {
                winner      = p;
                bestPowered = powered;
                bestMoney   = money;
                bestCities  = cities;
            }
        }

        final Player finalWinner = winner;
        List<GameResult.PlayerResult> results = new ArrayList<>();
        for (Player p : players) {
            int powered = citiesPoweredMap.containsKey(p)
                    ? citiesPoweredMap.get(p)
                    : Math.min(p.getActualPowerableCount(), p.getCityCount());
            results.add(new GameResult.PlayerResult(
                    p, powered, p.getMoney(), p.getCityCount(), p.equals(finalWinner)));
        }
        results.sort((a, b) -> {
            if (b.citiesPowered != a.citiesPowered) return b.citiesPowered - a.citiesPowered;
            if (b.money != a.money) return b.money - a.money;
            return b.citiesOwned - a.citiesOwned;
        });

        gameResult = new GameResult(winner, results);
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
     * Checks and applies step transitions. Called at the START of the Bureaucracy phase.
     */
    public void checkStepTransition() {
        if (step3CardPending) {
            step3CardPending = false;
            currentStep = 3;
            deck.removeLowestPlant();
            deck.activateStepThree();
            System.out.println("Step 3 activated (card drawn during auction).");
            return;
        }

        if (currentStep == 1) {
            int threshold = STEP2_THRESHOLDS.getOrDefault(players.size(), 7);
            for (Player p : players) {
                if (p.getCityCount() >= threshold) {
                    currentStep = 2;
                    deck.removeLowestPlant();
                    System.out.println("Step 2 activated (city threshold reached).");
                    return;
                }
            }
        }
    }

    /**
     * Called when the Step 3 card is drawn from the deck (during Phase 2 – Auction).
     */
    public void handleStep3Trigger() {
        step3CardPending = true;
        System.out.println("Step 3 card drawn during auction — will activate after Phase 4.");
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