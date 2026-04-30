package Backend;

import java.util.*;
// Note: Iterator import no longer needed after removing card #13 special-case logic

public class PowerplantDeck {
    private LinkedList<Powerplant> drawPile;
    private PriorityQueue<Powerplant> currentMarket;
    private PriorityQueue<Powerplant> futureMarket;
    private PriorityQueue<Powerplant> discardedPlants;
    private boolean stepThreeActive;

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
        this.drawPile = new LinkedList<>();
        this.currentMarket = new PriorityQueue<>();
        this.futureMarket = new PriorityQueue<>();
        this.discardedPlants = new PriorityQueue<>();
        this.stepThreeActive = false;
        this.step3Card = new Powerplant(STEP3_CARD_NUMBER, new ArrayList<>(), 0, 0);
    }

    // --- Getters ---

    public LinkedList<Powerplant> getDrawPile()            { return drawPile; }
    public PriorityQueue<Powerplant> getCurrentMarket()    { return currentMarket; }
    public PriorityQueue<Powerplant> getFutureMarket()     { return futureMarket; }
    public PriorityQueue<Powerplant> getDiscardedPlants()  { return discardedPlants; }
    public boolean isStepThreeActive()                     { return stepThreeActive; }
    public int getCurrentMarketSize()                      { return currentMarket.size(); }
    public int getFutureMarketSize()                       { return futureMarket.size(); }

    // --- FULL SETUP ---

    /**
     * Executes the complete Power Grid deck setup per the rulebook:
     *
     * 1. Randomly draw 8 cards from the full 03-15 set.
     *    Sort ascending: lowest 4 -> currentMarket, upper 4 -> futureMarket.
     * 2. Randomly pick 1 of the remaining 03-15 cards for the top of the draw pile.
     *    Show PLUG_REMOVAL_COUNT[playerCount] of the rest as discarded (face-up).
     *    Any remaining leftover plug cards also go on top of the draw pile — nothing is silently dropped.
     * 3. Remove SOCKET_REMOVAL_COUNT[playerCount] cards from the 16+ pile and show as discarded.
     * 4. Shuffle the remaining 16+ cards and build the draw pile.
     * 5. Place the Step 3 card at the very bottom of the draw pile.
     * 6. Place any extra plug cards, then the chosen plug card, on top of the draw pile.
     */
    public void setup(int playerCount) {
        currentMarket.clear();
        futureMarket.clear();
        drawPile.clear();
        discardedPlants.clear();
        stepThreeActive = false;

        List<Powerplant> startingCards = PowerplantData.getStartingCards(); // 03-15
        List<Powerplant> drawPileCards = PowerplantData.getDrawPileCards(); // 16+

        // Randomly draw 8 from 03-15, sort, split into markets
        Collections.shuffle(startingCards);
        List<Powerplant> initialEight = new ArrayList<>();
        List<Powerplant> leftoverStarting = new ArrayList<>();
        for (int i = 0; i < startingCards.size(); i++) {
            if (i < 8) initialEight.add(startingCards.get(i));
            else       leftoverStarting.add(startingCards.get(i));
        }

        Collections.sort(initialEight);
        for (int i = 0; i < initialEight.size(); i++) {
            if (i < 4) currentMarket.add(initialEight.get(i));
            else       futureMarket.add(initialEight.get(i));
        }

        // Randomly pick 1 leftover 03-15 card for the top of the draw pile.
        // Then discard the rulebook-specified number of plug cards (shown face-up).
        // Any remaining leftover plug cards also go on top of the draw pile — nothing is silently dropped.
        Collections.shuffle(leftoverStarting);
        Powerplant topOfDeck = leftoverStarting.isEmpty() ? null : leftoverStarting.remove(0);
        int plugRemoveCount = PLUG_REMOVAL_COUNT.getOrDefault(playerCount, 0);
        for (int i = 0; i < plugRemoveCount && !leftoverStarting.isEmpty(); i++) {
            discardedPlants.add(leftoverStarting.remove(0));
        }
        // All remaining leftover plug cards go on top of the draw pile (will be placed after step3 card is added).

        // Remove socket (16+) cards based on player count; always track them as discarded.
        int socketRemoveCount = SOCKET_REMOVAL_COUNT.getOrDefault(playerCount, 0);
        Collections.shuffle(drawPileCards);
        for (int i = 0; i < socketRemoveCount && !drawPileCards.isEmpty(); i++) {
            discardedPlants.add(drawPileCards.remove(drawPileCards.size() - 1));
        }

        // Shuffle remaining 16+ cards into the draw pile
        Collections.shuffle(drawPileCards);
        drawPile.addAll(drawPileCards);

        // Step 3 card at the very bottom
        drawPile.addLast(step3Card);

        // Any leftover plug cards that weren't discarded also go on top of the draw pile
        for (Powerplant p : leftoverStarting) {
            drawPile.addFirst(p);
        }

        // The single randomly chosen 03-15 card goes on top of everything
        if (topOfDeck != null) {
            drawPile.addFirst(topOfDeck);
        }
    }

    // --- Core deck operations ---

    public void shuffle() {
        Powerplant bottom = drawPile.removeLast();
        Collections.shuffle(drawPile);
        drawPile.addLast(bottom);
    }

    public void addToDrawPile(Powerplant p) {
        drawPile.add(p);
    }

    /**
     * Draws the top card. Caller must check isStep3Card() on the result.
     */
    public Powerplant draw() {
        if (drawPile.isEmpty()) return null;
        return drawPile.removeFirst();
    }

    public boolean isStep3Card(Powerplant p) {
        return p != null && p.getNumber() == STEP3_CARD_NUMBER;
    }

    /**
     * Activates Step 3: merges futureMarket into currentMarket (target 6 cards).
     */
    public void activateStepThree() {
        stepThreeActive = true;
        while (!futureMarket.isEmpty()) {
            currentMarket.add(futureMarket.poll());
        }
        while (currentMarket.size() < 6 && !drawPile.isEmpty()) {
            Powerplant drawn = drawPile.removeFirst();
            if (!isStep3Card(drawn)) {
                currentMarket.add(drawn);
            }
        }
    }

    /**
     * After any card leaves the market, slides future -> current and draws a replacement.
     */
    public void updateMarket() {
        if (stepThreeActive) {
            while (currentMarket.size() < 6 && !drawPile.isEmpty()) {
                Powerplant drawn = draw();
                if (drawn != null && !isStep3Card(drawn)) {
                    currentMarket.add(drawn);
                } else if (drawn != null) {
                    drawPile.addLast(drawn);
                }
            }
        } else {
            if (!futureMarket.isEmpty() && currentMarket.size() < 4) {
                currentMarket.add(futureMarket.poll());
            }
            while (futureMarket.size() < 4 && !drawPile.isEmpty()) {
                Powerplant drawn = draw();
                if (drawn == null) break;
                if (isStep3Card(drawn)) {
                    drawPile.addLast(drawn);
                    break;
                }
                futureMarket.add(drawn);
            }
        }
    }

    /**
     * BUREAUCRACY Steps 1 & 2: buries highest future market card above Step 3 card.
     */
    public void discardHighestPlant() {
        if (futureMarket.isEmpty()) return;
        Powerplant highest = getHighestFrom(futureMarket);
        futureMarket.remove(highest);
        int step3Index = findStep3Index();
        if (step3Index >= 0) drawPile.add(step3Index, highest);
        else                 drawPile.addLast(highest);
        updateMarket();
    }

    /**
     * BUREAUCRACY Step 3 / Step 2 transition: removes lowest current market card permanently.
     */
    public void removeLowestPlant() {
        if (currentMarket.isEmpty()) return;
        Powerplant lowest = currentMarket.poll();
        discardedPlants.add(lowest);
        if (!drawPile.isEmpty()) {
            Powerplant drawn = draw();
            if (drawn != null && !isStep3Card(drawn)) {
                currentMarket.add(drawn);
            } else if (drawn != null) {
                drawPile.addLast(drawn);
            }
        }
    }

    public boolean removeFromCurrentMarket(Powerplant p) {
        return currentMarket.remove(p);
    }

    /**
     * Removes any plants in currentMarket whose number <= leadingCityCount.
     */
    public void removeObsoletePlants(int leadingCityCount) {
        List<Powerplant> toRemove = new ArrayList<>();
        for (Powerplant p : currentMarket) {
            if (p.getNumber() <= leadingCityCount) toRemove.add(p);
        }
        for (Powerplant p : toRemove) {
            currentMarket.remove(p);
            discardedPlants.add(p);
            if (!drawPile.isEmpty()) {
                Powerplant drawn = drawPile.removeFirst();
                if (!isStep3Card(drawn)) currentMarket.add(drawn);
            }
        }
    }

    // --- Private helpers ---

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

    // --- Debug ---

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
    }
}