package P4Agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is used to represent the state of the game after applying one of the avaiable actions. It will also
 * track the A* specific information such as the parent pointer and the cost and heuristic function. Remember that
 * unlike the path planning A* from the first assignment the cost of an action may be more than 1. Specifically the cost
 * of executing a compound action such as move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2). Implement the methods provided and
 * add any other methods and member variables you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
  * Note that SEPIA saves the townhall as a unit. Therefore when you create a GameState instance,
 * you must be able to distinguish the townhall from a peasant. This can be done by getting
 * the name of the unit type from that unit's TemplateView:
 * state.getUnit(id).getTemplateView().getName().toLowerCase(): returns "townhall" or "peasant"
 * 
 * You will also need to distinguish between gold mines and trees.
 * state.getResourceNode(id).getType(): returns the type of the given resource
 * 
 * You can compare these types to values in the ResourceNode.Type enum:
 * ResourceNode.Type.GOLD_MINE and ResourceNode.Type.TREE
 * 
 * You can check how much of a resource is remaining with the following:
 * state.getResourceNode(id).getAmountRemaining()
 *
 * I recommend storing the actions that generated the instance of the GameState in this class using whatever
 * class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {

	StateView state;
	GameState state2; //what is this
	int requiredGold;
	int requiredWood;
	int currentGold = 0;
	int currentWood = 0;
	int goldheld; //how much gold peasants are holding currently
	int woodheld; //how much wood peasants are holding currently
	double cost;
	boolean buildPeasants;
	Position agent; //what is this? why only one
	List<Integer> goldmines = null;
	List<Integer> tree = null;
	List<Integer> townHallIds = null;
	List<Integer> peasantIds = null;
	StripsAction camefrom = null;
    /**
     * Construct a GameState from a stateview object. This is used to construct the initial search node. All other
     * nodes should be constructed from the another constructor you create or by factory functions that you create.
     *
     * @param state The current stateview at the time the plan is being created
     * @param playernum The player number of agent that is planning
     * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
     * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
     * @param buildPeasants True if the BuildPeasant action should be considered
     */
    public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {
    	//initalizes variables
    	this.state = state;
    	this.requiredGold = requiredGold;
    	this.requiredWood = requiredWood;
    	this.buildPeasants = buildPeasants;
    	int townHallId=-1;
    	int peasantId=-1;
    	Integer[] playerNums = state.getPlayerNumbers();
		List<Integer> unitIDs = state.getUnitIds(playerNums[0]);
		
		//gets townhall and pesant id
    	for(int id : unitIDs)
    	{
    		UnitView unit = state.getUnit(id);
			String type = unit.getTemplateView().getName(); //obtains the name of each unit, then categorizes them in respective list
			if (type.equals("TownHall"))
				townHallIds.add(id);
			else if (type.equals("Peasant"))
				peasantIds.add(id);
    	}
    	this.currentGold = state.getResourceAmount(townHallId, ResourceType.GOLD);
    	this.currentWood = state.getResourceAmount(townHallId, ResourceType.WOOD);
    	List<Integer> resourceIds = state.getAllResourceIds();  //gets all resources
    	
    	//gets ids of all goldmines and trees in respective list
    	for(int id : resourceIds)
    	{
			if(state.getResourceNode(id).getType() == ResourceNode.Type.GOLD_MINE)
    			goldmines.add(id);
			else if(state.getResourceNode(id).getType() == ResourceNode.Type.TREE)
				tree.add(id);   			
    	}
    	
    	
    	
    }
    public GameState(Position agent, int golddepoist, int wooddeposit, int goldheld, int woodheld, StripsAction action, GameState state)
    {
    	//initialzes everything
    	this.requiredGold = state.requiredGold;
    	this.requiredWood = state.requiredWood;
    	this.currentGold = golddepoist;
    	this.currentWood = wooddeposit;
    	this.goldheld = goldheld;
    	this.woodheld = woodheld;
    	camefrom = action;
    	Integer[] playerNums = state.state.getPlayerNumbers();
		List<Integer> unitIDs = state.state.getUnitIds(playerNums[0]);
		this.state2 = state;
		
		//gets townhall and pesant id
    	for(int id : unitIDs)
    	{
    		UnitView unit = state.state.getUnit(id);
			String type = unit.getTemplateView().getName(); //obtains the name of each unit, then categorizes them in respective list
			if (type.equals("TownHall"))
				townHallIds.add(id);
			else if (type.equals("Peasant"))
				peasantIds.add(id);
    	}
    	List<Integer> resourceIds = state.state.getAllResourceIds();  //gets all resources
    	
    	//gets ids of all goldmines and trees in respective list
    	for(int id : resourceIds)
    	{
			if(state.state.getResourceNode(id).getType() == ResourceNode.Type.GOLD_MINE)
				goldmines.add(id);
			else if(state.state.getResourceNode(id).getType() == ResourceNode.Type.TREE)
				tree.add(id);   			
    	}
    	
    	
    }

    /**
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
    public boolean isGoal() {
        return (currentGold == requiredGold) && (currentWood == requiredWood); //checks to see if gold and wood requirement fulfilled
    }

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    //for every peasant, checks each resource and action for possible children
    // can def be more efficient/cleaner gonna play around with it more
    public List<GameState> generateChildren() {
    	List<GameState> children = new LinkedList<GameState>();
    	for(int peasant : peasantIds) {
    		for(int i : goldmines) {
        		Position location = new Position(state.getUnit(goldmines.get(i)).getXPosition(),state.getUnit(goldmines.get(i)).getYPosition());
            	moveTo movingAgent = new moveTo(agent,location, peasant, i, state2);  
            	if(movingAgent.preconditionsMet(state2))	//checks to see if agent can move to desired location
            		children.add(movingAgent.apply(state2)); 	// if passes precondition, gets new gamestate with agent at that location
            	harvest harvestgold = new harvest(agent,location, peasant, i, state2);
            	if(harvestgold.preconditionsMet(state2))
            		children.add(harvestgold.apply(state2));
            	deposit depositgold = new deposit(agent,location, peasant, i, state2);
            	if(depositgold.preconditionsMet(state2))
            		children.add(state2);
        	}
    		for(int i : tree) {
        		Position location = new Position(state.getUnit(tree.get(i)).getXPosition(),state.getUnit(tree.get(i)).getYPosition());
            	moveTo movingAgent = new moveTo(agent,location, peasant, i, state2);  //Position agent, Position location, int agId, int locId, GameState state)
            	if(movingAgent.preconditionsMet(state2))	//checks to see if agent can move to desired location
            		children.add(movingAgent.apply(state2)); 	// if passes precondition, gets new gamestate with agent at that location
            	harvest harvestwood = new harvest(agent,location, peasant, i, state2);
            	if(harvestwood.preconditionsMet(state2))
            		children.add(harvestwood.apply(state2));
            	deposit depositwood = new deposit(agent,location, peasant, i, state2);
            	if(depositwood.preconditionsMet(state2))
            		children.add(state2);
        	}
    	}
        return children;
    }

    /**
     * Write your heuristic function here. Remember this must be admissible for the properties of A* to hold. If you
     * can come up with an easy way of computing a consistent heuristic that is even better, but not strictly necessary.
     *
     * Add a description here in your submission explaining your heuristic.
     *
     * @return The value estimated remaining cost to reach a goal state from this state.
     */
    public double heuristic() { //time it takes to gather all of our supplies if we could teleport? + euclidean distance
        // TODO: Implement me!
        return 0.0;
    }

    /**
     *
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost() {
        // TODO: Implement me!
        return cost;
    }

    /**
     * This is necessary to use your state in the Java priority queue. See the official priority queue and Comparable
     * interface documentation to learn how this function should work.
     *
     * @param o The other game state to compare
     * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
     */
    @Override
    public int compareTo(GameState o) {
        if (o.getCost() > getCost())
        	return -1;
        if (o.getCost() < getCost())
        	return 1;
        return 0;
    }

    /**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        // TODO: Implement me!
        return false;
    }

    /**
     * This is necessary to use the GameState as a key in a HashSet or HashMap. Remember that if two objects are
     * equal they should hash to the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode() {
        return (int)(31*(currentGold + currentWood + goldheld + woodheld + cost));
    }
}
