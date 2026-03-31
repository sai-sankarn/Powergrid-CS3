import java.util.*;

public class Player {
    private String name;
    private String color; 
    private int money;
    private List<Powerplant> powerplants;
    private Set<City> ownedCities;
    private boolean passedAuction;
    private boolean boughtThisRound;

    public Player(String name, int money, String color){
        this.name = name;
        this.color = color;
        this.money = money;
        powerplants = new ArrayList<>();
    }

    // basic getters
    public String getName(){return name;}

    public String getColor(){return color;}

    public int getMoney(){return money;}

    public List<Powerplant> getPowerplant(){return powerplants;}

    public Set<City> getOwnedCities(){return ownedCities;}

    public boolean ifPassedAuction(){return passedAuction;}

    public boolean ifBoughtThisRound(){return boughtThisRound;}

    // basic setters, add, subtract
    public void setName(String newName){ this.name = newName;}

    public void setColor(String newColor){this.color = newColor;}

    public void setPassedAuction(boolean x){
        this.passedAuction = x;
    }

    public void setBoughtThisRound(boolean x){
        this.boughtThisRound = x;
    }

    public void spendMoney(int amount){
        money -= amount;
    }

    public void addMoney(int amount){
        money += amount;
    }

    public boolean addPowerPlant(Powerplant p){
        if(powerplants.size() < 3){
            powerplants.add(p);
            return true;
        }
        return false;
    }
    
    public Powerplant discardPowerPlant(Powerplant p){
        return powerplants.remove(powerplants.indexOf(p));
    }

    public void addCity(City c){
        ownedCities.add(c);
    }

    // checks if a specific plant has room; capacity is 2x intake for standard plants, 2x combined for hybrids
    public boolean canStoreResource(Powerplant p, ResourceType type, int amount){

    }

    public void addResource(Powerplant p, ResourceType type, int amount){

    }

    public int getTotalPowerCapacity(){

    }
    
}
