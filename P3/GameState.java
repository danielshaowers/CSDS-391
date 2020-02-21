package minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
import minimax.AStarSearcher.MapLocation;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {
    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns the IDs of all of the obstacles in the map
     * state.getResourceNode(int resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     * 
     * You can get a list of all the units belonging to a player with the following command:
     * state.getUnitIds(int playerNum): gives a list of all unit IDs beloning to the player.
     * You control player 0, the enemy controls player 1.
     * 
     * In order to see information about a specific unit, you must first get the UnitView
     * corresponding to that unit.
     * state.getUnit(int id): gives the UnitView for a specific unit
     * 
     * With a UnitView you can find information about a given unit
     * unitView.getXPosition() and unitView.getYPosition(): get the current location of this unit
     * unitView.getHP(): get the current health of this unit
     * 
     * SEPIA stores information about unit types inside TemplateView objects.
     * For a given unit type you will need to find statistics from its Template View.
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit type deals
     * unitView.getTemplateView().getBaseHealth(): The initial amount of health of this unit type
     *
     * @param state Current state of the episode
     */
	private ArrayList<Stack<MapLocation>> bestPath = new ArrayList<Stack<MapLocation>>();
	private double[] enemyDistance;
	private double[] distanceFromBest;
	private int maxDepth;
	public List<Integer> getUnitIDs() {
		return unitIDs;
	}
	public List<Integer> getEnemyUnitIDs() {
		return enemyUnitIDs;
	}
	private List<Integer> unitIDs = new ArrayList<Integer>();
	private List<Integer> enemyUnitIDs = new ArrayList<Integer>();
	
    public GameState(State.StateView state) {
    	Integer[] playerNums = state.getPlayerNumbers();
    	int playernum = playerNums[0];
    	int enemyPlayerNum = playerNums[1];
    	// get the footman location
        unitIDs = state.getUnitIds(playernum);
        enemyUnitIDs = state.getUnitIds(enemyPlayerNum);
        enemyDistance = new double[unitIDs.size()];
    	for (int i = 0; i < unitIDs.size(); i++) {
    		UnitView dude = state.getUnit(unitIDs.get(i));
    		enemyDistance[i] = calculateEuclidean(getLocation(dude), getLocation(state.getUnit(enemyUnitIDs.get(i % enemyUnitIDs.size()))));
    	}
    }
    public GameState(State.StateView state, ArrayList<Stack<MapLocation>> optimalPath) {
    	bestPath = optimalPath;
    	Integer[] playerNums = state.getPlayerNumbers();
    	int playernum = playerNums[0];
    	int enemyPlayerNum = playerNums[1];
    	// get the footman location
        List<Integer> unitIDs = state.getUnitIds(playernum);
        List<Integer> enemyUnitIDs = state.getUnitIds(enemyPlayerNum);
        enemyDistance = new double[unitIDs.size()];
    	for (int i = 0; i < unitIDs.size(); i++) { //find distance away
    		UnitView dude = state.getUnit(unitIDs.get(i));
    		enemyDistance[i] = calculateEuclidean(getLocation(dude), getLocation(state.getUnit(enemyUnitIDs.get(i % enemyUnitIDs.size()))));
    	}
    	for (int i = 0; i < unitIDs.size(); i++) {//does this update the path passed in tho?
    		MapLocation current = optimalPath.get(i).pop();
    		distanceFromBest[i] = calculateEuclidean(getLocation(state.getUnit(unitIDs.get(i))), current); 
    	} //another measure of utility, distanceFromBest. might want to make this just one integer
    }
  
    private MapLocation getLocation(UnitView dude) {
    	return new MapLocation(dude.getXPosition(), dude.getYPosition(), null, 0, 0);
    }
   //finds the best path for all of our footmen
    public ArrayList<Stack<MapLocation>> getOptimalPath(int max_depth, State.StateView state, List<Integer> unitIDs, List<Integer> enemyUnitIDs) {   	
    	AStarSearcher search = new AStarSearcher();
    	bestPath.clear();
    	for (int i = 0; i < unitIDs.size(); i++) {
    		UnitView footman = state.getUnit(unitIDs.get(i));  
    		UnitView archer = state.getUnit(enemyUnitIDs.get(i % enemyUnitIDs.size()));
    		MapLocation start = new MapLocation(footman.getXPosition(), footman.getYPosition(), null, 0, 0);
    		MapLocation goal = new MapLocation(archer.getXPosition(), archer.getYPosition(), null, 0, 0);
    		bestPath.add(search.AstarSearch(max_depth, new Stack<MapLocation>(), state, start, goal, 
    				state.getXExtent(), state.getYExtent()));
    	}
    	return bestPath;
    }
    
    public ArrayList<Stack<MapLocation>> getPath(){
    	return bestPath;
    }
    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() { //health? distance to enemy #1 priority. closeness to Astar path?
    	//heuristic distance. maybe A* path length, outside of path range, near a corner. far from other footman
        
    	return 0.0;
    }

    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     * 
     * It may be useful to be able to create a SEPIA Action. In this assignment you will
     * deal with movement and attacking actions. There are static methods inside the Action
     * class that allow you to create basic actions:
     * Action.createPrimitiveAttack(int attackerID, int targetID): returns an Action where
     * the attacker unit attacks the target unit.
     * Action.createPrimitiveMove(int unitID, Direction dir): returns an Action where the unit
     * moves one space in the specified direction.
     *
     * You may find it useful to iterate over all the different directions in SEPIA. This can
     * be done with the following loop:
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     * 
     * If you wish to explicitly use a Direction you can use the Direction enum, for example
     * Direction.NORTH or Direction.NORTHEAST.
     * 
     * You can check many of the properties of an Action directly:
     * action.getType(): returns the ActionType of the action
     * action.getUnitID(): returns the ID of the unit performing the Action
     * 
     * ActionType is an enum containing different types of actions. The methods given above
     * create actions of type ActionType.PRIMITIVEATTACK and ActionType.PRIMITIVEMOVE.
     * 
     * For attack actions, you can check the unit that is being attacked. To do this, you
     * must cast the Action as a TargetedAction:
     * ((TargetedAction)action).getTargetID(): returns the ID of the unit being attacked
     * 
     * @return All possible actions and their associated resulting game state
     */
    //gameStateChild = game state with action associated with how we reached that child. has public variables that are just the variables
    public List<GameStateChild> getChildren() {
        return null;
    }
    
    //calculates euclidean distance given the current node and goal node
    public double calculateEuclidean(MapLocation current, MapLocation goal) {
    	return Math.sqrt(Math.pow(current.x - goal.x, 2) + Math.pow(current.y - goal.y, 2));		
    }
    
    //used to find the best path
    public class AStarSearcher {
    	Stack<MapLocation> path = new Stack<MapLocation>();
    	  
    	public Stack<MapLocation> AstarSearch(int maxDepth, Stack<MapLocation> path, State.StateView state, MapLocation start, MapLocation goal, int xExtent, int yExtent){		  
    	    	path.clear(); //want to empty stack every time Astar is called
    	    	int nextposx, nextposy; 	//the next coordinates to move to
    	    	Hashtable<Integer, MapLocation> closed = new Hashtable<Integer, MapLocation>(); //this becomes unnecessary?
    	    	PriorityQueue<MapLocation> open = new PriorityQueue<MapLocation>(); //tracks any potential nodes
    	        MapLocation temp = new MapLocation(0, 0, null, 0, 0); //the map location specified by nextpos
    	        open.add(start);
    	        while (open.size() != 0) { 
    	        	System.out.println("Open list size " + open.size());
    	        	MapLocation current = open.poll(); //get the cheapest node
    	        	if (current.equals(goal) || current.getParentCount() >= maxDepth) //if the goal is found
    	        		return tracePath(current); //helper method that traces from goal	
    	        	closed.putIfAbsent(current.hashCode(), current); //add current to closed list
    	    		for(int x = -1; x < 2; x++) { //gets all neighbors
    	            	for(int y= -1; y < 2; y++) { 
    	            		nextposx = current.x + x; //nextpos is the next coordinate we're going to check
    	            		nextposy = current.y + y;
    	            		temp = new MapLocation(nextposx, nextposy, current, Float.MAX_VALUE, current.getParentCount() + 1); //set temp to the new coordinate  
    	            		//skips positions that either don't exist or is current player position. 
    	            		if (nextposx < xExtent && nextposy < yExtent && !state.isResourceAt(nextposx, nextposy) 
    	            				&& nextposx > -1 && nextposy > -1) { 
    	            			temp.cost = (float) calculateEuclidean(temp, goal) + current.cost; //f = g+h
    	            			//if current has never been visited, or if cheaper than what's already visited
    	            			MapLocation hashVal = closed.get(temp.hashCode()); //tries to find temp in hash table
    	            			//adds to open list if temp is cheaper than other paths to temp in closed list
    	            		//	System.out.println("hashVal == null " + hashVal == null);
    	            			if (hashVal == null || (temp.equals(hashVal) && temp.cost < hashVal.cost))
    	            					open.add(temp); 
    	            			}
    	            	}
    	    		}
    	        }
    	    	return path;    
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
    }

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
}
