package minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.State;
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
	private State.StateView state;
	private int playerNum = 0; //indicates which player's perspective (footman or archer) for the curr gamestate
	private List<Integer> unitIDs = new ArrayList<Integer>();
	private List<Integer> enemyUnitIDs = new ArrayList<Integer>();
	private ArrayList<Stack<MapLocation>> optimalPath = new ArrayList<Stack<MapLocation>>();
	
	public List<Integer> getUnitIDs() {
		return unitIDs;
	}
	public List<Integer> getEnemyUnitIDs() {
		return enemyUnitIDs;
	}
	
    public GameState(State.StateView state) { //changes the turn each time the constructor is called...?not sure if this is ideal
    	this.state = state;
    	Integer[] playerNums = state.getPlayerNumbers();
    	// get the footman location
        unitIDs = state.getUnitIds(playerNums[playerNum]);
        enemyUnitIDs = state.getUnitIds(playerNums[(playerNum + 1) % 2]); //if we're 1, then they're 0
    	for (int i = 0; i < unitIDs.size(); i++) 
    		enemyDistance += getNearestEnemyDist(state.getUnit(unitIDs.get(i)));
    } //should I change turns here?
    
    public int getNearestEnemyDist(UnitView unit) {
    	int minDist = 1000000;
    	for (Integer enemyID: enemyUnitIDs) {
    		minDist = Math.min(minDist, MapLocation.calculateManhattan(getLocation(unit), getLocation(state.getUnit(enemyID))));
    	}
    	return minDist;
    }
    public int getPlayerNum() {
    	return playerNum;
    }
    public void changeTurn() {
    	playerNum = (playerNum + 1) % 2;
    }
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
    		enemyDistanceToEdge += Math.min(state.getYExtent() - enemy.getYPosition(), enemy.getYPosition());
    	}
    }
  
    public MapLocation getLocation(UnitView dude) {
    	return new MapLocation(dude.getXPosition(), dude.getYPosition(), null, 0, 0);
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
        return enemyDistanceToEdge/(0.4 * enemyDistance + 0.2 * allyDistance + 0.4 * distanceFromBest) ;
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
    	List<GameStateChild> childrens = null;
    	Map<Integer, Action> move = null;
    	State.StateView state = null;
    	
    		
       	//gets different combinations of directions for enemies and our team. I'm crying, do I really need this many for loops
    	for(Direction direction : Direction.values())
        {
    		//move for first footman
            move.put(unitIDs.get(0), Action.createPrimitiveMove(unitIDs.get(0), direction));
            
            for(Direction direction2 : Direction.values())
            {
            	//move for second footman
				move.put(unitIDs.get(1), Action.createPrimitiveMove(unitIDs.get(1), direction2));
				for(Direction direction3 : Direction.values())
	            {
					//move for first archer
					move.put(enemyUnitIDs.get(0), Action.createPrimitiveMove(enemyUnitIDs.get(0), direction3));
					if(enemyUnitIDs.get(1)!=null)
					{
						for(Direction direction4 : Direction.values())
			            {
							//move for second archer if she exists
							move.put(enemyUnitIDs.get(1), Action.createPrimitiveMove(enemyUnitIDs.get(1), direction4));
							GameState gameState = new GameState(state);
					    	GameStateChild child = new GameStateChild(move,gameState);
					    	childrens.add(child);
					    	return childrens;
			            }
					}
				}
            }
        }
    	

    	
        return null;
    }
}