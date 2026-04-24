package Backend;

public class ResourceMarket {
    // Each slot array holds the current token count in that slot.
    // Slots are ordered cheapest first (index 0 = cheapest).
    // Coal/Oil/Trash: 8 slots, max 3 tokens each.
    // Uranium: 12 slots, max 1 token each.
    private int[] coalSlots;
    private int[] oilSlots;
    private int[] trashSlots;
    private int[] uraniumSlots;

    private static final int[] COAL_PRICES   = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] OIL_PRICES    = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] TRASH_PRICES  = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] URANIUM_PRICES = {1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16};

    private static final int COAL_MAX_PER_SLOT    = 3;
    private static final int OIL_MAX_PER_SLOT     = 3;
    private static final int TRASH_MAX_PER_SLOT   = 3;
    private static final int URANIUM_MAX_PER_SLOT = 1;

    private boolean uraniumResupplyStopped;

    // Replenish amounts from Power Grid rulebook table
    // [playerCount-2][step-1] — indexed as [0..4][0..2]
    // playerCount 2->index 0, 3->1, 4->2, 5->3, 6->4
    private static final int[][] COAL_REPLENISH = {
        {3, 4, 3},  // 2 players
        {4, 5, 3},  // 3 players
        {5, 6, 4},  // 4 players
        {5, 7, 5},  // 5 players
        {7, 9, 6}   // 6 players
    };
    private static final int[][] OIL_REPLENISH = {
        {2, 2, 4},
        {2, 3, 4},
        {3, 4, 5},
        {4, 5, 6},
        {5, 6, 7}
    };
    private static final int[][] TRASH_REPLENISH = {
        {1, 2, 3},
        {2, 3, 3},
        {3, 3, 4},
        {3, 5, 5},
        {3, 5, 6}
    };
    private static final int[][] URANIUM_REPLENISH = {
        {1, 1, 1},
        {1, 1, 1},
        {1, 2, 2},
        {2, 3, 2},
        {2, 3, 3}
    };

    public ResourceMarket() {
        coalSlots    = new int[8];
        oilSlots     = new int[8];
        trashSlots   = new int[8];
        uraniumSlots = new int[12];
        uraniumResupplyStopped = false;
    }

    // --- Getters ---

    public int[] getCoalSlots()    { return coalSlots; }
    public int[] getOilSlots()     { return oilSlots; }
    public int[] getTrashSlots()   { return trashSlots; }
    public int[] getUraniumSlots() { return uraniumSlots; }
    public boolean isUraniumResupplyStopped() { return uraniumResupplyStopped; }

    public void setUraniumResupplyStopped(boolean x) { uraniumResupplyStopped = x; }

    // --- Setup: initial fill ---

    /**
     * Fills the market with starting values per the Power Grid rulebook.
     * Standard starting position:
     *   Coal: slots 0-2 full (3 tokens each) = 24 coal on board
     *   Oil:  slots 3-5 full (3 each) = first 3 slots empty, next 3 full
     *   Trash: slots 5-7 full = 9 trash
     *   Uranium: slots 9-11 have 1 each = 3 uranium (cheapest slots empty)
     */
    public void initializeStartingResources() {
        // Coal: first 3 slots (cheapest) filled to max
        for (int i = 0; i < 8; i++) coalSlots[i] = COAL_MAX_PER_SLOT;
        // Oil: slots 3-5 filled
        for (int i = 2; i < 8; i++) oilSlots[i] = OIL_MAX_PER_SLOT;
        // Trash: slots 5-7 filled
        for (int i = 5; i < 8; i++) trashSlots[i] = TRASH_MAX_PER_SLOT;
        // Uranium: slots 9-11 filled (most expensive)
        for (int i = 10; i < 12; i++) uraniumSlots[i] = URANIUM_MAX_PER_SLOT;
    }

    // --- Cost calculation ---

    /**
     * Calculates the total cost to buy `amount` of the given resource type.
     * Purchases from the cheapest available slot first.
     * Returns -1 if there are not enough resources available.
     */
    public int calculateCost(ResourceType type, int amount) {
        int[] slots = getSlotsFor(type);
        int[] prices = getPricesFor(type);
        if (slots == null || prices == null) return -1;

        int needed = amount;
        int totalCost = 0;

        for (int i = 0; i < slots.length && needed > 0; i++) {
            int available = slots[i];
            int take = Math.min(available, needed);
            totalCost += take * prices[i];
            needed -= take;
        }

        if (needed > 0) return -1; // not enough supply
        return totalCost;
    }

    /**
     * Returns how many tokens of a given type are currently available.
     */
    public int getAvailableAmount(ResourceType type) {
        int[] slots = getSlotsFor(type);
        if (slots == null) return 0;
        int total = 0;
        for (int s : slots) total += s;
        return total;
    }

    // --- Buying ---

    /**
     * Executes a purchase: deducts tokens from cheapest slots first.
     * Returns the actual cost charged.
     * Returns -1 if not enough supply.
     */
    public int buyResource(ResourceType type, int amount) {
        int cost = calculateCost(type, amount);
        if (cost < 0) return -1;

        int[] slots = getSlotsFor(type);
        int needed = amount;

        for (int i = 0; i < slots.length && needed > 0; i++) {
            int take = Math.min(slots[i], needed);
            slots[i] -= take;
            needed -= take;
        }

        return cost;
    }

    // --- Restocking ---

    /**
     * Restocks the market at the end of bureaucracy.
     * Fills from the most expensive slot downward (fills expensive slots first).
     * Uses the replenish table for exact amounts.
     * Respects uraniumResupplyStopped.
     */
    public void restock(int playerCount, int gameStep) {
        int pIdx = playerCount - 2; // 2 players = index 0
        int sIdx = gameStep - 1;    // step 1 = index 0

        restock(coalSlots,    COAL_REPLENISH[pIdx][sIdx],    COAL_MAX_PER_SLOT);
        restock(oilSlots,     OIL_REPLENISH[pIdx][sIdx],     OIL_MAX_PER_SLOT);
        restock(trashSlots,   TRASH_REPLENISH[pIdx][sIdx],   TRASH_MAX_PER_SLOT);

        if (!uraniumResupplyStopped) {
            restock(uraniumSlots, URANIUM_REPLENISH[pIdx][sIdx], URANIUM_MAX_PER_SLOT);
        }
    }

    /**
     * Internal helper: adds `amount` tokens to a slot array, filling most-expensive-first.
     * Stops if supply runs out (amount exhausted).
     */
    private void restock(int[] slots, int amount, int maxPerSlot) {
        int remaining = amount;
        // fill from most expensive (last index) downward
        for (int i = slots.length - 1; i >= 0 && remaining > 0; i--) {
            int space = maxPerSlot - slots[i];
            int add = Math.min(space, remaining);
            slots[i] += add;
            remaining -= add;
        }
    }

    // --- Helpers ---

    private int[] getSlotsFor(ResourceType type) {
        switch (type) {
            case COAL:    return coalSlots;
            case OIL:     return oilSlots;
            case TRASH:   return trashSlots;
            case URANIUM: return uraniumSlots;
            default:      return null;
        }
    }

    private int[] getPricesFor(ResourceType type) {
        switch (type) {
            case COAL:    return COAL_PRICES;
            case OIL:     return OIL_PRICES;
            case TRASH:   return TRASH_PRICES;
            case URANIUM: return URANIUM_PRICES;
            default:      return null;
        }
    }

    /**
     * Debug: prints current market state.
     */
    public void printMarket() {
        System.out.println("=== Resource Market ===");
        printRow("Coal",    coalSlots,    COAL_PRICES);
        printRow("Oil",     oilSlots,     OIL_PRICES);
        printRow("Trash",   trashSlots,   TRASH_PRICES);
        printRow("Uranium", uraniumSlots, URANIUM_PRICES);
    }

    private void printRow(String label, int[] slots, int[] prices) {
        System.out.print(label + ": ");
        for (int i = 0; i < slots.length; i++) {
            System.out.print("[" + slots[i] + "@$" + prices[i] + "] ");
        }
        System.out.println();
    }
}