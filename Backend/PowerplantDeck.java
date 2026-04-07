package Backend;

import java.util.*;

public class PowerplantDeck {
    private LinkedList<Powerplant> drawPile;
    private PriorityQueue<Powerplant> currentMarket;
    private PriorityQueue<Powerplant> futureMarket;
    private PriorityQueue<Powerplant> discardedPlants;
    private boolean stepThreeActive;

    // The special Step 3 sentinel card (number = -1 means it's the step trigger)
    private static final int STEP3_CARD_NUMBER = -1;
    private Powerplant step3Card;

    public PowerplantDeck() {
        this.drawPile = new LinkedList<>();
        this.currentMarket = new PriorityQueue<>();
        this.futureMarket = new PriorityQueue<>();
        this.discardedPlants = new PriorityQueue<>();
        this.stepThreeActive = false;
        // create the step 3 sentinel card (no resources, no output — it's just a trigger)
        this.step3Card = new Powerplant(STEP3_CARD_NUMBER, new ArrayList<>(), 0, 0);
    }

    // --- Getters ---

    public LinkedList<Powerplant> getDrawPile() { return drawPile; }

    public PriorityQueue<Powerplant> getCurrentMarket() { return currentMarket; }

    public PriorityQueue<Powerplant> getFutureMarket() { return futureMarket; }

    public PriorityQueue<Powerplant> getDiscardedPlants() { return discardedPlants; }

    public boolean isStepThreeActive() { return stepThreeActive; }

    public int getCurrentMarketSize() { return currentMarket.size(); }

    public int getFutureMarketSize() { return futureMarket.size(); }

    // --- Setup ---

    /**
     * Shuffles the draw pile randomly.
     */
    public void shuffle() {
        Collections.shuffle(drawPile);
    }

    /**
     * Adds a powerplant directly to the draw pile (used during setup).
     */
    public void addToDrawPile(Powerplant p) {
        drawPile.add(p);
    }

    /**
     * Places the Step 3 card at the very bottom of the draw pile.
     * Called at end of setup, before placing the set-aside plug-back card on top.
     */
    public void placeStep3CardAtBottom() {
        drawPile.addLast(step3Card);
    }

    /**
     * Places a card at the very top of the draw pile.
     * Used to place the set-aside plug-back card on top after Step 3 card is at bottom.
     */
    public void placeOnTop(Powerplant p) {
        drawPile.addFirst(p);
    }

    /**
     * Sorts a list of powerplants ascending and places lowest 4 in currentMarket,
     * upper 4 in futureMarket. Called during setup after drawing initial 8.
     */
    public void setupInitialMarket(List<Powerplant> initialEight) {
        List<Powerplant> sorted = new ArrayList<>(initialEight);
        Collections.sort(sorted);
        currentMarket.clear();
        futureMarket.clear();
        for (int i = 0; i < 4; i++) currentMarket.add(sorted.get(i));
        for (int i = 4; i < 8; i++) futureMarket.add(sorted.get(i));
    }

    // --- Core deck operations ---

    /**
     * Draws the top card from the draw pile.
     * Returns null if the pile is empty.
     * Does NOT handle Step 3 trigger — caller (RoundManager) checks the return value.
     */
    public Powerplant draw() {
        if (drawPile.isEmpty()) return null;
        return drawPile.removeFirst();
    }

    /**
     * Returns true if the given powerplant is the Step 3 trigger card.
     */
    public boolean isStep3Card(Powerplant p) {
        return p != null && p.getNumber() == STEP3_CARD_NUMBER;
    }

    /**
     * Activates Step 3 mode:
     * - Merges futureMarket into currentMarket
     * - currentMarket now holds 6 cards total
     * - futureMarket is cleared
     * - stepThreeActive = true
     * - The Step 3 card itself is removed from the game
     */
    public void activateStepThree() {
        stepThreeActive = true;
        // move all future market cards into current market
        while (!futureMarket.isEmpty()) {
            currentMarket.add(futureMarket.poll());
        }
        // draw 2 more to reach 6 if possible
        while (currentMarket.size() < 6 && !drawPile.isEmpty()) {
            Powerplant drawn = drawPile.removeFirst();
            if (!isStep3Card(drawn)) {
                currentMarket.add(drawn);
            }
        }
    }

    /**
     * Called after any card leaves the market (bought or removed).
     * Slides the lowest futureMarket card into currentMarket,
     * then draws a replacement from the draw pile into futureMarket.
     * In Step 3, there is no futureMarket — just draw directly into currentMarket.
     */
    public void updateMarket() {
        if (stepThreeActive) {
            // Step 3: currentMarket targets 6 cards, no future market
            while (currentMarket.size() < 6 && !drawPile.isEmpty()) {
                Powerplant drawn = draw();
                if (drawn != null && !isStep3Card(drawn)) {
                    currentMarket.add(drawn);
                }
            }
        } else {
            // Steps 1 & 2: keep currentMarket at 4, futureMarket at 4
            // slide lowest future card down into current
            if (!futureMarket.isEmpty() && currentMarket.size() < 4) {
                currentMarket.add(futureMarket.poll());
            }
            // draw replacement into future market
            while (futureMarket.size() < 4 && !drawPile.isEmpty()) {
                Powerplant drawn = draw();
                if (drawn != null) {
                    if (isStep3Card(drawn)) {
                        // Step 3 card drawn — caller should handle; put it back at bottom for now
                        drawPile.addLast(drawn);
                        break;
                    }
                    futureMarket.add(drawn);
                }
            }
        }
    }

    /**
     * BUREAUCRACY (Steps 1 & 2):
     * Takes the highest card in futureMarket and places it face-down
     * at the bottom of the draw pile (above the Step 3 card).
     * Then draws a replacement and resorts.
     */
    public void discardHighestPlant() {
        if (futureMarket.isEmpty()) return;
        // PriorityQueue is min-heap; to get the max we need to iterate
        Powerplant highest = getHighestFrom(futureMarket);
        futureMarket.remove(highest);
        // insert above the Step 3 card (second from last position)
        int step3Index = findStep3Index();
        if (step3Index >= 0) {
            drawPile.add(step3Index, highest);
        } else {
            drawPile.addLast(highest);
        }
        // draw replacement
        updateMarket();
    }

    /**
     * BUREAUCRACY (Step 3) and Step 2 transition:
     * Removes the lowest card in currentMarket permanently (out of game).
     * Draws a replacement if the draw pile is not empty.
     */
    public void removeLowestPlant() {
        if (currentMarket.isEmpty()) return;
        Powerplant lowest = currentMarket.poll(); // PriorityQueue poll() gives minimum
        discardedPlants.add(lowest);
        // draw replacement if possible
        if (!drawPile.isEmpty()) {
            Powerplant drawn = draw();
            if (drawn != null && !isStep3Card(drawn)) {
                currentMarket.add(drawn);
            } else if (drawn != null && isStep3Card(drawn)) {
                // step 3 card drawn unexpectedly here, put back
                drawPile.addLast(drawn);
            }
        }
    }

    /**
     * Removes a specific plant from the current market (e.g. when it's been bought).
     * Caller must then call updateMarket().
     */
    public boolean removeFromCurrentMarket(Powerplant p) {
        return currentMarket.remove(p);
    }

    /**
     * Rule: if no plant was sold in an entire round, remove the lowest from
     * currentMarket and replace it with a draw.
     */
    public void removeLowestIfNoSale() {
        removeLowestPlant();
    }

    /**
     * Rule: any plant in currentMarket with number <= leading player's city count
     * must be removed and replaced immediately.
     */
    public void removeObsoletePlants(int leadingCityCount) {
        List<Powerplant> toRemove = new ArrayList<>();
        for (Powerplant p : currentMarket) {
            if (p.getNumber() <= leadingCityCount) {
                toRemove.add(p);
            }
        }
        for (Powerplant p : toRemove) {
            currentMarket.remove(p);
            discardedPlants.add(p);
            // draw replacement
            if (!drawPile.isEmpty()) {
                Powerplant drawn = drawPile.removeFirst();
                if (!isStep3Card(drawn)) {
                    currentMarket.add(drawn);
                }
            }
        }
    }

    // --- Private helpers ---

    private Powerplant getHighestFrom(PriorityQueue<Powerplant> pq) {
        Powerplant highest = null;
        for (Powerplant p : pq) {
            if (highest == null || p.getNumber() > highest.getNumber()) {
                highest = p;
            }
        }
        return highest;
    }

    private int findStep3Index() {
        for (int i = 0; i < drawPile.size(); i++) {
            if (isStep3Card(drawPile.get(i))) return i;
        }
        return -1;
    }
}