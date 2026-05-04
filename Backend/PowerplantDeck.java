package Backend;

import java.util.*;

public class PowerplantDeck {
    private LinkedList<Powerplant> drawPile;
    private PriorityQueue<Powerplant> currentMarket;
    private PriorityQueue<Powerplant> futureMarket;
    private PriorityQueue<Powerplant> discardedPlants;
    private boolean stepThreeActive;

    /**
     * Set to true inside drawSafe() whenever the Step 3 card is encountered
     * while refilling the market. Cleared immediately after being read by
     * RoundManager.resolveAuction() via wasStep3CardEncountered().
     * This lets RoundManager react to Step 3 without needing direct access
     * to the draw pile.
     */
    private boolean step3CardEncountered = false;

    private static final int STEP3_CARD_NUMBER = -1;
    private Powerplant step3Card;

    // Plug (03-15) cards to randomly remove and show as discarded, per player count
    private static final Map<Integer, Integer> PLUG_REMOVAL_COUNT = new HashMap<>();
    // Socket (16+) cards to randomly remove and show as discarded, per player count
    private static final Map<Integer, Integer> SOCKET_REMOVAL_COUNT = new HashMap<>();
    static {
        PLUG_REMOVAL_COUNT.put(2, 1);
        PLUG_REMOVAL_COUNT.put(3, 2);
        PLUG_REMOVAL_COUNT.put(4, 1);
        PLUG_REMOVAL_COUNT.put(5, 0);
        PLUG_REMOVAL_COUNT.put(6, 0);

        SOCKET_REMOVAL_COUNT.put(2, 5);
        SOCKET_REMOVAL_COUNT.put(3, 6);
        SOCKET_REMOVAL_COUNT.put(4, 3);
        SOCKET_REMOVAL_COUNT.put(5, 0);
        SOCKET_REMOVAL_COUNT.put(6, 0);
    }

    public PowerplantDeck() {
        this.drawPile        = new LinkedList<>();
        this.currentMarket   = new PriorityQueue<>();
        this.futureMarket    = new PriorityQueue<>();
        this.discardedPlants = new PriorityQueue<>();
        this.stepThreeActive = false;
        this.step3Card = new Powerplant(STEP3_CARD_NUMBER, new ArrayList<>(), 0, 0);
    }

    // --- Getters ---

    public LinkedList<Powerplant> getDrawPile()           { return drawPile; }
    public PriorityQueue<Powerplant> getCurrentMarket()   { return currentMarket; }
    public PriorityQueue<Powerplant> getFutureMarket()    { return futureMarket; }
    public PriorityQueue<Powerplant> getDiscardedPlants() { return discardedPlants; }
    public boolean isStepThreeActive()                    { return stepThreeActive; }

    /**
     * Returns true if the Step 3 card was encountered by drawSafe() during the
     * most recent rebalanceMarkets() call, then clears the flag so subsequent
     * calls return false until the card is hit again.
     *
     * RoundManager.resolveAuction() polls this immediately after calling
     * removeFromCurrentMarket() so it can set step3CardPending at the right time.
     */
    public boolean wasStep3CardEncountered() {
        boolean result = step3CardEncountered;
        step3CardEncountered = false;
        return result;
    }
    public int getCurrentMarketSize()                     { return currentMarket.size(); }
    public int getFutureMarketSize()                      { return futureMarket.size(); }

    // =========================================================================
    // SETUP
    // =========================================================================

    /**
     * Executes the complete Power Grid deck setup per the rulebook:
     *
     * 1. Shuffle all 03-15 cards. Randomly pick 8 and sort them ascending.
     *    Lowest 4 go to currentMarket, upper 4 to futureMarket.
     * 2. From the remaining 03-15 cards, randomly pick ONE and place it on
     *    top of the draw pile. Discard PLUG_REMOVAL_COUNT[playerCount] more
     *    face-up. Any extras go just below the top plug card on the draw pile.
     * 3. Discard SOCKET_REMOVAL_COUNT[playerCount] random 16+ cards face-up.
     * 4. Shuffle remaining 16+ cards into the draw pile.
     * 5. Place the Step 3 card at the very bottom.
     * 6. Place extra plug cards, then the single chosen plug card, on top.
     */
    public void setup(int playerCount) {
        currentMarket.clear();
        futureMarket.clear();
        drawPile.clear();
        discardedPlants.clear();
        stepThreeActive = false;

        List<Powerplant> startingCards = PowerplantData.getStartingCards(); // 03-15
        List<Powerplant> drawPileCards = PowerplantData.getDrawPileCards(); // 16+

        // 1. Pick 8 random 03-15 cards, sort, split 4|4 into markets.
        Collections.shuffle(startingCards);
        List<Powerplant> initialEight     = new ArrayList<>();
        List<Powerplant> leftoverStarting = new ArrayList<>();
        for (int i = 0; i < startingCards.size(); i++) {
            if (i < 8) initialEight.add(startingCards.get(i));
            else        leftoverStarting.add(startingCards.get(i));
        }
        Collections.sort(initialEight);
        for (int i = 0; i < initialEight.size(); i++) {
            if (i < 4) currentMarket.add(initialEight.get(i));
            else       futureMarket.add(initialEight.get(i));
        }

        // 2. Pick 1 leftover plug card for the top of the deck; show the rest that are
        //    removed per player count in discardedPlants so the teacher can verify setup.
        Collections.shuffle(leftoverStarting);
        Powerplant topOfDeck = leftoverStarting.isEmpty() ? null : leftoverStarting.remove(0);
        int plugRemoveCount = PLUG_REMOVAL_COUNT.getOrDefault(playerCount, 0);
        for (int i = 0; i < plugRemoveCount && !leftoverStarting.isEmpty(); i++) {
            discardedPlants.add(leftoverStarting.remove(0));
        }
        // leftoverStarting now contains any extra plug cards that belong in the draw pile.

        // 3. Show the appropriate number of removed 16+ cards in discardedPlants.
        int socketRemoveCount = SOCKET_REMOVAL_COUNT.getOrDefault(playerCount, 0);
        Collections.shuffle(drawPileCards);
        for (int i = 0; i < socketRemoveCount && !drawPileCards.isEmpty(); i++) {
            discardedPlants.add(drawPileCards.remove(drawPileCards.size() - 1));
        }

        // 4. Shuffle ALL remaining cards (leftover plugs + remaining sockets) together
        //    into one pile. The rulebook says "shuffle all remaining power plant cards
        //    together" — they must not be layered separately with plugs always on top.
        List<Powerplant> combinedPile = new ArrayList<>();
        combinedPile.addAll(leftoverStarting); // remaining plug cards
        combinedPile.addAll(drawPileCards);    // remaining socket cards
        Collections.shuffle(combinedPile);
        drawPile.addAll(combinedPile);

        // 5. Step 3 card at the very bottom.
        drawPile.addLast(step3Card);

        // 6. The single set-aside plug card goes on top of the entire stack.
        if (topOfDeck != null) {
            drawPile.addFirst(topOfDeck);
        }
    }

    // =========================================================================
    // CORE DECK OPERATIONS
    // =========================================================================

    public void shuffle() {
        Powerplant bottom = drawPile.removeLast(); // keep Step 3 at bottom
        Collections.shuffle(drawPile);
        drawPile.addLast(bottom);
    }

    public void addToDrawPile(Powerplant p) {
        drawPile.add(p);
    }

    /**
     * Draws the top card of the draw pile.
     * Caller MUST check {@link #isStep3Card} on the result before using it.
     */
    public Powerplant draw() {
        if (drawPile.isEmpty()) return null;
        return drawPile.removeFirst();
    }

    public boolean isStep3Card(Powerplant p) {
        return p != null && p.getNumber() == STEP3_CARD_NUMBER;
    }

    // =========================================================================
    // MARKET MANAGEMENT
    //
    // Core invariant (Steps 1 & 2): every card in currentMarket has a lower
    // number than every card in futureMarket. Both queues target 4 cards.
    //
    // All methods that change either market funnel through rebalanceMarkets(),
    // which collects the full 8-card pool, sorts it, and re-splits it 4|4.
    // This makes it structurally impossible for the markets to go out of order.
    // =========================================================================

    /**
     * Restores the 4|4 current|future split after any change to either market.
     *
     * Algorithm:
     *   1. Collect all cards currently in both markets into one list.
     *   2. Draw from the pile (via drawSafe) until the pool has 8 cards or the
     *      pile is genuinely empty.
     *   3. Sort ascending and assign: indices 0-3 → currentMarket,
     *      indices 4-7 → futureMarket.
     *
     * Because we always sort the full pool before splitting, it is impossible
     * for a future-market card to have a lower number than a current-market card.
     *
     * In Step 3 the logic is simpler: fill currentMarket to 6 cards.
     */
    private void rebalanceMarkets() {
        step3CardEncountered = false; // reset before each rebalance pass
        if (stepThreeActive) {
            while (currentMarket.size() < 6) {
                Powerplant drawn = drawSafe();
                if (drawn == null) break;
                currentMarket.add(drawn);
            }
            return;
        }

        // Collect the existing pool from both markets.
        List<Powerplant> pool = new ArrayList<>();
        pool.addAll(currentMarket);
        pool.addAll(futureMarket);
        currentMarket.clear();
        futureMarket.clear();

        // Fill pool to 8 from the draw pile (drawSafe skips the Step 3 card).
        while (pool.size() < 8) {
            Powerplant drawn = drawSafe();
            if (drawn == null) break;
            pool.add(drawn);
        }

        // Sort and split: lowest 4 → current, upper 4 → future.
        Collections.sort(pool);
        for (int i = 0; i < pool.size(); i++) {
            if (i < 4) currentMarket.add(pool.get(i));
            else       futureMarket.add(pool.get(i));
        }
    }

    /**
     * Draws one non-Step-3 card from the top of the draw pile.
     *
     * If the Step 3 card is encountered it is immediately re-queued at the
     * bottom of the pile and null is returned, signalling "no more cards to
     * draw into the market right now."  This is the ONLY place in the class
     * that reads from the draw pile for market-filling purposes, which ensures
     * consistent Step 3 card handling everywhere.
     *
     * Returns null when the pile is empty or only the Step 3 card remains.
     */
    private Powerplant drawSafe() {
        if (drawPile.isEmpty()) return null;

        // If the Step 3 card is the only thing left, signal its presence and stop.
        // Previously this branch returned null silently, so step3CardEncountered was
        // NEVER set — the game ran out of cards with Step 3 stuck at the bottom forever.
        if (drawPile.size() == 1 && isStep3Card(drawPile.peek())) {
            step3CardEncountered = true;
            return null;
        }

        Powerplant card = drawPile.removeFirst();
        if (isStep3Card(card)) {
            // Step 3 card surfaced mid-pile (e.g. after shuffling). Put it back at the
            // bottom, signal RoundManager, and stop drawing for this rebalance pass.
            drawPile.addLast(card);
            step3CardEncountered = true;
            return null;
        }
        return card;
    }

    /**
     * Call this after any card is removed from or added to either market.
     * Restores the sorted 4|4 split and draws replacements from the pile.
     */
    public void updateMarket() {
        rebalanceMarkets();
    }

    /**
     * Activates Step 3: removes the Step 3 card permanently from the draw pile,
     * merges futureMarket into currentMarket, then fills to 6 cards.
     *
     * The Step 3 card must be removed here — if left in the pile, drawSafe() would
     * set step3CardEncountered = true on every subsequent rebalance call, causing
     * repeated spurious Step 3 activations for the rest of the game.
     */
    public void activateStepThree() {
        stepThreeActive = true;
        drawPile.removeIf(this::isStep3Card); // permanently out of the game
        while (!futureMarket.isEmpty()) {
            currentMarket.add(futureMarket.poll());
        }
        rebalanceMarkets(); // fill to 6 in Step 3 branch
    }

    // =========================================================================
    // BUREAUCRACY / PHASE OPERATIONS
    // =========================================================================

    /**
     * BUREAUCRACY Steps 1 & 2.
     * Takes the highest card from futureMarket, lets rebalanceMarkets() draw a
     * replacement from the pile to fill that slot, then buries the removed card
     * just above the Step 3 card so it re-enters the market later.
     *
     * FIX: the previous version buried the card BEFORE calling updateMarket(),
     * which immediately drew it right back out, making the burial pointless.
     * We now rebalance first so the pile slot is consumed, then bury afterward.
     */
    public void discardHighestPlant() {
        if (futureMarket.isEmpty()) return;

        Powerplant highest = getHighestFrom(futureMarket);
        futureMarket.remove(highest);

        // Rebalance first: draw a real replacement from the pile.
        rebalanceMarkets();

        // Now bury the removed card just above the Step 3 card.
        int step3Index = findStep3Index();
        if (step3Index >= 0) drawPile.add(step3Index, highest);
        else                 drawPile.addLast(highest);
    }

    /**
     * BUREAUCRACY Step 3 transition / Step 2 activation.
     * Permanently removes the lowest card from currentMarket (discarded face-up),
     * then rebalances.
     *
     * FIX: previously called drawPile.removeFirst() directly, which could consume
     * the Step 3 card without activating Step 3. Now all drawing goes through
     * drawSafe() inside rebalanceMarkets().
     */
    public void removeLowestPlant() {
        if (currentMarket.isEmpty()) return;
        Powerplant lowest = currentMarket.poll(); // PriorityQueue yields minimum first
        discardedPlants.add(lowest);
        rebalanceMarkets();
    }

    public boolean removeFromCurrentMarket(Powerplant p) {
        boolean removed = currentMarket.remove(p);
        if (removed) rebalanceMarkets();
        return removed;
    }

    /**
     * Removes all currentMarket cards whose number <= leadingCityCount.
     *
     * FIX: previously removed cards one-at-a-time and called removeFirst()
     * directly, bypassing Step 3 detection and causing one draw per removal.
     * Now all removals happen first, then one rebalance refills all the gaps,
     * with drawSafe() handling Step 3 correctly throughout.
     */
    public void removeObsoletePlants(int leadingCityCount) {
        List<Powerplant> toRemove = new ArrayList<>();
        for (Powerplant p : currentMarket) {
            if (p.getNumber() <= leadingCityCount) toRemove.add(p);
        }
        if (toRemove.isEmpty()) return;
        for (Powerplant p : toRemove) {
            currentMarket.remove(p);
            discardedPlants.add(p);
        }
        rebalanceMarkets();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Powerplant getHighestFrom(PriorityQueue<Powerplant> pq) {
        Powerplant highest = null;
        for (Powerplant p : pq) {
            if (highest == null || p.getNumber() > highest.getNumber()) highest = p;
        }
        return highest;
    }

    private int findStep3Index() {
        for (int i = 0; i < drawPile.size(); i++) {
            if (isStep3Card(drawPile.get(i))) return i;
        }
        return -1;
    }

    // =========================================================================
    // DEBUG
    // =========================================================================

    public void printState() {
        List<Powerplant> cm = new ArrayList<>(currentMarket);
        List<Powerplant> fm = new ArrayList<>(futureMarket);
        Collections.sort(cm);
        Collections.sort(fm);
        System.out.print("Current market: ");
        for (Powerplant p : cm) System.out.print("#" + p.getNumber() + " ");
        System.out.println();
        System.out.print("Future market:  ");
        for (Powerplant p : fm) System.out.print("#" + p.getNumber() + " ");
        System.out.println();
        System.out.println("Draw pile size: " + drawPile.size());
        System.out.println("Step 3 active:  " + stepThreeActive);

        // Invariant assertion — will log loudly if the sort/split ever breaks.
        if (!cm.isEmpty() && !fm.isEmpty()) {
            int maxCurrent = cm.get(cm.size() - 1).getNumber();
            int minFuture  = fm.get(0).getNumber();
            if (maxCurrent > minFuture) {
                System.err.println("INVARIANT VIOLATION: currentMarket max (#"
                        + maxCurrent + ") > futureMarket min (#" + minFuture + ")");
            }
        }
    }
}