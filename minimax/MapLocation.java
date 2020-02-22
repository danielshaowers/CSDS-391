package minimax;

public class MapLocation implements Comparable<MapLocation> {
    public int x, y;
    MapLocation cameFrom; //the node leading to our current
    float cost; //cost represents the heuristic value
    public int parents;

    public MapLocation(int x, int y, MapLocation cameFrom, float cost, int parentCount)
    {
        this.x = x;
        this.y = y;
        this.cost = cost;
        this.cameFrom = cameFrom;
        this.parents = parentCount;
    }
    
    //calculates euclidean distance given the current node and goal node
   	public static double calculateEuclidean(MapLocation current, MapLocation goal) {
   		return Math.sqrt(Math.pow(current.x - goal.x, 2) + Math.pow(current.y - goal.y, 2));	
   		}
   	
    @Override
    public boolean equals(Object ml) {
    	return ml instanceof MapLocation && this.x == ((MapLocation)ml).x 
    			&& this.y == ((MapLocation)ml).y;
    }
    
    public float getCost() {
    	return cost;
    }
    
    public int getParentCount() {
    	return parents;
    }
    
    @Override //hash code using bijective theorem
    public int hashCode() {
    	int tmp = ( y +  ((x+1)/2));
        return x +  ( tmp * tmp);
    }
    
    @Override
    public int compareTo(MapLocation compare) {
    	if (compare.getCost() < this.cost)
    		return 1;
    	if (compare.getCost() > this.cost)
    		return -1;
    	return 0;
    }
}