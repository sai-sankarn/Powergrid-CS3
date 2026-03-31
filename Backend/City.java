public class City {
    private String name;
    private int[] slotCosts = {10, 15, 20};
    private Player[] occupants;
    private boolean isMetropolis;
    private boolean passedAuction;

    public City(String name){
        this.name = name;
        this.occupants = new Player[3];
    }

    //getter
    public Player[] getOccupants(){
        return occupants;
    }

    public boolean ifMetropolis(){
        return isMetropolis;
    }
    
    public boolean ifPassedAuction(){
        return passedAuction;
    }
    
    //setter
    public void setMetropolis(boolean x){
        this.isMetropolis = x;
    }

    public void setPassedAuction(boolean x){
        this.passedAuction = x;
    } 
}
