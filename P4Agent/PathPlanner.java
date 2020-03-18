package P4Agent;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit;

public class PathPlanner {
	 public Stack<MapLocation> path = new Stack<MapLocation>();

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
	   	public int calculateManhattan(MapLocation current, MapLocation goal) {
	   		return Math.abs(current.x - goal.x) + Math.abs(current.y - goal.y);	
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
	public Stack<MapLocation> findPath(MapLocation current, MapLocation target, StateView state)
	    {
	        MapLocation startLoc = new MapLocation(current.x, current.y, null, 0, 0);

	        MapLocation goalLoc = new MapLocation(target.x, target.y, null, 0, 0);

	        // get resource locations
	        List<Integer> resourceIDs = state.getAllResourceIds();
	        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
	        for(Integer resourceID : resourceIDs)
	        {
	            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);
	            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0, 0));
	        }
	        System.out.println(startLoc.x);

	        return AstarSearch(state, startLoc, goalLoc, state.getXExtent(), state.getYExtent(), resourceLocations);
	    }
	    public Stack<MapLocation> AstarSearch(State.StateView state, MapLocation start, MapLocation goal, int xExtent, int yExtent, Set<MapLocation> resourceLocations)
	    {
	    	path.clear(); //want to empty stack every time Astar is called
	    	int nextposx, nextposy; 	//the next coordinates to move to
	    	Hashtable<Integer, MapLocation> closed = new Hashtable<Integer, MapLocation>(); //this becomes unnecessary?
	    	PriorityQueue<MapLocation> open = new PriorityQueue<MapLocation>(); //tracks any potential nodes
	        MapLocation temp = new MapLocation(0, 0, null, 0, 0); //the map location specified by nextpos
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
	            		temp = new MapLocation(nextposx, nextposy, current, Float.MAX_VALUE, 0); //set temp to the new coordinate  
	            		//skips positions that either don't exist or is current player position. 
	            		if (nextposx < xExtent && nextposy < yExtent && !state.isResourceAt(nextposx, nextposy) 
	            				&& nextposx > -1 && nextposy > -1) { 
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
	    private double calculateEuclidean(MapLocation current, MapLocation goal) {
	    	return Math.sqrt(Math.pow(current.x - goal.x, 2) + Math.pow(current.y - goal.y, 2));
			
	    }
	    //trace back the path from the goal node		
	    private Stack<MapLocation> tracePath(MapLocation goal) {
	    	for (MapLocation parent = goal.cameFrom; parent.cameFrom != null; parent = parent.cameFrom) 
	    		path.push(parent);
	    	return path;
	    }


}
