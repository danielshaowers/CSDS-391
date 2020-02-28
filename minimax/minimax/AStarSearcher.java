package minimax;

import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import edu.cwru.sepia.environment.model.state.State;

//calculate AStar distance. Recalculate if agent moves __ distance away
public class AStarSearcher {
	Stack<MapLocation> path = new Stack<MapLocation>();
	  
	public Stack<MapLocation> AstarSearch(Stack<MapLocation> path, State.StateView state, MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations){		  
	    path.clear(); //want to empty stack every time Astar is called
	   	int nextposx, nextposy; 	//the next coordinates to move to
	   	Hashtable<Integer, MapLocation> closed = new Hashtable<Integer, MapLocation>(); //this becomes unnecessary?
	   	PriorityQueue<MapLocation> open = new PriorityQueue<MapLocation>(); //tracks any potential nodes
	    MapLocation temp = new MapLocation(0, 0, null, 0); //the map location specified by nextpos
	    open.add(start);
	    while (open.size() != 0) { 
	    	MapLocation current = open.poll(); //get the cheapest node
	    	if (current.equals(goal)) //if the goal is found
	    		return tracePath(current); //helper method that traces from goal	
	    	closed.putIfAbsent(current.hashCode(), current); //add current to closed list
	    	for(int x = -1; x < 2; x++) { //gets all neighbors
	    		for(int y= -1; y < 2; y++) { 
	    			nextposx = current.x + x; //nextpos is the next coordinate we're going to check
	            	nextposy = current.y + y;
	            	temp = new MapLocation(nextposx, nextposy, current, Float.MAX_VALUE); //set temp to the new coordinate  
	            	//skips positions that either don't exist or is current player position. 
	            	if (nextposx < xExtent && nextposy < yExtent && !state.isResourceAt(nextposx, nextposy) 
	            			&& nextposx > -1 && nextposy > -1 && (enemyFootmanLoc == null || !enemyFootmanLoc.equals(temp))) { 
	            		temp.cost = (float) calculateEuclidean(temp, goal) + current.cost; //f = g+h
	            		//if current has never been visited, or if cheaper than what's already visited
	            		MapLocation hashVal = closed.get(temp.hashCode()); //tries to find temp in hash table
	            		//adds to open list if temp is cheaper than other paths to temp in closed list
	            		if (hashVal == null || (temp.equals(hashVal) && temp.cost < hashVal.cost))
	            			open.add(temp); 
	            		}
	            	}
	    		}
	        }
	    	return path;    
	    }
	    
	    //calculates euclidean distance given the current node and goal node
	    public double calculateEuclidean(MapLocation current, MapLocation goal) {
	    	return Math.sqrt(Math.pow(current.x - goal.x, 2) + Math.pow(current.y - goal.y, 2));
			
	    }
	    //returns whether the target destination is adjacent to our current location
	    private boolean isAdjacent(MapLocation current, MapLocation target) {
	    	return Math.abs(current.x - target.x) <= 1 && Math.abs(current.y - target.y) <= 1;
	    }
	    //trace back the path from the goal node		
	    public Stack<MapLocation> tracePath(MapLocation goal) {
	    	Stack<MapLocation> path = new Stack<MapLocation>();
	    	for (MapLocation parent = goal.cameFrom; parent.cameFrom != null; parent = parent.cameFrom) 
	    		path.push(parent);
	    	return path;
	    }
	    
	   public class MapLocation implements Comparable<MapLocation> {
	        public int x, y;
	        MapLocation cameFrom; //the node leading to our current
	        float cost; //cost represents the heuristic value

	        public MapLocation(int x, int y, MapLocation cameFrom, float cost)
	        {
	            this.x = x;
	            this.y = y;
	            this.cost = cost;
	            this.cameFrom = cameFrom;
	            
	        }
	        @Override
	        public boolean equals(Object ml) {
	        	if (ml instanceof MapLocation)
	        		return this.x == ((MapLocation)ml).x && this.y == ((MapLocation)ml).y;
	        	return false;
	        }
	        
	        public float getCost() {
	        	return cost;
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
}
