public class ResourceMarket {
    private int[][] coalSlots;
    private int[][] oilSlots;
    private int[][] trashSlots;
    private int[][] uraniumSlots;
    private static final int[] COAL_PRICES = {1,2,3,4,5,6,7,8};
    private static final int[] OIL_PRICES = {1,2,3,4,5,6,7,8};
    private static final int[] TRASH_PRICES = {1,2,3,4,5,6,7,8};
    private static final int[] URANIUM_PRICES = {1,2,3,4,5,6,7,8,10,12,14,16};
    private boolean uraniumResupplyStopped;

    public ResourceMarket(){

    }

    public int calculateCost(ResourceType type, int amount){
        return 0;
    }

    public int buyResource(ResourceType type, int amount){
        return 0;
    }

    public void restock(int playerCount, int gameStep){

    }
}
