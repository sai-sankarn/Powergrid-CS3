import java.util.*;

public class Powerplant {
    private int number;
    private List<ResourceType> acceptedResources;
    private int resourceIntake;
    private int houseOutput;
    Map<ResourceType, Integer> storedResources;

    public Powerplant(int number, List<ResourceType> acceptedResources, int resourceIntake, int houseOutput){
        this.number = number; 
        this.acceptedResources = acceptedResources;
        this.resourceIntake = resourceIntake;
        this.houseOutput = houseOutput;
        this.acceptedResources = acceptedResources;
    }

    // basic getters
    public int getNumber(){
        return number;
    }

    public List<ResourceType> getAcceptedResources(){
        return acceptedResources;
    }

    public int getResourceIntake(){
        return resourceIntake;
    }

    public int getHouseOutput(){
        return houseOutput;
    }

    public int getStoredTotal(){
        int total = 0;
        for(Integer amount : storedResources.values()){
            total += amount;            
        }
        return total;
    }

    public int getStorageCapacity(){
        return resourceIntake * 2;
    }

    // boolean methods
    public boolean isHybrid(){
        return acceptedResources.size() > 1;
    }

    public boolean isEcological(){
        return acceptedResources.isEmpty();
    }

    public boolean canStore(ResourceType type, int amount){
        if(acceptedResources.contains(type) && getStoredTotal() + amount <= getStorageCapacity()){
            return true;
        }
        return false;
    }

    
}
