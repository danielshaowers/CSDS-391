package P4Agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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
	Daniel peasant; 
	HashMap<Integer, Nacho> resources = new HashMap<Integer, Nacho>();
	StateView state;
	GameState state2; //what is this
	int requiredGold;
	int requiredWood;
	int currentGold = 0;
	int currentWood = 0;
	double cost;
	boolean buildPeasants;
	Position agent; //what is this? why only one
	List<Integer> goldmines = null;
	List<Integer> tree = null;
	int townHallId;
	int peasantId;
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
    	Integer[] playerNums = state.getPlayerNumbers();
		List<Integer> unitIDs = state.getUnitIds(playerNums[0]);
		//gets townhall and pesant id
    	for(int id : unitIDs){
    		UnitView unit = state.getUnit(id);
			String type = unit.getTemplateView().getName(); //obtains the name of each unit, then categorizes them in respective list
			if (type.equals("TownHall"))
				townHallId = id;
			else if (type.equals("Peasant")) {
				int woodheld = 0; 
				int goldheld = 0;
				peasantId = id;
				if (unit.getCargoAmount() > 0) {
					if (unit.getCargoType().compareTo(ResourceType.GOLD) == 0)
						woodheld = unit.getCargoAmount();
					else
						goldheld = unit.getCargoAmount();
				}
				peasant = new Daniel(id, new Position(unit.getXPosition(), unit.getYPosition()), goldheld, woodheld, 0);
			}
    	}
    	//NOTE I CHANGED THIS PLAYERNUM FROM TOWNHALLID
    	this.currentGold = state.getResourceAmount(playernum, ResourceType.GOLD);
    	this.currentWood = state.getResourceAmount(playernum, ResourceType.WOOD);
    	List<Integer> resourceIds = state.getAllResourceIds();  //gets all resources    	
    	//gets ids of all goldmines and trees in respective list
    	for(int id : resourceIds)
    	{
    		ResourceView resource = state.getResourceNode(id);
			if(resource.getType().equals(ResourceNode.Type.GOLD_MINE)) {
    			goldmines.add(id);    			
    			resources.put(id, new Nacho(resource.getXPosition(), resource.getYPosition(), resource.getAmountRemaining(), id, true));
    		}
			else if(resource.getType().equals(ResourceNode.Type.TREE)) {
				tree.add(id);
				resources.put(id, new Nacho(resource.getXPosition(), resource.getYPosition(), resource.getAmountRemaining(), id, false));
			}
    	}	
    }
    public GameState(Daniel peasant, HashMap<Integer, Nacho> resource, int golddepoist, int wooddeposit, int cost, StripsAction action, GameState state){
    	//initialzes everything
    	this.goldmines = state.goldmines;
    	this.resources = resource;
    	this.tree = state.tree;
    	this.peasant = peasant; //may change from previous gamestate
    	this.requiredGold = state.requiredGold;
    	this.requiredWood = state.requiredWood;
    	this.currentGold = state.currentGold + golddepoist; //may change from previous state
    	this.currentWood = state.currentWood + wooddeposit; //may change from previous state
    	this.camefrom = action; //definitely changes from previous state
		this.state2 = state; //the gamestate i don't know why it was named so ambiguously
		this.peasantId = peasant.getId();
		this.townHallId = state.townHallId;
    }
    
    public HashMap<Integer, Nacho> duplicateResourceMap(){
    	HashMap<Integer, Nacho> next = new HashMap<Integer, Nacho>();
    	for (Integer i : state.getAllResourceIds()) {
    		next.put(i, resources.get(i).makeCopy());
    	}
    	return next;
    }

    /**
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
    public boolean isGoal() {
        return (currentGold >= requiredGold) && (currentWood >= requiredWood); //checks to see if gold and wood requirement fulfilled
    }
    //might not work. check with the below to see if conceptually the same
    public ArrayList<ArrayList<StripsAction>> permute(List<List<StripsAction>> inputs, ArrayList<ArrayList<StripsAction>> outputs, ArrayList<StripsAction> current, int listNum){
    	if (listNum > inputs.size()) {
    		outputs.add(current);
    		return null;
    	}
    	for (StripsAction s : inputs.get(listNum)) {
    		ArrayList<StripsAction> branch = new ArrayList<StripsAction>(current);
    		branch.add(s);
    		permute(inputs, outputs, branch, listNum + 1);
    	}
    	return outputs;
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
    	List<StripsAction> allActions = new ArrayList<StripsAction>();
    	for(int i : goldmines) {
    		if (resources.get(i) != null) {
    			Position location = new Position(state.getUnit(goldmines.get(i)).getXPosition(),state.getUnit(goldmines.get(i)).getYPosition());
    			moveTo movingAgent = new moveTo(peasant, location, i, state2);  
    			if(movingAgent.preconditionsMet(state2)) {	//checks to see if agent can move to desired location
    				allActions.add(movingAgent);           		
    			}// if passes precondition, gets new gamestate with agent at that location
    			harvest harvestgold = new harvest(peasant,location, i, state2);
    			if(harvestgold.preconditionsMet(state2))
    				allActions.add(harvestgold);
    			}
        	}
    		for(int i : tree) {
    			if (resources.get(i) != null) {
    				Position location = new Position(state.getUnit(tree.get(i)).getXPosition(),state.getUnit(tree.get(i)).getYPosition());
    				moveTo movingAgent = new moveTo(peasant, location, i, state2);  
    				if(movingAgent.preconditionsMet(state2)) {	//checks to see if agent can move to desired location
    					allActions.add(movingAgent);           		
    				}// if passes precondition, gets new gamestate with agent at that location
    				harvest harvestwood = new harvest(peasant,location, i, state2);
    				if(harvestwood.preconditionsMet(state2))
    					allActions.add(harvestwood);
            	//I can make all these precondition checks and additions into a single method.
    			}
        	}    		
    			Position location = new Position(state.getUnit(townHallId).getXPosition(),state.getUnit(townHallId).getYPosition());
        		moveTo movingAgent = new moveTo(peasant, location, townHallId, state2);  
            	if(movingAgent.preconditionsMet(state2)) {	//checks to see if agent can move to desired location
            		allActions.add(movingAgent);           		
            	}// if passes precondition, gets new gamestate with agent at that location
            	deposit depositwood = new deposit(peasant,location, townHallId, state2);
            	if(depositwood.preconditionsMet(state2))
            		allActions.add(depositwood);
             	deposit depositgold = new deposit(peasant,location, townHallId, state2);
            	if(depositgold.preconditionsMet(state2))
            		allActions.add(depositgold);
            	buildPeasants babymaker = new buildPeasants(peasant, location, townHallId, state2); 
            	if (babymaker.preconditionsMet(state2))
            		allActions.add(babymaker);
    	//generatePermutations(allActions, new ArrayList<ArrayList<StripsAction>>(), 0, null);
      /*  ArrayList<ArrayList<StripsAction>> actionCombinations = permute(allActions, 
        		new ArrayList<ArrayList<StripsAction>>(), new ArrayList<StripsAction>(), 0); 
        for (ArrayList<StripsAction> nextState : actionCombinations) {
        	GameState next = this;
        	for (int i = 0; i < nextState.size(); i++) {
        		next = nextState.get(i).apply(next); //continuously builds on the gamestate
        	} */
         for (StripsAction a : allActions)	
        	 children.add(a.apply(this));
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
    public double heuristic() { //time it takes to walk to resource and back if gathering took no time
        int remainingGoldCost = requiredGold - currentGold; //time it takes to harvest 
        int remainingWoodCost = requiredWood - currentWood; //time it takes to harvest that much wood
        return remainingGoldCost + remainingWoodCost +
        		peasant.getPosition().euclideanDistance(new Position(state.getUnit(townHallId).getXPosition(), state.getUnit(townHallId).getYPosition()));
    }

    /**
     *
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost() {
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
    public int compareTo(GameState o) { //doesn't that mean priorityqueue is a minheap
        if (o.getCost() + o.heuristic() > getCost() + heuristic())
        	return -1;
        if (o.getCost() + o.heuristic() < getCost() + heuristic())
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
        	if (o instanceof GameState) {
        		GameState bruh = (GameState) o;
        		return hashCode() == bruh.hashCode();
        	}
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
        return (int)(31*(currentGold + currentWood + peasant.hashCode() + cost));
    }
}
