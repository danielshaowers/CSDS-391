package P4Agent;

import java.util.HashMap;
import java.util.List;

import HW2.src.edu.cwru.sepia.agent.AStarAgent;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.util.Direction;

/**
 * A useful start of an interface representing strips actions. You may add new methods to this interface if needed, but
 * you should implement the ones provided. You may also find it useful to specify a method that returns the effects
 * of a StripsAction.
 */
public interface StripsAction {

    /**
     * Returns true if the provided GameState meets all of the necessary conditions for this action to successfully
     * execute.
     *
     * As an example consider a Move action that moves peasant 1 in the NORTH direction. The partial game state might
     * specify that peasant 1 is at location (3, 3). In this case the game state shows that nothing is at location (3, 2)
     * and (3, 2) is within bounds. So the method returns true.
     *
     * If the peasant were at (3, 0) this method would return false because the peasant cannot move to (3, -1).
     *
     * @param state GameState to check if action is applicable
     * @return true if apply can be called, false otherwise
     */
    public boolean preconditionsMet(GameState state);

    /**
     * Applies the action instance to the given GameState producing a new GameState in the process.
     *
     * As an example consider a Move action that moves peasant 1 in the NORTH direction. The partial game state
     * might specify that peasant 1 is at location (3, 3). The returned GameState should specify
     * peasant 1 at location (3, 2).
     *
     * In the process of updating the peasant state you should also update the GameState's cost and parent pointers.
     *
     * @param state State to apply action to
     * @return State resulting from successful action appliction.
     */
    public GameState apply(GameState state);
    
    //returns the StripsAction the lead to the new gamestate
    public StripsAction getCameFrom();
    
    public Action toSepiaAction();
}

//Strips action classes that implement interface
class moveTo implements StripsAction{
	Peasant peasant;
	Position locationpos;
	int locationId;
	StripsAction camefrom;
	int cost;
	double minDist = Double.POSITIVE_INFINITY;
	
	
	//Initializes agents position and location the agent wants go to 
	public moveTo(Peasant agent, Position location, int locId, GameState state)
	{
		peasant = agent;
		locationpos = location;
		locationId = locId;
		camefrom = state.camefrom;
		double tempDist = 0;
		for (Position adj : locationpos.getAdjacentPositions()) {
			tempDist = peasant.getPosition().euclideanDistance(adj);
			if (minDist > tempDist) {
				minDist = tempDist;
				locationpos = adj;
			}
		}
	}
	@Override
	//determines if agent position is not equal to location position
	public boolean preconditionsMet(GameState state) {
		return !state.isGoal() && !peasant.getPosition().equals(locationpos);
	}
	@Override
	public GameState apply(GameState state) {		
		peasant = peasant.makeCopy();
		int cost = (int)(state.getCost() + minDist);
		peasant.setPosition(locationpos);
		return new GameState(peasant, state.resources, 0, 0, cost, this, state);
	}

	@Override
	//returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}
	
	public String toString() {
		return "Agent " + peasant.getId() + ": MOVE(" + locationpos.x + ", " + locationpos.y + ")";
	}
	
	//converts STRIPS action to sepia action
	public Action toSepiaAction() {
		return Action.createCompoundMove(peasant.getId(), locationpos.x, locationpos.y);
	}
}

class deposit implements StripsAction{
	Peasant peasant;
	Position locationpos;
	int locationId;
	StripsAction camefrom;
	//Initializes agents position and location the agent wants go to 
	public deposit(Peasant agent, Position location, int locId, GameState state)
	{
		peasant = agent;
		locationpos = location;
		locationId = locId;
		camefrom = state.camefrom;
	}
	@Override
	public boolean preconditionsMet(GameState state) {
		return !state.isGoal() && peasant.getPosition().isAdjacent(locationpos) && peasant.hasResource();	
	}

	@Override
	public GameState apply(GameState state) {
		peasant = peasant.makeCopy();
		int woodDeposit = peasant.getWood();
		int goldDeposit = peasant.getGold();
		peasant.setWood(0);
		peasant.setGold(0);
		return new GameState(peasant, state.resources, woodDeposit, goldDeposit, (int)state.cost+1, this,state); //previously + 25
		//is this cost correct?
	}
	@Override
	//returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}
	
	@Override
	public String toString() {
		return "Agent " + peasant.getId() + " DEPOSIT(" + locationpos.x + ", " + locationpos.y + ")";
	}
	
	@Override
	public Action toSepiaAction() {
		return Action.createPrimitiveDeposit(peasant.getId(), peasant.getPosition().getDirection(locationpos));
	}
}
class harvest implements StripsAction{
	Peasant peasant;
	HashMap<Integer, Resource> resources;
	Position locationpos;
	int locationId;
	StripsAction camefrom;


	//Initializes agents position and location the agent wants go to 
	public harvest(Peasant agent, Position location, int locId, GameState state)
	{
		peasant = agent;
		resources = state.duplicateResourceMap();
		locationpos = location;
		locationId = locId;
		camefrom = state.camefrom;
	}
	@Override
	public boolean preconditionsMet(GameState state) {
		return !state.isGoal() && peasant.getPosition().isAdjacent(locationpos) && !peasant.hasResource(); //makes sure state isn't goal state and that the agent is next to harvest location
	}
	@Override
	public GameState apply(GameState state) { 
		peasant = peasant.makeCopy();
		Resource resource = resources.get(locationId);
		int amount = Math.min(100, resource.remaining);
		int duration;
		if(resource.isGold) {  //determine where it collected the resource from				
			peasant.setGold(amount);
			duration = 200;
		}
		else {
			peasant.setWood(amount);
			duration = 1000;
		}
		duration = 1;
		if ((resource.remaining -= amount) <= 0)
			resources.remove(locationId);
		return new GameState(peasant, resources, 0, 0, (int)state.cost+duration, this,state);
			//returns new GameState with updated class variables and stripsAction that led to this GameState	
	}

	@Override
	//returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}
	@Override 
	public String toString() {
		return "Agent " + peasant.getId() + " HARVESTING( " + locationpos.x + ", " + locationpos.y + ")";
	}
	public Action toSepiaAction() {
		return Action.createPrimitiveGather(peasant.getId(), peasant.getPosition().getDirection(locationpos));
	}
	
}


