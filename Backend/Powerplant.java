package Backend;

import java.util.*;

public class Powerplant implements Comparable<Powerplant> {
    private int number;
    private List<ResourceType> acceptedResources;
    private int resourceIntake;
    private int houseOutput;
    private Map<ResourceType, Integer> storedResources;

    public Powerplant(int number, List<ResourceType> acceptedResources, int resourceIntake, int houseOutput) {
        this.number = number;
        this.acceptedResources = new ArrayList<>(acceptedResources);
        this.resourceIntake = resourceIntake;
        this.houseOutput = houseOutput;
        this.storedResources = new HashMap<>();
        // initialize storage to 0 for each accepted type
        for (ResourceType type : acceptedResources) {
            storedResources.put(type, 0);
        }
    }

    // --- Comparable: sort by number ascending (for PriorityQueues) ---

    @Override
    public int compareTo(Powerplant other) {
        return Integer.compare(this.number, other.number);
    }

    // --- Basic Getters ---

    public int getNumber() {
        return number;
    }

    public List<ResourceType> getAcceptedResources() {
        return Collections.unmodifiableList(acceptedResources);
    }

    public int getResourceIntake() {
        return resourceIntake;
    }

    public int getHouseOutput() {
        return houseOutput;
    }

    public Map<ResourceType, Integer> getStoredResources() {
        return Collections.unmodifiableMap(storedResources);
    }

    // --- Storage calculations ---

    /**
     * Returns the total number of resource tokens currently loaded on this plant.
     */
    public int getStoredTotal() {
        int total = 0;
        for (Integer amount : storedResources.values()) {
            total += amount;
        }
        return total;
    }

    /**
     * Capacity is always 2x the intake.
     * Ecological plants return 0 (they store nothing).
     */
    public int getStorageCapacity() {
        return resourceIntake * 2;
    }

    /**
     * Returns the remaining free storage space.
     */
    public int getRemainingStorage() {
        return getStorageCapacity() - getStoredTotal();
    }

    // --- Type checks ---

    /**
     * Hybrid: accepts two resource types (COAL + OIL).
     */
    public boolean isHybrid() {
        return acceptedResources.size() == 2;
    }

    /**
     * Ecological: no resources required (wind, nuclear-free, etc.)
     */
    public boolean isEcological() {
        return acceptedResources.isEmpty();
    }

    // --- Resource management ---

    /**
     * Checks if this plant can accept the given type and amount.
     * Type must be accepted AND there must be enough combined space.
     */
    public boolean canStore(ResourceType type, int amount) {
        if (!acceptedResources.contains(type)) return false;
        return getStoredTotal() + amount <= getStorageCapacity();
    }

    /**
     * Adds amount of type to this plant's storage.
     * Assumes canStore() was already checked.
     */
    public void storeResource(ResourceType type, int amount) {
        if (!acceptedResources.contains(type)) return;
        storedResources.put(type, storedResources.getOrDefault(type, 0) + amount);
    }

    /**
     * Fires the plant: decrements storedResources by resourceIntake total.
     * For hybrid plants, deducts from whichever resources are loaded (any combo).
     * Returns a map of how much of each type was consumed (for returning to supply).
     * Returns null if the plant cannot fire (not enough resources).
     */
    public Map<ResourceType, Integer> fire() {
        if (isEcological()) {
            return new HashMap<>(); // ecological plants always fire, consume nothing
        }
        if (getStoredTotal() < resourceIntake) {
            return null; // cannot fire
        }
        Map<ResourceType, Integer> consumed = new HashMap<>();
        int remaining = resourceIntake;
        // deduct greedily from stored resources
        for (ResourceType type : storedResources.keySet()) {
            int stored = storedResources.get(type);
            int take = Math.min(stored, remaining);
            if (take > 0) {
                storedResources.put(type, stored - take);
                consumed.put(type, take);
                remaining -= take;
            }
            if (remaining == 0) break;
        }
        return consumed;
    }

    /**
     * Returns true if this plant has enough resources to fire.
     */
    public boolean canFire() {
        return isEcological() || getStoredTotal() >= resourceIntake;
    }

    /**
     * Removes all resources from this plant and returns them as a map.
     * Used when discarding a plant — caller must return tokens to supply.
     */
    public Map<ResourceType, Integer> clearResources() {
        Map<ResourceType, Integer> cleared = new HashMap<>(storedResources);
        for (ResourceType type : storedResources.keySet()) {
            storedResources.put(type, 0);
        }
        return cleared;
    }

    /**
     * Returns how many tokens of a given type are stored on this plant.
     */
    public int getStoredAmount(ResourceType type) {
        return storedResources.getOrDefault(type, 0);
    }

    @Override
    public String toString() {
        return "Powerplant #" + number + " [output:" + houseOutput + ", intake:" + resourceIntake + "]";
    }
}