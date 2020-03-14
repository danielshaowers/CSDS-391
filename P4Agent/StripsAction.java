package P4Agent;

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
    
    //returns the StripsAction the lead to the new gamestate
    public StripsAction getCameFrom();
}

//Strips action classes that implement interface
class moveTo implements StripsAction{
	Position agentpos;
	Position locationpos;
	int agentId;
	int locationId;
	StripsAction camefrom;
	
	
	//Initializes agents position and location the agent wants go to 
	public moveTo(Position agent, Position location, int agId, int locId, GameState state)
	{
		agentpos = agent;
		locationpos = location;
		this.agentId = agId;
		locationId = locId;
		camefrom = state.camefrom;
	}
	@Override
	//determines if agent position is equal to location position
	public boolean preconditionsMet(GameState state) {
		if(!agentpos.equals(locationpos))
			return true;
		return false;
	}
	//do we need to check if the move is a valid location?
	@Override
	public GameState apply(GameState state) {
		Position agentposnew = agentpos.move(agentpos.getDirection(locationpos));
		return new GameState(agentposnew, state.currentGold, state.currentWood, state.goldheld,state.woodheld, 
				new moveTo(agentpos,locationpos,agentId,locationId, state), state);
	}
	//returns cost to get from agent position to desired location
	public double getCost(){
		return agentpos.euclideanDistance(locationpos);
	}
	
	@Override
	//returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}
	
	public String toString() {
		return "Agent " + agentId + ": MOVE(" + locationpos.x + ", " + locationpos.y + ")";
	}
}

class harvest implements StripsAction{
	Position agentpos;
	Position locationpos;
	int agentId;
	int locationId;
	StripsAction camefrom;

	//Initializes agents position and location the agent wants go to 
	public harvest(Position agent, Position location, int agId, int locId, GameState state)
	{
		this.agentpos = agent;
		this.locationpos = location;
		this.agentId = agId;
		locationId = locId;
		camefrom = state.camefrom;
	}
	@Override
	public boolean preconditionsMet(GameState state) {
		return (!state.isGoal() && agentpos.isAdjacent(locationpos)); //makes sure state isn't goal state and that the agent is next to harvest location
	}
	@Override
	public GameState apply(GameState state) {
		Action gather = Action.createPrimitiveGather(agentId, agentpos.getDirection(locationpos)); //
		if(location(state)) { //determine where it collected the resource from
			
			int gathered = state.state.getUnit(gather.getUnitId()).getTemplateView().getGoldCost(); //how much gold collected after action is executed 
			//returns new GameState with updated class variables and stripsAction that led to this GameState
			return new GameState(agentpos, state.currentGold, state.currentWood, gathered,state.woodheld, 
					new harvest(agentpos,locationpos,agentId,locationId,state), state);
		}
		else {
			int gathered = state.state.getUnit(gather.getUnitId()).getTemplateView().getWoodCost(); //how much wood collected after action is executed 
			//returns new GameState with updated class variables and stripsAction that led to this GameState
			return new GameState(agentpos, state.currentGold, state.currentWood,state.goldheld, gathered, 
					new harvest(agentpos,locationpos,agentId,locationId,state), state);
		}	
	}
	//determines if its a goldmine or a tree   might try and condense this with above
	public boolean location(GameState state){ 
		for(int loc : state.goldmines)
		{
			if(loc == locationId)
				return true;
		}
		return false;
	}
	@Override
	//returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}
	@Override 
	public String toString() {
		return "Agent " + agentId + " HARVESTING( " + locationpos.x + ", " + locationpos.y + ")";
	}
	
}


class deposit implements StripsAction{
	Position agentpos;
	Position locationpos;
	int agentId;
	int locationId;
	StripsAction camefrom;
	//Initializes agents position and location the agent wants go to 
	public deposit(Position agent, Position location, int agId, int locId, GameState state)
	{
		this.agentpos = agent;
		this.locationpos = location;
		this.agentId = agId;
		this.locationId = locId;
		camefrom = state.camefrom;
	}
	@Override
	public boolean preconditionsMet(GameState state) {
		return (state.isGoal() && agentpos.isAdjacent(locationpos));
	}

	@Override
	public GameState apply(GameState state) {
		Action deposit = Action.createPrimitiveGather(agentId, agentpos.getDirection(locationpos));
		int deposited = state.state.getUnit(deposit.getUnitId()).getTemplateView().getGoldCost(); //how much resource was deposited 
		if(deposited ==0)  //if its zero then no gold was deposited
			return new GameState(agentpos, state.currentGold, deposited,state.goldheld, state.woodheld, 
					new harvest(agentpos,locationpos,agentId,locationId,state), state);				// returns state that has deposited wood
		return new GameState(agentpos, deposited, state.currentWood,state.goldheld, state.woodheld, 
				new harvest(agentpos,locationpos,agentId,locationId,state), state);					// returns state that has deposited gold
	}
	@Override
	//returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}
	
	@Override
	public String toString() {
		return "Agent " + agentId + " DEPOSIT(" + locationpos.x + ", " + locationpos.y + ")";
	}
	
}


