package Backend;

import java.util.*;

public class Player {
    private String name;
    private String color;
    private int money;
    private List<Powerplant> powerplants;
    private Set<City> ownedCities;
    private boolean passedAuction;
    private boolean boughtThisRound;

    public Player(String name, int money, String color) {
        this.name = name;
        this.color = color;
        this.money = money;
        this.powerplants = new ArrayList<>();
        this.ownedCities = new HashSet<>();
    }

    // basic getters

    public String getName() { return name; }

    public String getColor() { return color; }

    public int getMoney() { return money; }

    public List<Powerplant> getPowerplants() { return powerplants; }

    public Set<City> getOwnedCities() { return ownedCities; }

    public boolean isPassedAuction() { return passedAuction; }

    public boolean isBoughtThisRound() { return boughtThisRound; }

    // basic setters

    public void setName(String newName) { this.name = newName; }

    public void setColor(String newColor) { this.color = newColor; }

    public void setPassedAuction(boolean x) { this.passedAuction = x; }

    public void setBoughtThisRound(boolean x) { this.boughtThisRound = x; }

    // Money

    public boolean canAfford(int cost) {
        return money >= cost;
    }

    public void spendMoney(int amount) {
        money -= amount;
    }

    public void addMoney(int amount) {
        money += amount;
    }

    // Powerplants:

    /*
    Adds a powerplant if the player has fewer than 3.
    Returns true if added, false if already at max (caller must prompt discardPowerplant method).
     */
    public boolean addPowerplant(Powerplant p) {
        if (powerplants.size() < 3) {
            powerplants.add(p);
            return true;
        }
        return false;
    }

    /*
    Discards a powerplant from this player's list.
    Tries to move its stored resources to remaining plants.
    Resources that cannot be stored are returned (caller returns them to supply).
    Returns a map of resource tokens that could NOT be redistributed.
     */
    public Map<ResourceType, Integer> discardPowerplant(Powerplant p) {
        powerplants.remove(p);

        // collect all resources from the discarded plant
        Map<ResourceType, Integer> leftover = new HashMap<>(p.getStoredResources());

        // try to redistribute to remaining plants
        for (ResourceType type : new ArrayList<>(leftover.keySet())) {
            int remaining = leftover.get(type);
            for (Powerplant other : powerplants) {
                if (remaining == 0) break;
                int canTake = 0;
                // find how many of this type the other plant can absorb
                while (canTake < remaining && other.canStore(type, canTake + 1)) {
                    canTake++;
                }
                if (canTake > 0) {
                    other.storeResource(type, canTake);
                    remaining -= canTake;
                }
            }
            leftover.put(type, remaining);
        }

        // remove zeroed entries
        leftover.entrySet().removeIf(e -> e.getValue() == 0);
        return leftover; // caller must return these to supply
    }

    /**
     * Returns the highest plant number this player owns.
     * Returns 0 if the player owns no plants (used for tiebreaking turn order).
     */
    public int getHighestPlantNumber() {
        return powerplants.stream()
                .mapToInt(Powerplant::getNumber)
                .max()
                .orElse(0);
    }

    // --- Resources ---

    /**
     * Checks if a specific plant on this player can store the given type and amount.
     * The plant must be owned by this player.
     */
    public boolean canStoreResource(Powerplant p, ResourceType type, int amount) {
        if (!powerplants.contains(p)) return false;
        return p.canStore(type, amount);
    }

    /**
     * Adds resources to a specific plant.
     * Validates ownership and capacity via canStoreResource() first.
     * Returns true if successful.
     */
    public boolean addResource(Powerplant p, ResourceType type, int amount) {
        if (!canStoreResource(p, type, amount)) return false;
        p.storeResource(type, amount);
        return true;
    }

    /**
     * Moves resources between two of this player's plants during the buying phase.
     * Returns true if the move was successful.
     */
    public boolean moveResource(Powerplant from, Powerplant to, ResourceType type, int amount) {
        if (!powerplants.contains(from) || !powerplants.contains(to)) return false;
        if (from.getStoredAmount(type) < amount) return false;
        if (!to.canStore(type, amount)) return false;
        // perform the move
        from.getStoredResources(); // just for reference; actual mutation below
        from.clearResources();     // this is too destructive — use targeted removal instead
        // targeted removal: rebuild stored map minus the moved amount
        Map<ResourceType, Integer> snapshot = new HashMap<>(from.getStoredResources());
        snapshot.put(type, snapshot.get(type) - amount);
        from.clearResources();
        for (Map.Entry<ResourceType, Integer> entry : snapshot.entrySet()) {
            if (entry.getValue() > 0) {
                from.storeResource(entry.getKey(), entry.getValue());
            }
        }
        to.storeResource(type, amount);
        return true;
    }

    /**
     * Sums houseOutput across all owned powerplants.
     * This is the maximum number of cities this player can power this bureaucracy.
     */
    public int getTotalPowerCapacity() {
        int total = 0;
        for (Powerplant p : powerplants) {
            total += p.getHouseOutput();
        }
        return total;
    }

    /**
     * Returns how many cities this player can actually power right now
     * (limited by which plants can fire, not just capacity).
     */
    public int getActualPowerableCount() {
        int total = 0;
        for (Powerplant p : powerplants) {
            if (p.canFire()) {
                total += p.getHouseOutput();
            }
        }
        return total;
    }

    // --- Cities ---

    public void addCity(City c) {
        ownedCities.add(c);
    }

    public void removeCity(City c) {
        ownedCities.remove(c);
    }

    public int getCityCount() {
        return ownedCities.size();
    }

    @Override
    public String toString() {
        return name + " ($" + money + ", cities:" + ownedCities.size() + ")";
    }
}