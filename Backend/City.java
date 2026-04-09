package Backend;

public class City {
    private String name;
    private int[] slotCosts = {10, 15, 20};
    private Player[] occupants;
    private boolean isMetropolis;
    private boolean passedAuction;

    public City(String name) {
        this.name = name;
        this.occupants = new Player[3];
    }

    public City(String name, boolean isMetropolis) {
        this.name = name;
        this.occupants = new Player[3];
        this.isMetropolis = isMetropolis;
    }

    // Getters
    public String getName() {
        return name;
    }

    public int[] getSlotCosts() {
        return slotCosts;
    }

    public Player[] getOccupants() {
        return occupants;
    }

    public boolean isMetropolis() {
        return isMetropolis;
    }

    public boolean isPassedAuction() {
        return passedAuction;
    }

    //setters
    public void setMetropolis(boolean x) {
        this.isMetropolis = x;
    }

    public void setPassedAuction(boolean x) {
        this.passedAuction = x;
    }

    // slot logic

    // returns the index of the next open slot (0,1,2)
    // or -1 when the city is full
    public int getNextOpenSlot() {
        for (int i = 0; i < occupants.length; i++) {
            if (occupants[i] == null) return i;
        }
        return -1;
    }

    // returns cost of the next available slot based on current step
    // step 1: only slot 0 (10) is available
    // step 2: slots 0-1 (10, 15) available
    // step 3: all slots (0,1,2) available
    public int getNextSlotCost(int currentStep) {
        for (int i = 0; i < currentStep && i < occupants.length; i++) {
            if (occupants[i] == null) return slotCosts[i];
        }
        return -1; // no slot available for this step
    }

    //Returns true if the city has an open slot for the given step.
    public boolean hasOpenSlot(int currentStep) {
        return getNextSlotCost(currentStep) != -1;
    }

    // returns true if this player already occupies this city.
    public boolean isOccupiedBy(Player player) {
        for (Player p : occupants) {
            if (p != null && p.equals(player)) return true;
        }
        return false;
    }

    
    // Adds a player to the next open slot. Returns true if successful.
    // has step restriction.
    public boolean addOccupant(Player player, int currentStep) {
        for (int i = 0; i < currentStep && i < occupants.length; i++) {
            if (occupants[i] == null) {
                occupants[i] = player;
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}