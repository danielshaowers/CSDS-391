package PEAsants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import HW2.src.edu.cwru.sepia.agent.AStarAgent;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;

/**
 * A useful start of an interface representing strips actions. You may add new
 * methods to this interface if needed, but you should implement the ones
 * provided. You may also find it useful to specify a method that returns the
 * effects of a StripsAction.
 */
public interface StripsAction {

	/**
	 * Returns true if the provided GameState meets all of the necessary conditions
	 * for this action to successfully execute.
	 *
	 * As an example consider a Move action that moves peasant 1 in the NORTH
	 * direction. The partial game state might specify that peasant 1 is at location
	 * (3, 3). In this case the game state shows that nothing is at location (3, 2)
	 * and (3, 2) is within bounds. So the method returns true.
	 *
	 * If the peasant were at (3, 0) this method would return false because the
	 * peasant cannot move to (3, -1).
	 *
	 * @param state GameState to check if action is applicable
	 * @return true if apply can be called, false otherwise
	 */
	public boolean preconditionsMet();
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action); 
	public int getLocationID();
	public Position getLocationPos();
	/**
	 * Applies the action instance to the given GameState producing a new GameState
	 * in the process.
	 *
	 * As an example consider a Move action that moves peasant 1 in the NORTH
	 * direction. The partial game state might specify that peasant 1 is at location
	 * (3, 3). The returned GameState should specify peasant 1 at location (3, 2).
	 *
	 * In the process of updating the peasant state you should also update the
	 * GameState's cost and parent pointers.
	 *
	 * @param state State to apply action to
	 * @return State resulting from successful action appliction.
	 */
	public GameState apply(GameState state);

	// returns the StripsAction the lead to the new gamestate
	public StripsAction getCameFrom();

	public HashMap<Integer, Action> toSepiaAction(int[] stateId);
	
	public int[] getActorIds();
}

//Strips action classes that implement interface
class moveTo implements StripsAction {
	GameState gs;
	GameMap next;
	int[] agentId;
	Peasant peasant;
	Position locationpos;
	int locationId;
	StripsAction camefrom;
	int cost;
	double minDist = Double.POSITIVE_INFINITY;
	
	@Override
	public int getLocationID() {
		return locationId;
	}
	
	@Override 
	public Position getLocationPos() {
		return locationpos;
	}
	
	// Initializes agents position and location the agent wants go to
	public moveTo(int[] agentID, Position location, int locId, GameState state) {
		this.gs = state;
		agentId = agentID;
		next = state.game;
		if (agentId.length == 1) {
		peasant = next.peasants.get(agentId[0]);
		locationpos = location;
		locationId = locId;
		camefrom = state.camefrom;
		double tempDist = 0;
		// technically I don't think I should do this
		for (Position adj : locationpos.getAdjacentPositions()) {
			tempDist = peasant.getPosition().chebyshevDistance(adj);
			if (minDist > tempDist) {
				minDist = tempDist;
				locationpos = adj;
			}
		}
		}
	}
	@Override
	// determines if agent position is not equal to location position
	public boolean preconditionsMet() {
		return agentId.length == 1 && !gs.isGoal() && !peasant.getPosition().equals(locationpos) && peasant.getTurns() < 5;//5; //I really am not sure about this turn thing																										// available
	}
	
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action) {
		GameState gs = new GameState(state, playernum, requiredGold, requiredWood, buildPeasants);
		moveTo checker = new moveTo(action.getActorIds(), action.getLocationPos(), action.getLocationID(), gs);
		return checker.preconditionsMet();
	}
	@Override
	public GameState apply(GameState state) {
		GameMap next = state.game.copyAll();
		peasant = next.peasants.get(peasant.getId());
		int cost = (int) (state.getCost() + minDist);
		peasant.setPosition(locationpos); // move the peasant to our target location
		int[] actor = { agentId[0] };
		next.updateTurns(actor, (int)minDist); // updates the turn number of all members
		GameState gs = new GameState(next, cost, this, state);
		//System.out.println("new move to action considered with f value " + (gs.cost + gs.heuristic()));
		return new GameState(next, cost, this, state);
	}

	@Override
	// returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}

	public String toString() {
		return "Agent " + peasant.getId() + ": MOVE(" + locationpos.x + ", " + locationpos.y + ")";
	}

	// converts STRIPS action to sepia action
	public HashMap<Integer, Action> toSepiaAction(int[] stateId) {
		HashMap<Integer, Action> action = new HashMap<Integer, Action>();
		action.put(stateId[0], Action.createCompoundMove(stateId[0], locationpos.x, locationpos.y));
		return action;
	}
	@Override
	public int[] getActorIds() {
		return new int[]{agentId[0]};
	}
}

class moveTo2 implements StripsAction {
	GameMap next;
	GameState gs;
	Peasant[] peasant = new Peasant[2];
	int numAgents;
	Position locationpos;
	Position[] destination = new Position[2];
	int locationId;
	StripsAction camefrom;
	int cost;
	double[] minDist = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY} ;
	int[] agentId;

	public moveTo2(int[] agentID, Position location, int locId, GameState state) {
		gs=state;
		this.agentId = agentID;
		if ((numAgents = agentID.length) == 2) {
			next = state.game;
			for (int i = 0; i < agentID.length; i++) {
				peasant[i] = next.peasants.get(agentID[i]);
			}
			locationpos = location;
			locationId = locId;
			camefrom = state.camefrom;
			// technically I don't think I should do this
			for (int i = 0; i < agentID.length; i++) { //find the nearest for each unit and make sure no repeats
				for (Position adj : locationpos.getAdjacentPositions()) {
					double tempDist;
					for (int j = i; j >= 0; j--) {
						if (!adj.equals(destination[j])) { 
							tempDist = peasant[i].getPosition().chebyshevDistance(adj); //find the smallest adjacent dist
							if (minDist[i] > tempDist) {
								minDist[i] = tempDist;
								destination[i] = adj;
							}
						}
					}
				}
			}
		}
	}
	@Override
	public int getLocationID() {
		return locationId;
	}
	
	@Override 
	public Position getLocationPos() {
		return locationpos;
	}
	@Override
	public int[] getActorIds() {
		return agentId;
	}
	@Override
	// determines if agent position is not equal to location position
	public boolean preconditionsMet() {
		return !gs.isGoal() && numAgents == 2 && !peasant[0].getPosition().equals(destination[0])
				&& peasant[0].getTurns() < 5//5 //DANIEL CHANGED FROM 3 // peasant is available
				&& !peasant[1].getPosition().equals(destination[1]) && peasant[1].getTurns() < 5;// 5;
	}
	@Override
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action) {
		GameState gs = new GameState(state, playernum, requiredGold, requiredWood, buildPeasants);
		moveTo2 checker = new moveTo2(action.getActorIds(), action.getLocationPos(), action.getLocationID(), gs);
		return checker.preconditionsMet();
	}	

	@Override
	public GameState apply(GameState state) {
		GameMap next = state.game.copyAll();
		peasant[0] = next.peasants.get(peasant[0].getId());
		peasant[1] = next.peasants.get(peasant[1].getId());
		double maxDist = Math.min(minDist[0], minDist[1]);
		int cost = (int) (state.getCost() + maxDist);
		peasant[0].setPosition(destination[0]); // move the peasant to our target location
		peasant[1].setPosition(destination[1]);
		int[] actor = { peasant[0].getId(), peasant[1].getId() };
		next.updateTurns(actor, (int)maxDist); // updates the turn number of all members
		return new GameState(next, cost, this, state);
	}

	@Override
	// returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}

	public String toString() {
		return "Agents " + peasant[0].getId() + ", " + peasant[1].getId() + ": MOVE(" + destination[0].x + ", "
			+ destination[0].y + ")";
	}

	// converts STRIPS action to sepia action
	public HashMap<Integer, Action> toSepiaAction(int[] stateId) {
		HashMap<Integer, Action> actions = new HashMap<Integer, Action>();
		actions.put(stateId[0], Action.createCompoundMove(stateId[0], destination[0].x, destination[1].y));
		actions.put(stateId[1], Action.createCompoundMove(stateId[1], destination[1].x, destination[1].y));
		return actions;
	}
}

class moveTo3 implements StripsAction {
	int numAgents;
	GameMap next;
	GameState gs;
	Peasant[] peasant = new Peasant[3];
	Position locationpos;
	Position[] destination = new Position[3];
	int locationId;
	int[] agentId;
	StripsAction camefrom;
	int cost;
	double[] minDist = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
	
	public moveTo3(int[] agentID, Position location, int locId, GameState state) {
		
		agentId = agentID;
		gs = state;
		next = state.game.copyAll();
		if ((numAgents = agentID.length) == 3) {
			for (int i = 0; i < agentID.length; i++) {
				peasant[i] = next.peasants.get(agentID[i]);
			}
			locationpos = location;
			locationId = locId;
			camefrom = state.camefrom;
			double tempDist;
			// technically I don't think I should do this
			for (int i = 0; i < agentID.length; i++) { //find the nearest for each unit and make sure no repeats
				for (Position adj : locationpos.getAdjacentPositions()) {
					for (int j = i; j >= 0; j--) {
						if (!adj.equals(destination[j])) { 
							tempDist = peasant[i].getPosition().chebyshevDistance(adj); //find the smallest adjacent dist
							if (minDist[i] > tempDist) {
								minDist[i] = tempDist;
								destination[i] = adj;
							}
						}
					}
				}
			}
		}
	}
	@Override
	public int getLocationID() {
		return locationId;
	}
	
	@Override 
	public Position getLocationPos() {
		return locationpos;
	}
	
	@Override
	public int[] getActorIds() {
		return agentId;
	}
	
	@Override
	// determines if agent position is not equal to location position
	public boolean preconditionsMet() {
		return !gs.isGoal() && numAgents == 3 && !peasant[0].getPosition().equals(destination[0])
			//	&& peasant[0].getTurns() < 5 // peasant is available
			//	&& !peasant[1].getPosition().equals(destination[1]) && peasant[1].getTurns() < 5
			//	&& !peasant[2].getPosition().equals(destination[0]) && peasant[2].getTurns() < 5;
					&& peasant[0].getTurns() < 5 // peasant is available
					&& !peasant[1].getPosition().equals(destination[1]) && peasant[1].getTurns() < 5
					&& !peasant[2].getPosition().equals(destination[0]) && peasant[2].getTurns() < 5;
	}
	@Override
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action) {
		GameState gs = new GameState(state, playernum, requiredGold, requiredWood, buildPeasants);
		moveTo3 checker = new moveTo3(action.getActorIds(), action.getLocationPos(), action.getLocationID(), gs);
		return checker.preconditionsMet();
	}
	
	@Override
	public GameState apply(GameState state) {
		GameMap next = state.game.copyAll();
		int[] actor = { peasant[0].getId(), peasant[1].getId(), peasant[2].getId() };
		for (int i = 0; i < peasant.length; i++) {
			peasant[i] = next.peasants.get(peasant[i].getId()); // kind of weird way to do it but it works
			peasant[i].setPosition(destination[i]);
		}
		double maxDist = Math.min(Math.min(minDist[0], minDist[1]), minDist[2]);
		int cost = (int) (state.getCost() + maxDist);
		next.updateTurns(actor, (int)maxDist); // updates the turn number of all members
		return new GameState(next, cost, this, state);
	}

	@Override
	// returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}

	public String toString() {
		return "Agents " + peasant[0].getId() + ", " + peasant[1].getId() + ", " + peasant[2].getId() + "MOVE(" + destination[0].x + ", "
				+ destination[0].y + ")";
	}

	// converts STRIPS action to sepia action
	public HashMap<Integer, Action> toSepiaAction(int[] stateId) {
		HashMap<Integer, Action> actions = new HashMap<Integer, Action>();
		actions.put(stateId[0], Action.createCompoundMove(stateId[0], destination[0].x, destination[0].y));
		actions.put(stateId[1], Action.createCompoundMove(stateId[1], destination[1].x, destination[1].y));
		actions.put(stateId[2], Action.createCompoundMove(stateId[2], destination[2].x, destination[2].y));
		return actions;
	}
}

class buildPeasant implements StripsAction {
	GameMap next;
	// Peasant baby = new Peasant();
	Position townHall; // Daniel: might have to add the townhall to our gamemap
	int locationId;
	StripsAction camefrom;
	GameState state;

	public buildPeasant(GameState state, Position th, int thid) {
		this.state = state;
		camefrom = state.camefrom;
		next = state.game;
		townHall = th;
		locationId = thid;
	}
	@Override
	public int getLocationID() {
		return locationId;
	}
	
	@Override 
	public Position getLocationPos() {
		return townHall;
	}
	@Override
	public int[] getActorIds() {
		return new int[] {locationId};
	}
	@Override
	public boolean preconditionsMet() { // do I need to account for whether the townhall is available?
		return next.currentFood > 0 && next.currentGold >= 400;

	}
	@Override
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action) {
		GameState gs = new GameState(state, playernum, requiredGold, requiredWood, buildPeasants);
		UnitView townhall = state.getUnit(0);
		for (int unitId : state.getUnitIds(playernum)) {
			Unit.UnitView unit = state.getUnit(unitId);
			String unitType = unit.getTemplateView().getName().toLowerCase();
			if (unitType.equals("townhall")) {
				locationId = unitId;
				townhall = unit;
				break;
			}
			townHall = new Position(townhall.getXPosition(), townhall.getYPosition());
		}
		buildPeasant checker = new buildPeasant(gs, townHall, locationId);
		return checker.preconditionsMet();
	}

	@Override
	public GameState apply(GameState state) {
		next = state.game.copyAll();
		Peasant baby = new Peasant(next.peasants.size() + 1, new Position(townHall.x + 1, townHall.y + 1), 0, 0, 0); 																																																					// bad
		next.peasants.put(baby.getId(), baby);
		next.currentGold -= 400;
		next.currentFood--;
		GameState nextState = new GameState(next, state.cost+1, this, state);
		System.out.println("child being created with f value " + nextState.heuristic());
		return new GameState(next, state.cost+1, this, state);
	}

	@Override
	public StripsAction getCameFrom() {
		return camefrom;
	}

	@Override
	public HashMap<Integer, Action> toSepiaAction(int[] useless) {
		HashMap<Integer, Action> action = new HashMap<Integer, Action>();
		int build = state.state.getTemplate(state.playerNum, "Peasant").getID();
		action.put(locationId, Action.createCompoundProduction(locationId, build));
		return action;
	}
	
	public String toString() {
		return "Peasant being built with id " + (next.peasants.size());
	}
}

class deposit implements StripsAction {
	GameMap game;
	int[] peasants;
	Position locationpos;
	int locationId;
	StripsAction camefrom;
	GameState gs;

	// Initializes agents position and location the agent wants go to
	public deposit(int[] agentIDs, Position location, int locId, GameState state) { 
		// the Position location parameter will ALWAYS be the townhall position
		this.gs = state;
		peasants = agentIDs;
		game = state.game;
		locationpos = location;
		locationId = locId;
		camefrom = state.camefrom;
	}
	@Override
	public int[] getActorIds() {
		return peasants;
	}
	@Override
	public boolean preconditionsMet() { 
		// if we're not at goal, we are adjacent to the townhall, and have a resource
		boolean met = peasants.length == 1 && !gs.isGoal();
		for (int i = 0; met && i < peasants.length; i++) {		
			met = game.peasants.get(peasants[i]).getTurns() <= 0 
					&& game.peasants.get(peasants[i]).getPosition().isAdjacent(locationpos)
					&& game.peasants.get(peasants[i]).hasResource(); 
		}
		return met;
	}
	@Override
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action) {
		GameState gs = new GameState(state, playernum, requiredGold, requiredWood, buildPeasants);
		deposit checker = new deposit(action.getActorIds(), action.getLocationPos(), action.getLocationID(), gs);
		return checker.preconditionsMet();
	}
	@Override
	public int getLocationID() {
		return locationId;
	}
	
	@Override 
	public Position getLocationPos() {
		return locationpos;
	}
	
	@Override
	public GameState apply(GameState state) {
		game = state.game.copyAll();
		int woodDeposit = 0, goldDeposit = 0;
		game.updateTurns(peasants, 1);
		for (int i = 0; i < peasants.length; i++) {
			Peasant p = game.peasants.get(peasants[i]);
			woodDeposit += p.getWood();
			goldDeposit += p.getGold();
			p.setWood(0);
			p.setGold(0);
		}
		game.currentGold += goldDeposit;
		game.currentWood += woodDeposit;
		return new GameState(game, (int) state.cost + 1, this, state); // previously + 25
	}

	@Override
	// returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}

	@Override
	public String toString() {
		return "Agent " + peasants[0] + " DEPOSIT(" + locationpos.x + ", " + locationpos.y + ")";
	}

	@Override
	public HashMap<Integer, Action> toSepiaAction(int[] stateId) {
		HashMap<Integer, Action> actions = new HashMap<Integer, Action>();
		actions.put(stateId[0], Action.createCompoundDeposit(stateId[0], locationId));
		//actions.put(stateId[0], Action.createPrimitiveDeposit(stateId[0],
			//	game.peasants.get(peasants[0]).getPosition().getDirection(locationpos)));
		return actions;
	}
}

class deposit2 implements StripsAction {
	GameMap game;
	int numAgents;
	int[] peasants;
	Position locationpos;
	int locationId;
	StripsAction camefrom;
	GameState gs;

	// Initializes agents position and location the agent wants go to
	public deposit2(int[] agentIDs, Position location, int locId, GameState state) {
		// the Position location parameter will ALWAYS be the townhall position
		peasants = agentIDs;
		gs = state;
		if ((numAgents = agentIDs.length) == 2) {
			game = state.game;
			locationpos = location;
			locationId = locId;
			camefrom = state.camefrom;
		}
	}
	@Override
	public int[] getActorIds() {
		return peasants;
	}
	@Override
	public int getLocationID() {
		return locationId;
	}
	
	@Override 
	public Position getLocationPos() {
		return locationpos;
	}
	@Override
	public boolean preconditionsMet() { 
		// if we're not at goal, we are adjacent to the townhall, and have a resource
		boolean met = !gs.isGoal() && numAgents == 2;
		for (int i = 0; met && i < peasants.length; i++) {
			met = game.peasants.get(peasants[i]).getTurns() <= 0
					&& game.peasants.get(peasants[i]).getPosition().isAdjacent(locationpos)
					&& game.peasants.get(peasants[i]).hasResource();
		}
		return met;
	}
	
	@Override
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action) {
		GameState gs = new GameState(state, playernum, requiredGold, requiredWood, buildPeasants);
		deposit2 checker = new deposit2(action.getActorIds(), action.getLocationPos(), action.getLocationID(), gs);
		return checker.preconditionsMet();
	}

	@Override
	public GameState apply(GameState state) {
		game = state.game.copyAll();
		int woodDeposit = 0, goldDeposit = 0;
		game.updateTurns(peasants, 1);
		for (int i = 0; i < peasants.length; i++) {
			Peasant p = game.peasants.get(peasants[i]);
			woodDeposit += p.getWood();
			goldDeposit += p.getGold();
			p.setWood(0);
			p.setGold(0);
		}
		game.currentGold += goldDeposit;
		game.currentWood += woodDeposit;
		return new GameState(game, (int) state.cost + 1, this, state); // previously + 25
	}

	@Override
	// returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}

	@Override
	public String toString() {
		return "Agent " + peasants[0] + ", " + peasants[1] + " DEPOSIT(" + locationpos.x + ", " + locationpos.y + ")";
	}

	@Override
	public HashMap<Integer, Action> toSepiaAction(int[] stateId) {
		HashMap<Integer, Action> actions = new HashMap<Integer, Action>();
		for (int i = 0; i < stateId.length; i++)
			actions.put(stateId[i], Action.createCompoundDeposit(stateId[i], locationId));
			//actions.put(stateId[i], Action.createPrimitiveDeposit(stateId[i],
				//	game.peasants.get(peasants[i]).getPosition().getDirection(locationpos)));
		return actions;
	}
}

class deposit3 implements StripsAction {
	GameMap game;
	int[] peasants;
	Position locationpos;
	int locationId;
	StripsAction camefrom;
	int numAgents;
	GameState gs;

	// Initializes agents position and location the agent wants go to
	public deposit3(int[] agentIDs, Position location, int locId, GameState state) { 
		// the Position location parameter will ALWAYS be the townhall position
		gs = state;
		if ((numAgents = agentIDs.length) == 3) {
			peasants = agentIDs;
			game = state.game;
			locationpos = location;
			locationId = locId;
			camefrom = state.camefrom;	
		}
	}
	@Override
	public int getLocationID() {
		return locationId;
	}
	
	@Override 
	public Position getLocationPos() {
		return locationpos;
	}
	@Override
	public int[] getActorIds() {
		return peasants;
	}
	@Override
	public boolean preconditionsMet() { // if we're not at goal, we are adjacent to the townhall, and have a resource
		boolean met = !gs.isGoal() && numAgents == 3;
		for (int i = 0; met && i < peasants.length; i++) {
			met = game.peasants.get(peasants[i]).getTurns() <= 0
				&& game.peasants.get(peasants[i]).getPosition().isAdjacent(locationpos)
				&& game.peasants.get(peasants[i]).hasResource();
		}
		return met;
	}
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action) {
		GameState gs = new GameState(state, playernum, requiredGold, requiredWood, buildPeasants);
		deposit3 checker = new deposit3(action.getActorIds(), action.getLocationPos(), action.getLocationID(), gs);
		return checker.preconditionsMet();
	}
	
	@Override
	public GameState apply(GameState state) {
		game = state.game.copyAll();
		int woodDeposit = 0, goldDeposit = 0;
		game.updateTurns(peasants, 1);
		for (int i = 0; i < peasants.length; i++) {
			Peasant p = game.peasants.get(peasants[i]);
			woodDeposit += p.getWood();
			goldDeposit += p.getGold();
			p.setWood(0);
			p.setGold(0);
		}
		game.currentGold += goldDeposit;
		game.currentWood += woodDeposit;
		return new GameState(game, (int) state.cost + 1, this, state); // previously + 25
	}

	@Override
	// returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}

	@Override
	public String toString() {
		return "Agent " + peasants[0] + ", " + peasants[1] + ", " + peasants[2] + " DEPOSIT(" + locationpos.x + ", "
				+ locationpos.y + ")";
	}

	@Override
	public HashMap<Integer, Action> toSepiaAction(int[] stateId) {
		HashMap<Integer, Action> actions = new HashMap<Integer, Action>();
		for (int i = 0; i < peasants.length; i++)
			//actions.put(stateId[i], Action.createPrimitiveDeposit(stateId[i],
			//		game.peasants.get(peasants[i]).getPosition().getDirection(locationpos)));
			actions.put(stateId[i], Action.createCompoundDeposit(stateId[i], locationId));
		return actions;
	}
}

class harvest implements StripsAction {
	GameMap game;
	int[] peasants;
	HashMap<Integer, Resource> resources;
	Position locationpos;
	int locationId;
	StripsAction camefrom;
	GameState gs;

	// Initializes agents position and location the agent wants go to
	public harvest(int[] p, Position location, int locId, GameState state) {
		game = state.game;
		gs = state;
		peasants = p;
		// creates a second resource map with all the same values. allows us to change
		resources = game.resources;
		locationpos = location;
		locationId = locId;
		camefrom = state.camefrom;
	}
	@Override
	public int[] getActorIds() {
		return peasants;
	}
	@Override
	public boolean preconditionsMet() {
		boolean met = peasants.length == 1 && !gs.isGoal();
		for (int i = 0; met && i < peasants.length; i++) { 
			met = game.peasants.get(peasants[i]).getTurns() <= 0
					&& game.resources.get(locationId) != null
					&& game.resources.get(locationId).remaining >= 100
					&& game.peasants.get(peasants[i]).getPosition().isAdjacent(locationpos)
					&& !game.peasants.get(peasants[i]).hasResource(); // makes sure state isn't goal state and that the agent is next to harvest location
		}
		return met;
	}
	@Override
	public int getLocationID() {
		return locationId;
	}
	
	@Override 
	public Position getLocationPos() {
		return locationpos;
	}
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action) {
		GameState gs = new GameState(state, playernum, requiredGold, requiredWood, buildPeasants);
		harvest checker = new harvest(action.getActorIds(), action.getLocationPos(), action.getLocationID(), gs);
		return checker.preconditionsMet();
	}
	

	@Override
	public GameState apply(GameState state) {
		game = state.game.copyAll();
		Resource resource = game.resources.get(locationId);
		int goldAmt = 0;
		int woodAmt = 0;
		int duration = 0;
		for (int i = 0; i < peasants.length; i++) {
			if (resource.isGold) { // determine where it collected the resource from
				int gold = Math.max(0, Math.min(100, resource.remaining));
				game.peasants.get(peasants[i]).setGold(gold);
				resource.remaining -= gold;
				goldAmt += gold;
				duration = 200;
			} else {
				int wood = Math.max(0, Math.min(100, resource.remaining));
				game.peasants.get(peasants[i]).setWood(wood);
				resource.remaining -= wood;
				woodAmt += wood;
				duration = 1000;
			}
		}
		game.updateTurns(peasants, 1);
		duration = 1;
		if (resource.remaining <= 0) // ahh this is where I decrease the resource amount
			game.resources.remove(locationId);
		return new GameState(game, (int) state.cost + duration, this, state);
		// returns new GameState with updated class variables and stripsAction that led to this GameState
	}

	@Override
	// returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}

	@Override
	public String toString() {
		return "Agent " + peasants[0] + " HARVESTING( " + locationpos.x + ", " + locationpos.y + ")";
	}

	public HashMap<Integer, Action> toSepiaAction(int[] stateId) {
		HashMap<Integer, Action> actions = new HashMap<Integer, Action>();
		for (int i = 0; i < peasants.length; i++)
			//actions.put(stateId[i], Action.createPrimitiveGather(stateId[i],
				//	game.peasants.get(peasants[i]).getPosition().getDirection(locationpos)));
			actions.put(stateId[i], Action.createCompoundGather(stateId[i], locationId));
		return actions;
	}
}

class harvest2 implements StripsAction {
	GameMap game;
	int[] peasants;
	HashMap<Integer, Resource> resources;
	Position locationpos;
	int locationId;
	StripsAction camefrom;
	int numAgents;
	GameState gs;
	// Initializes agents position and location the agent wants go to
	public harvest2(int[] p, Position location, int locId, GameState state) {
		gs = state;
		if ((numAgents = p.length) == 2) {
			game = state.game;
			peasants = p;
			// creates a second resource map with all the same values. allows us to change
			resources = game.resources;
			locationpos = location;
			locationId = locId;
			camefrom = state.camefrom;
		}
	}
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action) {
		GameState gs = new GameState(state, playernum, requiredGold, requiredWood, buildPeasants);
		harvest2 checker = new harvest2(action.getActorIds(), action.getLocationPos(), action.getLocationID(), gs);
		return checker.preconditionsMet();
	}
	@Override
	public int[] getActorIds() {
		return peasants;
	}
	@Override
	public boolean preconditionsMet() {
		boolean met = !gs.isGoal() && numAgents == 2;
		for (int i = 0; met && i < peasants.length; i++) { // DANIEL: I may want to make the remaining resource 200instead of 100
			met = game.peasants.get(peasants[i]).getTurns() <= 0 && game.resources.get(locationId).remaining > 100
					&& game.peasants.get(peasants[i]).getPosition().isAdjacent(locationpos)
					&& !game.peasants.get(peasants[i]).hasResource(); // makes sure state isn't goal state and that the
																		// agent is next to harvest location
		}
		return met;
	}
	@Override
	public int getLocationID() {
		return locationId;
	}
	
	@Override 
	public Position getLocationPos() {
		return locationpos;
	}
	@Override
	public GameState apply(GameState state) {
		game = state.game.copyAll();
		Resource resource = game.resources.get(locationId);
		int goldAmt = 0;
		int woodAmt = 0;
		int duration = 0;
		for (int i = 0; i < peasants.length; i++) {
			if (resource.isGold) { // determine where it collected the resource from
				int gold = Math.max(0, Math.min(100, resource.remaining));
				game.peasants.get(peasants[i]).setGold(gold);
				resource.remaining -= gold;
				goldAmt += gold;
				duration = 200;
			} else {
				int wood = Math.max(0, Math.min(100, resource.remaining));
				game.peasants.get(peasants[i]).setWood(wood);
				resource.remaining -= wood;
				woodAmt += wood;
				duration = 1000;
			}
		}
		game.updateTurns(peasants, 1);
		duration = 1;
		if (resource.remaining <= 0) // ahh this is where I decrease the resource amount
			game.resources.remove(locationId);
		return new GameState(game, (int) state.cost + duration, this, state);
		// returns new GameState with updated class variables and stripsAction that led
		// to this GameState
	}

	@Override
	// returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}

	@Override
	public String toString() {
		return "Agent " + peasants[0] + ", " + peasants[1] + " ," + " HARVESTING( " + locationpos.x + ", "
				+ locationpos.y + ")";
	}

	public HashMap<Integer, Action> toSepiaAction(int[] stateId) {
		HashMap<Integer, Action> actions = new HashMap<Integer, Action>();
		for (int i = 0; i < peasants.length; i++)
			actions.put(stateId[i], Action.createCompoundGather(stateId[i], locationId));
			//actions.put(stateId[i], Action.createPrimitiveGather(stateId[0],
				//	game.peasants.get(peasants[0]).getPosition().getDirection(locationpos)));
		return actions;
	}
}

class harvest3 implements StripsAction {
	GameMap game;
	int[] peasants;
	HashMap<Integer, Resource> resources;
	Position locationpos;
	int locationId;
	StripsAction camefrom;
	int numAgents;
	GameState gs;

	// Initializes agents position and location the agent wants go to
	public harvest3(int[] p, Position location, int locId, GameState state) {
		game = state.game;
		gs = state;
		if ((numAgents = p.length) == 3) {
			peasants = p;
			// creates a second resource map with all the same values. allows us to change
			resources = game.resources;
			locationpos = location;
			locationId = locId;
			camefrom = state.camefrom;
		}
	}
	@Override
	public int[] getActorIds() {
		return peasants;
	}
	@Override
	public boolean preconditionsMet() {
		boolean met = !gs.isGoal() && numAgents == 3;
		for (int i = 0; met && i < peasants.length; i++) { // DANIEL: I may want to make the remaining resource 200
															// instead of 100
			met = game.peasants.get(peasants[i]).getTurns() <= 0 && game.resources.get(locationId).remaining > 100
					&& game.peasants.get(peasants[i]).getPosition().isAdjacent(locationpos)
					&& !game.peasants.get(peasants[i]).hasResource(); // makes sure state isn't goal state and that the
																		// agent is next to harvest location
		}
		return met;
	}
	public boolean preconditionsMet(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants, StripsAction action) {
		GameState gs = new GameState(state, playernum, requiredGold, requiredWood, buildPeasants);
		harvest3 checker = new harvest3(action.getActorIds(), action.getLocationPos(), action.getLocationID(), gs);
		return checker.preconditionsMet();
	}
	@Override
	public int getLocationID() {
		return locationId;
	}
	
	@Override 
	public Position getLocationPos() {
		return locationpos;
	}
	@Override
	public GameState apply(GameState state) {
		game = state.game.copyAll();
		Resource resource = game.resources.get(locationId);
		int goldAmt = 0;
		int woodAmt = 0;
		int duration = 0;
		for (int i = 0; i < peasants.length; i++) {
			if (resource.isGold) { // determine where it collected the resource from
				int gold = Math.max(0, Math.min(100, resource.remaining));
				game.peasants.get(peasants[i]).setGold(gold);
				resource.remaining -= gold;
				goldAmt += gold;
				duration = 200;
			} else {
				int wood = Math.max(0, Math.min(100, resource.remaining));
				game.peasants.get(peasants[i]).setWood(wood);
				resource.remaining -= wood;
				woodAmt += wood;
				duration = 1000;
			}
		}
		game.updateTurns(peasants, 1);
		duration = 1;
		if (resource.remaining <= 0) // ahh this is where I decrease the resource amount
			game.resources.remove(locationId);
		return new GameState(game, (int) state.cost + duration, this, state);
	}

	@Override
	// returns StripsAction took to get to this state
	public StripsAction getCameFrom() {
		return camefrom;
	}

	@Override
	public String toString() {
		return "Agent " + peasants[0] + ", " + peasants[1] + ", " + peasants[2] + " HARVESTING( " + locationpos.x + ", "
				+ locationpos.y + ")";
	}

	public HashMap<Integer, Action> toSepiaAction(int[] stateId) {
		HashMap<Integer, Action> actions = new HashMap<Integer, Action>();
		for (int i = 0; i < peasants.length; i++)
			//actions.put(stateId[i], Action.createPrimitiveGather(stateId[i],
				//	game.peasants.get(peasants[i]).getPosition().getDirection(locationpos)));
			actions.put(stateId[i], Action.createCompoundGather(stateId[i], locationId));
		return actions;
	}
}
