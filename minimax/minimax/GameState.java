package minimax;

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
import minimax.MapLocation;

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
	private double enemyDistance = 0;
	private double distanceFromBest =0;
	private double allyDistance = 0;
	private double enemyDistanceToEdge = 0;
	private int turnNum = 0; //is there ANY continuity here?? how do things carry over between states?
	private State.StateView state;
	//private int playerNum = 0; //indicates which player's perspective (footman or archer) for the curr gamestate
	private List<Integer> unitIDs = new ArrayList<Integer>();
	private List<Integer> enemyUnitIDs = new ArrayList<Integer>();
	private ArrayList<Stack<MapLocation>> optimalPath = new ArrayList<Stack<MapLocation>>();
	private HashMap<Integer, LinkedList<Integer>> inArrowRange = new HashMap<Integer, LinkedList<Integer>>(); //key is attacker, value is target 
	private HashMap<Integer, LinkedList<Integer>> inMeleeRange = new HashMap<Integer, LinkedList<Integer>>(); //key is attacker, then target
	private int unitHP = 0; 
	private int enemyHP = 0;
	private HashMap<Integer, FutureUnit> futureUnits = new HashMap<Integer, FutureUnit>();
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
    public GameState(State.StateView state) { //changes the turn each time the constructor is called...?not sure if this is ideal
    	this.state = state;    	
    	Integer[] playerNums = state.getPlayerNumbers();
    	futureUnits = new HashMap<Integer, FutureUnit>();
    	// get the footman location
        unitIDs = state.getUnitIds(playerNums[0]);
    	enemyUnitIDs = state.getUnitIds(playerNums[1]); //if we're 1, then they're 0
        int[] enemyDist;
        for (Integer i : enemyUnitIDs) {
        	UnitView unit = state.getUnit(i);
        	futureUnits.put(unit.getID(), new FutureUnit(unit.getXPosition(), 
    				unit.getYPosition(), unit.getID(), unit.getHP(), turnNum));
        }
    	for (int i = 0; i < unitIDs.size(); i++) {
    		UnitView unit = state.getUnit(unitIDs.get(i));
    		futureUnits.put(unit.getID(), new FutureUnit(unit.getXPosition(), 
    				unit.getYPosition(), unit.getID(), unit.getHP(), turnNum));
    		
    		unitHP += unit.getHP();
    		enemyDist = findEnemyDistances(state.getUnit(unitIDs.get(i)));
    		
    		enemyDistance += enemyDist[0] + enemyDist[2];
    		for (int j = 0; j < enemyDist.length; j += 2) {
    		if (enemyDist[j] <= state.getUnit(enemyUnitIDs.get(i)).getTemplateView().getRange()) {
    			if (inArrowRange.get(enemyUnitIDs.get(i)) == null)
    				inArrowRange.put(enemyUnitIDs.get(i), new LinkedList<Integer>());
    			inArrowRange.get(enemyUnitIDs.get(i)).add(enemyDist[j + 1]); //puts archer id, then target id
    		}
    		if (enemyDist[j] == 1) {
    			if (inMeleeRange.get(unitIDs.get(i)) == null)
    				inMeleeRange.put(unitIDs.get(i), new LinkedList<Integer>());
    			inMeleeRange.get(unitIDs.get(i)).add(enemyDist[j + 1]);
    		}
        }    	
    	}	
    }
    
    public GameState(GameState gs, HashMap<Integer, FutureUnit> fus) { //this doesn't work because the previous gs is of the other player
    	//it gets the new state when it's called in middle step. there's a chance
    	//that I would have to track the effects of actions (movement and attack) separately, otherwise state will never change, right?
    	this.state = gs.getState(); //is this state outdated?
    	this.futureUnits = fus;
    		int[] enemyDist;
    		Integer[] playerNums = state.getPlayerNumbers();
    		// get the footman location
    		unitIDs = state.getUnitIds(playerNums[0]);       
    		enemyUnitIDs = state.getUnitIds(playerNums[1]); //if we're 1, then they're 0
    		for (int i = 0; i < unitIDs.size(); i++) {
    			FutureUnit unit = fus.get(unitIDs.get(i));
    			turnNum = unit.getTurn() + 1;
    	    		if (unit.getGood())
    	    			this.unitHP += unit.getHp();
    	    		else
    	    			this.enemyHP+= unit.getHp();   
    			
    			enemyDist = findEnemyDistances(unit);
    			enemyDistance += enemyDist[0];
    			UnitView enemy = state.getUnit(enemyUnitIDs.get(0));
    			for (int j = 0; j < enemyDist.length; j += 2) {
    				if (enemyDist[j] <= enemy.getTemplateView().getRange()) {
    					if (inArrowRange.get(enemyUnitIDs.get(i)) == null)
    						inArrowRange.put(enemyUnitIDs.get(i), new LinkedList<Integer>());
    					inArrowRange.get(enemyUnitIDs.get(i)).add(enemyDist[j + 1]); //puts archer id, then target id
    				}
    				if (enemyDist[j] == 1) {
    					if (inMeleeRange.get(unitIDs.get(i)) == null)
    						inMeleeRange.put(unitIDs.get(i), new LinkedList<Integer>());
    					inMeleeRange.get(unitIDs.get(i)).add(enemyDist[j + 1]);
    				}
    			}
    		}
    		System.out.println("turn number is " + turnNum);
    	}
    
    //index 0: min distance of enemy1. index 1: id of enemy1. 
    //index 2: distance of enemy 2. index 3: id if 2nd enemy is in range. else 10000
    public int[] findEnemyDistances(FutureUnit unit) {
    	int[] distAndID = {10000, 10000, 10000, 10000};
    	int i = 0;
    	for (Integer enemyID: enemyUnitIDs) {    
    		UnitView enemy = state.getUnit(enemyID);
    		enemyDistanceToEdge += Math.min(state.getXExtent() - enemy.getXPosition(), enemy.getXPosition());
    		enemyDistanceToEdge += Math.min(state.getYExtent() - enemy.getYPosition(), enemy.getYPosition());
    		distAndID[i++] =  MapLocation.calculateManhattan(getLocation(unit), getLocation(state.getUnit(enemyID)));   	;
    		distAndID[i++] = enemyID;
    	}
    	return distAndID;
    }
    
    public int[] findEnemyDistances(UnitView unit) {
    	int[] distAndID = {10000, 10000, 10000, 10000};
    	int i = 0;
    	for (Integer enemyID: enemyUnitIDs) {    
    		UnitView enemy = state.getUnit(enemyID);
    		enemyDistanceToEdge += Math.min(state.getXExtent() - enemy.getXPosition(), enemy.getXPosition());
    		enemyDistanceToEdge += Math.min(state.getYExtent() - enemy.getYPosition(), enemy.getYPosition());
    		distAndID[i++] =  MapLocation.calculateManhattan(getLocation(unit), getLocation(state.getUnit(enemyID)));   	;
    		distAndID[i++] = enemyID;
    	}
    	return distAndID;
    }
    
    public int getPlayerNum() {
    	return 0;
    }
   /* public void changeTurn() {
    	playerNum = (playerNum + 1) % 2;
    }*/
    public State.StateView getState(){
    	return state;
    }
    
    public GameState(State.StateView state, MapLocation optimalPath) { //when a-b search is called, must pop while creating game state
    	//bestPath = optimalPath;
    	this.state = state;
    	Integer[] playerNums = state.getPlayerNumbers();
    	int playernum = playerNums[0];
    	int enemyPlayerNum = playerNums[1];
    	// get the footman location
        List<Integer> unitIDs = state.getUnitIds(playernum);
        List<Integer> enemyUnitIDs = state.getUnitIds(enemyPlayerNum);
    	for (int i = 0; i < unitIDs.size(); i++) { //find distance away from enemy and from optimal path
    		UnitView dude = state.getUnit(unitIDs.get(i));
    		enemyDistance += MapLocation.calculateManhattan(getLocation(dude), getLocation(state.getUnit(enemyUnitIDs.get(i % enemyUnitIDs.size()))));
    		MapLocation current = optimalPath;
    		distanceFromBest += MapLocation.calculateManhattan(getLocation(state.getUnit(unitIDs.get(i))), current); 
    		for(int j = i+1; j < unitIDs.size(); j++) {
    			allyDistance += MapLocation.calculateManhattan(getLocation(dude), getLocation(state.getUnit(unitIDs.get(j))));
    		}
    	}
    	for (Integer e: enemyUnitIDs) {
    		UnitView enemy = state.getUnit(e);
    		enemyDistanceToEdge += Math.min(state.getXExtent() - enemy.getXPosition(), enemy.getXPosition());
    		}
    }
  
    public MapLocation getLocation(UnitView dude) {
    	return new MapLocation(dude.getXPosition(), dude.getYPosition(), null, 0, 0);
    }
    public MapLocation getLocation(FutureUnit dude) {
    	return new MapLocation(dude.getX(), dude.getY(), null, 0, 0);
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
    //if we could make this proportional to our unit health that'd be dope. find if in range and how much damage they do
    public double getUtility() { //health? distance to enemy #1 priority. closeness to Astar path? out of range
    	//heuristic distance. maybe A* path length, outside of path range, near a corner. far from other footman
        return   (enemyDistanceToEdge - 5*enemyDistance + allyDistance + unitHP - enemyHP);
        //(turnNum % 2 * -1) do we need to do this or is it accounted for elsewhere. elsewhere
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
    	List<GameStateChild> children = new LinkedList<GameStateChild>();
    	List<Integer> movers = unitIDs; //the units moving this turn
    	if (turnNum % 2 == 1)
    		movers = enemyUnitIDs;
    	Stack<HashMap<Integer, Action>> actionMaps = possibleMoves(movers);
    	while (actionMaps.size() != 0) {
    		HashMap<Integer, Action> actions = actionMaps.pop(); //set of actions taken to reach a new state
    		HashMap<Integer, FutureUnit> nextState = new HashMap<Integer, FutureUnit>(); //new list of future units
    		FutureUnit fu;
    		for (Integer id : state.getAllUnitIds()) {
    			if (actions.get(id) != null) {
    				if(actions.get(id).getType().equals(ActionType.PRIMITIVEATTACK)) {
    				TargetedAction ta = (TargetedAction)actions.get(id); 
    			    fu = futureUnits.get(ta.getTargetId()).duplicate();
    			    //update health of attacked unit
    				fu.attacked(actions.get(id), state.getUnit(unitIDs.get(0)).getTemplateView().getBasicAttack());	
    				nextState.put(fu.getId(), fu); //add attacked
    				nextState.put(id, futureUnits.get(id)); //add attacker
    				}
    				//if the action is a move, update the futureUnit's location
    				if (actions.get(id).getType().equals(ActionType.PRIMITIVEMOVE)) {
    					fu = futureUnits.get(id).duplicate();
    					fu.moved(actions.get(id));
    					nextState.put(id, fu);
    				}
    			}
    			else  //add units that don't have actions too!
    				nextState.put(id, futureUnits.get(id));
    		}
    		children.add(new GameStateChild(actions, new GameState(this, nextState)));
    	}
    	System.out.println("children added?" + children.size());
        return children;
    }
    
    public Stack<HashMap<Integer, Action>> possibleMoves(List<Integer> ids){
    	List<Action> allActions = allActions(ids.get(0));
    	Stack<HashMap<Integer, Action>> fullList = new Stack<HashMap<Integer, Action>>();
    	HashMap<Integer, Action> moves = new HashMap<Integer, Action>();
    	int id = ids.get(1); 
   		for (Action action: allActions) {
   			if (ids.size() == 1) {
   				moves = new HashMap<Integer, Action>();
   				moves.put(id, action);
   				fullList.push(moves);
   			}
   			else {
   				List<Action> allActions2 = allActions(id);
   				for (Action action2: allActions2) {
   					moves = new HashMap<Integer, Action>();
    				moves.put(id, action);
    				moves.put(ids.get(1), action2);
    				fullList.push(moves);
   				}
   			}
   		}
   		return fullList;
    }
    //could probably do some recursive shit but i dont want to
    public List<Action> allActions(Integer id){
    	List<Action> move = new ArrayList<Action>();
    	for (Direction direction : Direction.values()) {               
    		if (direction.equals(Direction.NORTH) || direction.equals(Direction.EAST) ||
                direction.equals(Direction.SOUTH) || direction.equals(Direction.WEST)) {
                UnitView unit = state.getUnit(id);
                int nextposx = unit.getXPosition() + direction.xComponent();
                int nextposy = unit.getYPosition() + direction.yComponent();
                if (nextposx <= state.getXExtent() && nextposy <= state.getYExtent() && 
                		!state.isResourceAt(nextposx, nextposy) && nextposx > -1
                		&& nextposy > -1 &&!state.hasUnit(nextposx, nextposy)) {  
                			move.add(Action.createPrimitiveMove(id, direction));
                }
                if (turnNum % 2 == 1 && inArrowRange.get(id) != null) { //implies the unit is an archer and enemy is in range
                	for (Integer target: inArrowRange.get(id)) {
                		move.add(Action.createPrimitiveAttack(id, target));
                	}
                }
                if (turnNum % 2 == 0 && inMeleeRange.get(id) != null) { //implies the unit is a footman and enemy is in range
                	for (Integer target: inMeleeRange.get(id)) {
                		System.out.println("melee target is " + target);
                		move.add(Action.createPrimitiveAttack(id, target));
                	}
                }
    		}
    	}
    	return move;
    }
}