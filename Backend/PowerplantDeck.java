package Backend;

import java.util.*;

public class PowerplantDeck {
    private LinkedList<Powerplant> drawPile;
    private PriorityQueue<Powerplant> currentMarket;
    private PriorityQueue<Powerplant> futureMarket;
    private PriorityQueue<Powerplant> discardedPlants;
    private boolean stepThreeActive;

    private static final int STEP3_CARD_NUMBER = -1;
    private Powerplant step3Card;

    // Number of cards to randomly remove from the 16+ pile per player count (rulebook)
    private static final Map<Integer, Integer> RANDOM_REMOVAL_COUNT = new HashMap<>();
    static {
        RANDOM_REMOVAL_COUNT.put(2, 8);
        RANDOM_REMOVAL_COUNT.put(3, 8);
        RANDOM_REMOVAL_COUNT.put(4, 4);
        RANDOM_REMOVAL_COUNT.put(5, 0);
        RANDOM_REMOVAL_COUNT.put(6, 0);
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
     * 1. Separate cards 03-15 from 16+.
     * 2. Remove card #13 (stays out for 2-5 players; kept in for 6 players).
     * 3. Shuffle cards 03-12, 14-15. Draw 8, sort ascending.
     *    Lowest 4 -> currentMarket, upper 4 -> futureMarket.
     *    Remaining plug-back card(s) set aside.
     * 4. Remove cards from 16+ pile randomly based on player count.
     * 5. Combine remaining 16+ cards + leftover plug-backs and shuffle.
     * 6. Place Step 3 card at bottom of drawPile.
     * 7. Place set-aside plug-back card on top of drawPile.
     */
    public void setup(int playerCount) {
        currentMarket.clear();
        futureMarket.clear();
        drawPile.clear();
        discardedPlants.clear();
        stepThreeActive = false;

        List<Powerplant> startingCards = PowerplantData.getStartingCards(); // 03-15
        List<Powerplant> drawPileCards = PowerplantData.getDrawPileCards(); // 16+

        // Remove card #13 (ecological, taken out unless 6 players)
        Powerplant card13 = null;
        Iterator<Powerplant> it = startingCards.iterator();
        while (it.hasNext()) {
            Powerplant p = it.next();
            if (p.getNumber() == 13) {
                card13 = p;
                it.remove();
                break;
            }
        }
        if (playerCount == 6 && card13 != null) {
            startingCards.add(card13); // 6-player keeps it
        }

        // Shuffle 03-15 (minus #13), draw 8
        Collections.shuffle(startingCards);
        List<Powerplant> initialEight = new ArrayList<>();
        List<Powerplant> leftoverStarting = new ArrayList<>();
        for (int i = 0; i < startingCards.size(); i++) {
            if (i < 8) initialEight.add(startingCards.get(i));
            else       leftoverStarting.add(startingCards.get(i));
        }

        // Sort and split into markets
        Collections.sort(initialEight);
        for (int i = 0; i < initialEight.size(); i++) {
            if (i < 4) currentMarket.add(initialEight.get(i));
            else       futureMarket.add(initialEight.get(i));
        }

        // Set aside one plug-back card (goes on top of draw pile later)
        Powerplant setAsidePlugBack = leftoverStarting.isEmpty() ? null : leftoverStarting.remove(0);

        // Remove cards from 16+ pile based on player count
        int removeCount = RANDOM_REMOVAL_COUNT.getOrDefault(playerCount, 0);
        Collections.shuffle(drawPileCards);
        for (int i = 0; i < removeCount && !drawPileCards.isEmpty(); i++) {
            discardedPlants.add(drawPileCards.remove(drawPileCards.size() - 1));
        }

        // Combine 16+ remainder with any extra plug-back leftovers, shuffle
        List<Powerplant> combined = new ArrayList<>();
        combined.addAll(drawPileCards);
        combined.addAll(leftoverStarting);
        Collections.shuffle(combined);
        drawPile.addAll(combined);

        // Step 3 card at the very bottom
        drawPile.addLast(step3Card);

        // Set-aside plug-back card on top
        if (setAsidePlugBack != null) {
            drawPile.addFirst(setAsidePlugBack);
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