package EECS391_sepia;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.State;

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
}

//Strips action classes that implement interface
class moveTo implements StripsAction{
	Position agentpos;
	Position locationpos;
	int agentId;
	
	//Initializes agents position and location the agent wants go to 
	public moveTo(Position agent, Position location, int id)
	{
		agentpos = agent;
		locationpos = location;
		this.agentId = id;
	}
	@Override
	//determines if agent position is equal to location position
	public boolean preconditionsMet(GameState state) {
		if(!agentpos.equals(locationpos))
			return true;
		return false;
	}
	@Override
	public GameState apply(GameState state) {
		agentpos = agentpos.move(agentpos.getDirection(locationpos));
		return new GameState(agentpos, state);
	}
	//returns cost to get from agent position to desired location
	public double getCost(){
		return agentpos.euclideanDistance(locationpos);
	}
	public Action getStripsAction()
	{
		return Action.createPrimitiveMove(agentId, agentpos.getDirection(locationpos));	
	}
}
class harvest implements StripsAction{
	Position agentpos;
	Position locationpos;
	int agentId;
	//Initializes agents position and location the agent wants go to 
	public harvest(Position agent, Position location, int id)
	{
		this.agentpos = agent;
		this.locationpos = location;
		this.agentId = id;
	}
	@Override
	public boolean preconditionsMet(GameState state) {
		return (!state.isGoal() && agentpos.isAdjacent(locationpos));
	}
	@Override
	public GameState apply(GameState state) {
		Action gather = Action.createPrimitiveGather(agentId, agentpos.getDirection(locationpos));
		int goldgathered = state.state.getUnit(gather.getUnitId()).getTemplateView().getGoldCost();
		return new GameState(agentpos, goldgathered, state);
 )
		return null;
	}
	public Action getStripsAction()
	{
		return Action.createPrimitiveGather(agentId, agentpos.getDirection(locationpos));
	}
	
}
class deposit implements StripsAction{
	Position agentpos;
	Position locationpos;
	int agentId;
	//Initializes agents position and location the agent wants go to 
	public deposit(Position agent, Position location, int id)
	{
		this.agentpos = agent;
		this.locationpos = location;
		this.agentId = id;
	}
	@Override
	public boolean preconditionsMet(GameState state) {
		return (state.isGoal() && agentpos.isAdjacent(locationpos));
	}

	@Override
	public GameState apply(GameState state) {
		Action deposit = Action.createPrimitiveGather(agentId, agentpos.getDirection(locationpos));
		return null;
	}
	public Action getStripsAction()
	{
		return Action.createPrimitiveGather(agentId, agentpos.getDirection(locationpos));
	}
	
}


