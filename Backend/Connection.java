package Backend;

public class Connection {
    private City target;
    private int cost;

    public Connection(City target, int cost) {
        this.target = target;
        this.cost = cost;
    }

    // getters
    
    public City getTarget() {
        return target;
    }

    public int getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "Connection -> " + target.getName() + " (cost: " + cost + ")";
    }
}