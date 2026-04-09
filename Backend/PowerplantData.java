package Backend;

import java.util.*;

/**
 * Static factory that creates every powerplant card in the standard Power Grid deck.
 *
 * Each card has:
 *   number          — printed on card; minimum bid and sort key
 *   acceptedResources — what it burns (empty = ecological)
 *   resourceIntake  — how many tokens it consumes per firing
 *   houseOutput     — how many cities it can power
 *
 * Cards 03–15 are the "plug-back" starting cards (sorted face-up at setup).
 * Cards 16–50 are shuffled into the draw pile.
 * Card 13 is special: removed based on player count during setup.
 * The Step 3 card is created separately in PowerplantDeck.
 */
public class PowerplantData {

    /**
     * Returns the complete list of all 42 powerplant cards.
     */
    public static List<Powerplant> getAllCards() {
        List<Powerplant> deck = new ArrayList<>();

        // COAL
        deck.add(new Powerplant(4,  coal(2), 2, 1));
        deck.add(new Powerplant(8,  coal(3), 3, 2));
        deck.add(new Powerplant(10, coal(2), 2, 2)); // efficient coal
        deck.add(new Powerplant(15, coal(2), 2, 3));
        deck.add(new Powerplant(20, coal(3), 3, 5));
        deck.add(new Powerplant(25, coal(2), 2, 5));
        deck.add(new Powerplant(31, coal(3), 3, 6));
        deck.add(new Powerplant(36, coal(3), 3, 7));
        deck.add(new Powerplant(42, coal(2), 2, 6));

        // OIL
        deck.add(new Powerplant(3,  oil(2),  2, 1));
        deck.add(new Powerplant(7,  oil(3),  3, 2));
        deck.add(new Powerplant(9,  oil(1),  1, 1));
        deck.add(new Powerplant(16, oil(2),  2, 3));
        deck.add(new Powerplant(26, oil(2),  2, 5));
        deck.add(new Powerplant(32, oil(3),  3, 6));
        deck.add(new Powerplant(35, oil(1),  1, 5));
        deck.add(new Powerplant(40, oil(2),  2, 6));

        // TRASH
        deck.add(new Powerplant(6,  trash(1), 1, 1));
        deck.add(new Powerplant(14, trash(2), 2, 2));
        deck.add(new Powerplant(19, trash(2), 2, 3));
        deck.add(new Powerplant(24, trash(2), 2, 4));
        deck.add(new Powerplant(30, trash(3), 3, 6));
        deck.add(new Powerplant(38, trash(3), 3, 7));

        // URANIUM
        deck.add(new Powerplant(11, uranium(1), 1, 2));
        deck.add(new Powerplant(17, uranium(1), 1, 2));
        deck.add(new Powerplant(23, uranium(1), 1, 3));
        deck.add(new Powerplant(28, uranium(1), 1, 4));
        deck.add(new Powerplant(34, uranium(1), 1, 5));
        deck.add(new Powerplant(39, uranium(1), 1, 6)); // buying this stops uranium restock

        // HYBRID (COAL + OIL)
        deck.add(new Powerplant(5,  hybrid(), 2, 1));
        deck.add(new Powerplant(12, hybrid(), 2, 2));
        deck.add(new Powerplant(21, hybrid(), 2, 4));
        deck.add(new Powerplant(29, hybrid(), 1, 4));
        deck.add(new Powerplant(46, hybrid(), 3, 7));

        // ECOLOGICAL (FREE)
        deck.add(new Powerplant(13, eco(), 0, 1)); // removed based on player count at setup
        deck.add(new Powerplant(18, eco(), 0, 2));
        deck.add(new Powerplant(22, eco(), 0, 2));
        deck.add(new Powerplant(27, eco(), 0, 3));
        deck.add(new Powerplant(33, eco(), 0, 4));
        deck.add(new Powerplant(37, eco(), 0, 4));
        deck.add(new Powerplant(44, eco(), 0, 5));
        deck.add(new Powerplant(50, eco(), 0, 6));

        return deck;
    }

    /**
     * Returns only the cards numbered 03–15 (the plug-back starting cards).
     */
    public static List<Powerplant> getStartingCards() {
        List<Powerplant> all = getAllCards();
        List<Powerplant> starting = new ArrayList<>();
        for (Powerplant p : all) {
            if (p.getNumber() >= 3 && p.getNumber() <= 15) {
                starting.add(p);
            }
        }
        return starting;
    }

    /**
     * Returns only the cards numbered 16+ (the main shuffled draw pile cards).
     */
    public static List<Powerplant> getDrawPileCards() {
        List<Powerplant> all = getAllCards();
        List<Powerplant> draw = new ArrayList<>();
        for (Powerplant p : all) {
            if (p.getNumber() > 15) {
                draw.add(p);
            }
        }
        return draw;
    }

    /**
     * Returns card #13 (the ecological card removed based on player count).
     */
    public static Powerplant getCard13() {
        for (Powerplant p : getAllCards()) {
            if (p.getNumber() == 13) return p;
        }
        return null;
    }

    // ── Resource list helpers ─────────────────────────────────

    private static List<ResourceType> coal(int count) {
        List<ResourceType> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(ResourceType.COAL);
        return list;
    }

    private static List<ResourceType> oil(int count) {
        List<ResourceType> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(ResourceType.OIL);
        return list;
    }

    private static List<ResourceType> trash(int count) {
        List<ResourceType> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(ResourceType.TRASH);
        return list;
    }

    private static List<ResourceType> uranium(int count) {
        List<ResourceType> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(ResourceType.URANIUM);
        return list;
    }

    private static List<ResourceType> hybrid() {
        // hybrid accepts COAL and OIL (one of each type in the list)
        List<ResourceType> list = new ArrayList<>();
        list.add(ResourceType.COAL);
        list.add(ResourceType.OIL);
        return list;
    }

    private static List<ResourceType> eco() {
        return new ArrayList<>(); // empty = ecological
    }
}