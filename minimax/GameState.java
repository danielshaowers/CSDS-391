package minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
//import minimax.MapLocation;
import javafx.util.Pair;
import minimax.Trees.Tree;

import java.util.*;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
//import minimax.MapLocation;

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
	private double enemyDistance = 0;	//sum of distance from nearest enemy
	private double distanceFromBest =0; //sum of distance from optimal path found by A star
	private double allyDistance = 0;	//distance between footmen
	private double enemyDistanceToEdge = 0; //distance of enemies to edges of map
	private int turnNum = 0; 			
	private State.StateView state;	//the original state
	//private int playerNum = 0; //indicates which player's perspective (footman or archer) for the curr gamestate
	private List<Integer> unitIDs = new ArrayList<Integer>();
	private List<Integer> enemyUnitIDs = new ArrayList<Integer>();
	//each maplocation array in the stack of optimalPaths is an array of optimal locations. 
	//the ith index of MapLocation[] corresponds to the ideal location after i moves by our units
	private ArrayList<MapLocation[]> optimalPaths = new ArrayList<MapLocation[]>(); 
	private HashMap<Integer, int[]> nearestEnemies = new HashMap<Integer, int[]>(); //the key is our unit ID, the int[] stores the id and distances of enemies from the key
	private HashMap<Integer, LinkedList<Integer>> inArrowRange = new HashMap<Integer, LinkedList<Integer>>(); //key is attacker, value is target 
	private HashMap<Integer, LinkedList<Integer>> inMeleeRange = new HashMap<Integer, LinkedList<Integer>>(); //key is attacker, then target
	private int unitHP = 0; //sum of footman healths
	private int enemyHP = 0; //sum of enemy healths
	private HashMap<Integer, FutureUnit> futureUnits = new HashMap<Integer, FutureUnit>(); //key is unit ID, value is the unit in the future after a hypothetical move
	
    public GameState(State.StateView state) { 
    	this.state = state;    	
    	Integer[] playerNums = state.getPlayerNumbers();
    	futureUnits = new HashMap<Integer, FutureUnit>();
    	unitIDs = state.getUnitIds(playerNums[0]);
    	enemyUnitIDs = state.getUnitIds(playerNums[1]); //if we're 1, then they're 0
        int[] enemyDist;
        for (Integer i : enemyUnitIDs) { //creates future units with the xy positions of our state
        	UnitView unit = state.getUnit(i); 
        	//add the future unit to our hashtable of future units, giving unit ID as the key
        	futureUnits.put(unit.getID(), new FutureUnit(unit.getXPosition(),
        			unit.getYPosition(), unit.getID(), unit.getHP(), turnNum, false)); 
        	enemyHP += unit.getHP();
        	enemyDistanceToEdge += Math.min(state.getXExtent() - unit.getXPosition(), unit.getXPosition());
    		enemyDistanceToEdge += Math.min(state.getYExtent() - unit.getYPosition(), unit.getYPosition());	  
        }
    	for (int i = 0; i < unitIDs.size(); i++) {
    		UnitView unit = state.getUnit(unitIDs.get(i));
    		futureUnits.put(unit.getID(), new FutureUnit(unit.getXPosition(), 
    				unit.getYPosition(), unit.getID(), unit.getHP(), turnNum, true));	
    		enemyDist = findEnemyDistances(futureUnits.get(unitIDs.get(i)));
    		//optimal path from our unit to nearest enemy
        	optimalPaths.add(i, getOptimalPath(state, unitIDs.get(i), enemyDist[1]));
			enemyDistance += enemyDist[0];
			
			//find which enemies are in range of attack
			for (int j = 0; j < enemyDist.length; j += 2) {
				Integer enemyID = enemyDist[j + 1];
				if (enemyDist[j] != 10000 && enemyDist[j] <= state.getUnit(enemyID).getTemplateView().getRange()) {
					if (inArrowRange.get(enemyID) == null)
						inArrowRange.put(enemyID, new LinkedList<Integer>());
					
					inArrowRange.get(enemyID).add(unit.getID()); //puts archer id, then target id
				}
				if (enemyDist[j] == 1) {
					if (inMeleeRange.get(unit.getID()) == null)
						inMeleeRange.put(unit.getID(), new LinkedList<Integer>());
					inMeleeRange.get(unit.getID()).add(enemyID);
				}
			}    	
    	}
    	allyDistance += calculateDistance(futureUnits.get(unitIDs.get(0)), futureUnits.get(unitIDs.get(1 % unitIDs.size())));	
    }
    public GameState(GameState gs, HashMap<Integer, FutureUnit> fus, ArrayList<MapLocation[]> optimalPath, MapLocation[] nextBestSpot ) {
    	enemyDistance = 0; //reset all values 
    	distanceFromBest =0;
    	allyDistance = 0;
    	enemyDistanceToEdge = 0;
    	unitHP = 0;
    	enemyHP = 0;
    	inArrowRange.clear();
    	inMeleeRange.clear();
    	this.state = gs.getState(); //note that this state is outdated
    	this.futureUnits = fus; 
    	int[] enemyDist;
    	optimalPaths = optimalPath;
     	int count = 0;
    	Integer[] playerNums = state.getPlayerNumbers(); //gets all team awesome's unit ids
    	unitIDs = state.getUnitIds(playerNums[0]); 
    	enemyUnitIDs = state.getUnitIds(playerNums[1]);	//gets enemy unit ids
    	List<Integer> allIDs = state.getAllUnitIds();   //gets all of the states  unit ids 
    	// goes through all the unit ids in the state and compare it to charactersitics of the possible future state (fus) 
    	for (int i = 0; i < allIDs.size(); i++) {  				
    		Integer unitID = allIDs.get(i);
    		FutureUnit unit = fus.get(unitID);
    		if (unit != null) { 					//checks to see if unit died
    		turnNum = unit.getTurn();
    	   	if (!unit.getGood()) { //only for enemy units
    	   		this.enemyHP+= unit.getHp();
    	   		enemyDistanceToEdge += Math.min(state.getXExtent() - unit.getX(), unit.getX());
        		enemyDistanceToEdge += Math.min(state.getYExtent() - unit.getY(), unit.getY());
    	   	}
    	  	else { //if the unit we're looking at is a footman
    	 		this.unitHP += unit.getHp(); 
    	  		enemyDist = findEnemyDistances(unit); 
    	  		//returns an array of distance between friendly unit and and enemy unit, next index is enemy id 
    	  		enemyDistance += enemyDist[0]; //distance of nearest  
        		for (int j = 0; j < enemyDist.length; j += 2) { //goes through all the enemy units
   	    			Integer enemyID = enemyDist[j + 1];
   	    			if (enemyDist[j] != 10000 && enemyDist[j] <= state.getUnit(enemyID).getTemplateView().getRange()) { //if there is an enemy attacker and they are within range
   	    				if (inArrowRange.get(enemyID) == null)	
   	    					inArrowRange.put(enemyID, new LinkedList<Integer>());
   	    					inArrowRange.get(enemyID).add(unitID); //key as the archer id and value as the team awesome id
   	    				}
   	    				if (enemyDist[j] == 1) { //checking to see if enemy the footman range
   	    					if (inMeleeRange.get(unitID) == null)
   	    						inMeleeRange.put(unitID, new LinkedList<Integer>());
   	    					inMeleeRange.get(unitID).add(enemyID); //key is footman and value is archer 
    	    				}
    	    	}             	
        		if (nextBestSpot[count] != null) //if there are still spots to move until we reach enemy
        			distanceFromBest += Math.abs(unit.getX() - nextBestSpot[count].x) + Math.abs(unit.getY() - nextBestSpot[count++].y);	
    	   	}
    	  }
    	}
    	allyDistance += calculateDistance(futureUnits.get(unitIDs.get(0)), futureUnits.get(unitIDs.get((1 % unitIDs.size())))); //gets Manhattan distance  		    	
    }

    //index 0: nearest enemy distance. index 1: id of nearest enemy. 
    //index 2: distance of enemy 2. index 3: id if 2nd enemy is in range. else 10000
    public int[] findEnemyDistances(FutureUnit unit) {
    	int[] distAndID = {10000, -1, 10000, -1};
    	int i = 0;
    	for (Integer enemyID: enemyUnitIDs) {    
    		FutureUnit enemy = futureUnits.get(enemyID);
    		distAndID[i++] =  calculateDistance(enemy, unit);
    		distAndID[i++] = enemyID;
    	} //sets the minimum distance at position 0 and nearest enemy at position 1
    	if (distAndID[0] > distAndID[2]) {
    		int min = distAndID[0];
    		int minID = distAndID[1];
    		distAndID[0] = distAndID[2];
    		distAndID[1] = distAndID[3];
    		distAndID[2] = min;
    		distAndID[3] = minID;
    	}
    	nearestEnemies.put(unit.getId(), distAndID);
    	return distAndID;
    }
    //gives Manhattan distance between two units
    public int calculateDistance(FutureUnit unit, FutureUnit unit2) {
    	return Math.abs(unit.getX() - unit2.getX()) + Math.abs(unit.getY() - unit2.getY());
    }
    
    public State.StateView getState(){
    	return state;
    }
    
    //chooses utility by distance from the optimal path, distance from enemy, whether the enemy is near an edge
    //our health, archer health, and distance to ally
    public double getUtility() { 
       int utility = 0;
       utility += -distanceFromBest * 10;  //we place highest priority on distance from optimal path
       utility += 5 * -enemyDistance; 
       utility += (int)(enemyDistanceToEdge + unitHP - 1000 * enemyHP - allyDistance); 
       return utility;
    }
    //get a list of all possible future game states based on the possible combination of actions
    public List<GameStateChild> getChildren() {
      	List<GameStateChild> children = new LinkedList<GameStateChild>();
    	List<Integer> movers = unitIDs; //the units moving this turn
    	if (turnNum % 2 == 1)
    		movers = enemyUnitIDs;
    	Stack<HashMap<Integer, Action>> actionMaps = possibleMoves(movers); //stack of possible action combinations
    	MapLocation[] nextSpots = new MapLocation[2]; //the optimal next step
    	for (int i = 0; i < unitIDs.size(); i++) {
    		//gets the ideal spot for our units if there are any. turnNum/2 because we move every other turn
    		if (optimalPaths.get(i).length - 1 >= Math.ceil(turnNum / 2)) { 
    			nextSpots[i] = optimalPaths.get(i)[(int)Math.ceil(turnNum/ 2)]; 
    		}
    	}
    	while (actionMaps.size() != 0) { //while there are still combinations of actions
    		HashMap<Integer, Action> actions = actionMaps.pop(); //set of actions taken to reach a new state
    		HashMap<Integer, FutureUnit> nextState = new HashMap<Integer, FutureUnit>(); //new list of future units after action applied
    		for (Integer id : state.getAllUnitIds()) {
    			FutureUnit fu;
    			if (actions.get(id) != null) { //if this unit performs an action, find its effect
    				if(actions.get(id).getType().equals(ActionType.PRIMITIVEATTACK)) {
    					if (nextState.get(id) == null)
    						nextState.put(id, futureUnits.get(id).duplicate()); //add attacker
    					TargetedAction ta = (TargetedAction)actions.get(id); 
    					fu = nextState.get(ta.getTargetId());
    					if (fu == null) //if the nextState doesn't already have the unit, create one
    						fu = futureUnits.get(ta.getTargetId()).duplicate();
    					//update health of attacked unit
    					fu.attacked(actions.get(id), state.getUnit(id).getTemplateView().getBasicAttack());	
    						nextState.put(fu.getId(), fu); //add attacked if not dead
    				}
    				//if the action is a move, update the futureUnit's location
    				if (actions.get(id).getType().equals(ActionType.PRIMITIVEMOVE)) {
    					fu = nextState.get(id);
    					if (fu == null)
    						fu = futureUnits.get(id).duplicate();
    					fu.moved(actions.get(id));
    					nextState.put(id, fu);
    				}
    			}
    			else {  //add units that don't have actions too
    				if (nextState.get(id) == null)
    					nextState.put(id, futureUnits.get(id).duplicate());
    			}
    		} 
    		children.add(new GameStateChild(actions, new GameState(this, nextState, optimalPaths, nextSpots)));
    	}
        return children;
    }
    
    //takes a list of ids that are our current movers and returns all possible moves
    //each HashMap is one set of actions that can be performed in a turn
    //sadly it only works for a max of two footman and two archers
    public Stack<HashMap<Integer, Action>> possibleMoves(List<Integer> ids){
    	//each HashMap in fullList is a set of actions that can be performed in one turn
    	Stack<HashMap<Integer, Action>> fullList = new Stack<HashMap<Integer, Action>>();
    	HashMap<Integer, Action> moves = new HashMap<Integer, Action>();
    	Integer id = ids.get(0);
    	List<Action> allActions = allActions(id); //finds all actions for a given unit id
    	for (Action action: allActions) {
    		if (ids.size() == 1) {
    			moves = new HashMap<Integer, Action>(); //reset moves list every time
    			moves.put(id, action);
    			fullList.push(moves);
    		}
    		else {
    	 		if (ids.size() == 2) {
    	 			Integer id2 = ids.get(1);
    	 			//obtains a full list of all the actions that can be taken by one unit
    	 			List<Action> allActions2 = allActions(id2);
    	 			for (Action action2: allActions2) { //for each action that can be taken, create a new hashmap
    	 				moves = new HashMap<Integer, Action>();
    	 				moves.put(id, action);
    	 				moves.put(id2, action2);
    	 				fullList.push(moves);
    	 			}
    	 		}
    		}
    		
    	}
   		return fullList;
    }
    //gets all possible combinations of moves for a single unit ID
    public List<Action> allActions(Integer id) {
    	List<Action> move = new ArrayList<Action>();
    	for (Direction direction : Direction.values()) {               
    		if (direction.equals(Direction.NORTH) || direction.equals(Direction.EAST) ||
                direction.equals(Direction.SOUTH) || direction.equals(Direction.WEST)) {
                FutureUnit unit = futureUnits.get(id);
                int nextposx = unit.getX() + direction.xComponent();
                int nextposy = unit.getY() + direction.yComponent();
                //checks if there's a tree or unit at the next potential step
                if (nextposx <= state.getXExtent() && nextposy <= state.getYExtent() && 
                		!state.isResourceAt(nextposx, nextposy) && nextposx > -1
                		&& nextposy > -1 && !state.isUnitAt(nextposx, nextposy))   
                			move.add(Action.createPrimitiveMove(id, direction));
                
                //if it's the archer's turn and our footmen are in range, add attacks to map            	
                if (turnNum % 2 == 1 && inArrowRange.get(id) != null) { 
                	for (Integer target: inArrowRange.get(id)) 
                		move.add(Action.createPrimitiveAttack(id, target));
                }
                //add all enemies in our melee range to the attack list 
                if (turnNum % 2 == 0 && inMeleeRange.get(id) != null) { //implies the unit is a footman and enemy is in range
                	for (Integer target: inMeleeRange.get(id)) {
                		move.add(Action.createPrimitiveAttack(id, target));
                	}
                }
    		}
    	}
    	return move; 
    }

    //finds the Astar path as an array. The ith index corresponds to the best position i turns from now
    public MapLocation[] getOptimalPath(State.StateView state, Integer unitID, Integer enemyUnitID) {   	
    	AStarSearcher search = new AStarSearcher();
    	Stack<MapLocation> optimalPath = new Stack<MapLocation>();
    	FutureUnit footman = futureUnits.get(unitID);  
    	FutureUnit archer = futureUnits.get(enemyUnitID);   		
    	MapLocation start = new MapLocation(footman.getX(), footman.getY(), null, 0, 0);
    	MapLocation goal = new MapLocation(archer.getX(), archer.getY(), null, 0, 0);
    	optimalPath = (search.AstarSearch(search.path, state, start, goal, state.getXExtent(), state.getYExtent()));
    	
    	MapLocation[] optimalPath2 = new MapLocation[optimalPath.size() + 1];
    	for (int i = 0; optimalPath.size() > 0; i++) 
    		optimalPath2[i] = optimalPath.pop();
    	
    	return optimalPath2; 
    }
    
    public List<Integer> getUnitIDs() {
		return unitIDs;
	}
	
	public List<Integer> getEnemyUnitIDs() {
		return enemyUnitIDs;
	}
	
	public int getTurnNum() {
		return turnNum;
	}
	public int getUnitHP() {
		return unitHP;
	}
	public int getEnemyHP() {
		return enemyHP;
	}
    public HashMap<Integer, FutureUnit> getFutureUnits(){
    	return futureUnits;
    }
    public HashMap<Integer, int[]> getNearestEnemy(){
    	return nearestEnemies;
    }
    //nested class to perform A Star Search
    public class AStarSearcher {
    	Stack<MapLocation> path = new Stack<MapLocation>();    	  
    	public Stack<MapLocation> AstarSearch(Stack<MapLocation> path, State.StateView state, MapLocation start, MapLocation goal, int xExtent, int yExtent){		  
    	    	path.clear(); //want to empty Stack every time Astar is called
    	    	Hashtable<Integer, MapLocation> closed = new Hashtable<Integer, MapLocation>(); //this becomes unnecessary?
    	    	PriorityQueue<MapLocation> open = new PriorityQueue<MapLocation>(); //tracks any potential nodes
    	        MapLocation temp = new MapLocation(0, 0, null, 0, 0); //the map location specified by nextpos
    	        open.add(start);
    	        while (open.size() != 0) { 
    	        	MapLocation current = open.poll(); //get the cheapest node   	     
    	        	if (current.equals(goal)) //if the goal is found
    	        		return tracePath(current); //helper method that traces from goal	
    	        	closed.putIfAbsent(current.hashCode(), current); //add current to closed list
    	    		for(int i = 1; i <= 4 ; i++) { //gets all neighbors. nope! no diagonals 
    	    			int nextposx = current.x;
    	    			int nextposy = current.y; 
    	    			if(i % 2 == 1)  //nextpos is the next coordinate we're going to check
    	            		nextposx += i - 2;
    	            	else
    	            		nextposy += i - 3;   	            	
    	            		temp = new MapLocation(nextposx, nextposy, current, Float.MAX_VALUE, current.getParentCount() + 1); //set temp to the new coordinate  
    	            		//skips positions that either don't exist or is current player position. 
    	            		if (nextposx < xExtent && nextposy < yExtent && !state.isResourceAt(nextposx, nextposy) 
    	            				&& nextposx > -1 && nextposy > -1) { 
    	            			temp.cost = (float) MapLocation.calculateManhattan(temp, goal) + current.cost; //f = g+h
    	            			//if current has never been visited, or if cheaper than what's already visited
    	            			MapLocation hashVal = closed.get(temp.hashCode()); //tries to find temp in hash table
    	            			//adds to open list if temp is cheaper than other paths to temp in closed list
    	            			if (hashVal == null || (temp.equals(hashVal) && temp.cost < hashVal.cost))
    	            					open.add(temp); 
    	            			}
    	    		}
    	        }
    	    	return path;    
    	    }
    	
    	    //trace back the path from the goal node		
    	    public Stack<MapLocation> tracePath(MapLocation goal) {
    	    	Stack<MapLocation> path = new Stack<MapLocation>();
    	    	for (MapLocation parent = goal.cameFrom; parent.cameFrom != null; parent = parent.cameFrom) { 
    	    		path.push(parent);
    	    	}
    	    	return path;
    	    }
    }
}

    