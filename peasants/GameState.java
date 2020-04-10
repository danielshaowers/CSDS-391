package PEAsants;

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
 * This class is used to represent the state of the game after applying one of
 * the avaiable actions. It will also track the A* specific information such as
 * the parent pointer and the cost and heuristic function. Remember that unlike
 * the path planning A* from the first assignment the cost of an action may be
 * more than 1. Specifically the cost of executing a compound action such as
 * move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2).
 * Implement the methods provided and add any other methods and member variables
 * you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
 * Note that SEPIA saves the townhall as a unit. Therefore when you create a
 * GameState instance, you must be able to distinguish the townhall from a
 * peasant. This can be done by getting the name of the unit type from that
 * unit's TemplateView:
 * state.getUnit(id).getTemplateView().getName().toLowerCase(): returns
 * "townhall" or "peasant"
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
 * I recommend storing the actions that generated the instance of the GameState
 * in this class using whatever class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {
	Position townhall;
	GameMap game;
	int playerNum;
	StateView state;
	int requiredGold;
	int requiredWood;
	int currentGold = 0;
	int currentFood = 0;
	int currentWood = 0;
	int cost = 0;
	boolean buildPeasants;
	List<Integer> goldmines = new ArrayList<Integer>();
	List<Integer> tree = new ArrayList<Integer>();
	int townHallId;
	List<Integer> peasantIds = new ArrayList<Integer>();
	StripsAction camefrom = null;

	/**
	 * Construct a GameState from a stateview object. This is used to construct the
	 * initial search node. All other nodes should be constructed from the another
	 * constructor you create or by factory functions that you create.
	 *
	 * @param state         The current stateview at the time the plan is being
	 *                      created
	 * @param playernum     The player number of agent that is planning
	 * @param requiredGold  The goal amount of gold (e.g. 200 for the small
	 *                      scenario)
	 * @param requiredWood  The goal amount of wood (e.g. 200 for the small
	 *                      scenario)
	 * @param buildPeasants True if the BuildPeasant action should be considered
	 */
	public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {
		// initalizes variables
		playerNum = playernum;
		this.state = state;
		this.requiredGold = requiredGold;
		this.requiredWood = requiredWood;
		this.buildPeasants = buildPeasants;
		Integer[] playerNums = state.getPlayerNumbers();
		game = new GameMap(1, new HashMap<Integer, Peasant>(), new HashMap<Integer, Resource>(), 2, currentGold, currentWood );
		List<Integer> unitIDs = state.getUnitIds(playerNums[0]);
		// gets townhall and pesant id
		int presetID = 1;
		for (int id : unitIDs) {
			UnitView unit = state.getUnit(id);
			String type = unit.getTemplateView().getName(); // obtains the name of each unit, then categorizes them in
															// respective list
			if (type.equals("TownHall"))
				townHallId = id;
			else if (type.equals("Peasant")) {
				int woodheld = 0;
				int goldheld = 0;
				peasantIds.add(id);
				if (unit.getCargoAmount() > 0) {
					if (unit.getCargoType().compareTo(ResourceType.WOOD) == 0)
						woodheld += unit.getCargoAmount();
					else
						goldheld += unit.getCargoAmount();
				}
				game.peasants.put(presetID, new Peasant(presetID++, new Position(unit.getXPosition(), unit.getYPosition()), goldheld, woodheld, 0));
			}
		}
		 townhall = new Position(state.getUnit(townHallId).getXPosition(), state.getUnit(townHallId).getYPosition());
		this.currentGold = state.getResourceAmount(playernum, ResourceType.GOLD);
		this.currentWood = state.getResourceAmount(playernum, ResourceType.WOOD);
		List<Integer> resourceIds = state.getAllResourceIds(); // gets all resources
		// gets ids of all goldmines and trees in respective list
		for (int id : resourceIds) {
			ResourceView resource = state.getResourceNode(id);
			if (resource.getType().equals(ResourceNode.Type.GOLD_MINE)) {
				goldmines.add(id);
				game.resources.put(id, new Resource(resource.getXPosition(), resource.getYPosition(),
						resource.getAmountRemaining(), id, true));
			} else if (resource.getType().equals(ResourceNode.Type.TREE)) {
				tree.add(id);
				game.resources.put(id, new Resource(resource.getXPosition(), resource.getYPosition(),
						resource.getAmountRemaining(), id, false));
			}
		}
	}

	public GameState(GameMap game, int cost, StripsAction action, GameState state) {
		// initialzes everything
		this.game = game;
		this.goldmines = state.goldmines;
		this.tree = state.tree;
		this.requiredGold = state.requiredGold;
		this.requiredWood = state.requiredWood;
		this.currentGold = game.currentGold; // updates current gold based on apply function in strips action
		this.currentWood = game.currentWood;
		this.camefrom = action; // the action taken to reach this state
		this.townHallId = state.townHallId;
		this.state = state.state;
		this.cost = cost;
		townhall = new Position(state.state.getUnit(townHallId).getXPosition(), state.state.getUnit(townHallId).getYPosition());
	}

	/**
	 * @return true if the goal conditions are met in this instance of game state.
	 */
	public boolean isGoal() {
		return currentGold >= requiredGold && currentWood >= requiredWood;
	}

	// unused function that we're too emotionally attached to to delete. Would help
	// us enumerate all possible actions for any given number of peassants
	public ArrayList<ArrayList<StripsAction>> permute(List<List<StripsAction>> inputs,
			ArrayList<ArrayList<StripsAction>> outputs, ArrayList<StripsAction> current, int listNum) {
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
	 * The branching factor of this search graph are much higher than the planning.
	 * Generate all of the possible successor states and their associated actions in
	 * this method.
	 *
	 * @return A list of the possible successor states and their associated actions
	 */
	// for every peasant, checks each resource and action for possible children
	// based on if preconditions are met
	public List<GameState> generateChildren() {
		List<ArrayList<int[]>> pairs = game.availablePairs();
		List<GameState> children = new LinkedList<GameState>();
		List<StripsAction> allActions = new ArrayList<StripsAction>();
		for (Resource res : game.resources.values()) {
			Position location = new Position(res.x, res.y);
			for (ArrayList<int[]> tuplet : pairs) {
				for (int[] pID : tuplet) {
					moveTo movingAgent = new moveTo(pID, location, res.id, this);
					if (movingAgent.preconditionsMet()) { // checks to see if agent can move to desired location
						allActions.add(movingAgent);
					} // if passes precondition, gets new gamestate with agent at that location
					moveTo2 movingAgent2 = new moveTo2(pID, location, res.id, this);
					if (movingAgent2.preconditionsMet()) {
						allActions.add(movingAgent2);
					}
					moveTo3 movingAgent3 = new moveTo3(pID, location, res.id, this);
					if (movingAgent3.preconditionsMet()) { // checks to see if agent can move to desired location
						allActions.add(movingAgent3);
					}
					harvest get = new harvest(pID, location, res.id, this);
					if (get.preconditionsMet())
						allActions.add(get);
					harvest2 get2 = new harvest2(pID, location, res.id, this);
					if (get2.preconditionsMet())
						allActions.add(get2);
					harvest3 get3 = new harvest3(pID, location, res.id, this);
					if (get3.preconditionsMet())
						allActions.add(get3);
				}
			}
		}
		Position townhall = new Position(state.getUnit(townHallId).getXPosition(), state.getUnit(townHallId).getYPosition());
		buildPeasant baby = new buildPeasant(this, townhall, townHallId);
		if (baby.preconditionsMet())
			allActions.add(baby);
		
		for (ArrayList<int[]> tuplet : pairs) {
			for (int[] pID : tuplet) {
				moveTo movingAgent = new moveTo(pID, townhall, townHallId, this);
				if (movingAgent.preconditionsMet())
					allActions.add(movingAgent);
				moveTo2 movingAgent2 = new moveTo2(pID, townhall, townHallId, this);
				if (movingAgent2.preconditionsMet())
					allActions.add(movingAgent2);
				moveTo3 movingAgent3 = new moveTo3(pID, townhall, townHallId, this);
				if (movingAgent3.preconditionsMet()) 
					allActions.add(movingAgent3);
					deposit dump = new deposit(pID, townhall, townHallId, this); //if we're adjacent and have something in our hands
					if (dump.preconditionsMet())
						allActions.add(dump);
					deposit2 dump2 = new deposit2(pID, townhall, townHallId, this); 
					if (dump2.preconditionsMet())
						allActions.add(dump2);
					deposit3 dump3 = new deposit3(pID, townhall, townHallId, this); 
					if (dump3.preconditionsMet())
						allActions.add(dump3);
			}
		}
			for (StripsAction a : allActions) // add all strips actions to the children stack
				children.add(a.apply(this));
		return children; //every possible child
	}

	/**
	 *
	 * the heuristic simply calculates how much remaining wood and gold must be
	 * gathered. It is admissible because it assumes we can teleport and that all
	 * gold and wood held by the peasant is automatically deposited. Not consistent
	 *
	 */
	public double heuristic() { // distance from townhall + time it takes to harvest. definitely not consistent
		int goldHeld = 0;
		int woodHeld = 0;
		int thDist = 0;	
		int numPeasants = game.peasants.size();
		for (Peasant p : game.peasants.values()) {
			if (p.hasResource()) {
				goldHeld += p.getGold();
				woodHeld += p.getWood();
				thDist = p.getPosition().chebyshevDistance(townhall) - 1;
			}
		}
		int remainingGoldCost = Math.max(0, requiredGold - currentGold - goldHeld);
		int remainingWoodCost = Math.max(0, requiredWood - currentWood - woodHeld);

		//int ngGoldCost = requiredGold - currentGold - goldHeld;
		//int remainingWoodCost =  requiredWood - currentWood - woodHeld;

		return (remainingGoldCost + remainingWoodCost) / numPeasants +thDist; //- 10000 * numPeasants;

		
		//ATTEMPT 2 AT HEURISTIC
	/*	int heuristic = 0;
		int goldHeld = 0;
		int woodHeld = 0;
		int thDist = 0;	
		int numPeasants = game.peasants.size();
		int canBuild = 0;
		for (Peasant p : game.peasants.values()) {
			if (p.hasResource()) {
				goldHeld += p.getGold();
				woodHeld += p.getWood();
				thDist = p.getPosition().chebyshevDistance(townhall) - 1;
			}
		}
		int remainingGoldCost = Math.max(0, requiredGold - currentGold - goldHeld);
		int remainingWoodCost = Math.max(0, requiredWood - currentWood - woodHeld);
		if (currentGold + goldHeld >= 400)
			numPeasants = Math.min(3, numPeasants + 1);
		if (currentGold + goldHeld > woodHeld + currentWood) {
			 heuristic += 10;
		}
		//int ngGoldCost = requiredGold - currentGold - goldHeld;
		//int remainingWoodCost =  requiredWood - currentWood - woodHeld;
		int actors = camefrom.getActorIds().length;
		heuristic += actors * 3;
		heuristic += (remainingGoldCost + remainingWoodCost) / (100*numPeasants) +thDist - canBuild; //- 10000 * numPeasants;
		return heuristic;
		
		*/
		
		
		
		/*int goldHeld = 0;
	
		
		
		
		
		int heuristic = 0;
		int woodHeld = 0;
		int townhall_gold_dist = Integer.MAX_VALUE;
		int townhall_wood_dist = Integer.MAX_VALUE;
		int dist = Integer.MAX_VALUE;
		for (Resource r: game.resources.values()) {
			if (r.isGold)
				townhall_gold_dist = Math.min(townhall_gold_dist, (int)townhall.chebyshevDistance(new Position(r.x, r.y)) - 2);
			else
				townhall_wood_dist = Math.min(townhall_wood_dist, (int)townhall.chebyshevDistance(new Position(r.x, r.y)) - 2);
		} 
		for (Peasant p : game.peasants.values()) {
			if (p.hasResource()) {
				goldHeld += p.getGold() ;
				woodHeld += p.getWood(); 
				dist = Math.min(dist, (int)p.getPosition().chebyshevDistance(townhall) - 1); //might want to do max but i could lose admissabilitiy then. 
			}
		}
		
		int numPeasants = game.peasants.size();
		int remainingGoldCost = Math.abs(requiredGold - currentGold - goldHeld); //how much is left to harvest
		int remainingWoodCost = Math.abs(requiredWood - currentWood - woodHeld); //previously max(0,val), which is more admissible but less good
		double gold_deposit_cycles = (remainingGoldCost) / (100* 2 *numPeasants); //assume we're already next to a resrouce
		double wood_deposit_cycles = remainingWoodCost / (100 * numPeasants);
		if (goldHeld > 0) { //cost to reach the townhall and deposit
			heuristic += Math.max(0, dist) + 1; //cost of reaching the townhall and depositing. now remainingGold corresponds to how much we need to deposit/harvest
			heuristic += 2 * gold_deposit_cycles; //cost of depositing and harvesting
			heuristic += gold_deposit_cycles * 2 * townhall_gold_dist; //cost of going back and forth from th to resource (we are at th rn)
		}
		else {
			if (woodHeld > 0) {
				heuristic += Math.max(0,  dist) + 1;
				heuristic += 2 * wood_deposit_cycles;
				heuristic += wood_deposit_cycles * 2 * townhall_gold_dist;
			}
		
			else { //this means we're not holding anything
				if (camefrom instanceof deposit || camefrom instanceof deposit2 || camefrom instanceof deposit2) {
					heuristic += Math.min(townhall_gold_dist, townhall_wood_dist); //cost to reach the nearest resource. if previously deposit, we know from postconditions that we're still adj to resource
				}
				heuristic += (wood_deposit_cycles - 1) * 2 * townhall_wood_dist; //how many times we need to deposit
				heuristic += (gold_deposit_cycles - 1) * 2 * townhall_gold_dist;
			}
		}
		return heuristic;*/
	}

	/**
	 *
	 * Write the function that computes the current cost to get to this node. This
	 * is combined with your heuristic to determine which actions/states are better
	 * to explore.
	 *
	 * @return The current cost to reach this goal
	 */
	public double getCost() {
		return cost;
	}

	/**
	 * This is necessary to use your state in the Java priority queue. See the
	 * official priority queue and Comparable interface documentation to learn how
	 * this function should work.
	 *
	 * @param o The other game state to compare
	 * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
	 */
	@Override
	public int compareTo(GameState o) {
		double myH = heuristic();
		double oH = o.heuristic();
		if (getCost() + myH > o.getCost() + oH)
			return 1;
		if (getCost() + myH < o.getCost() + oH)
			return -1;
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
	 * This is necessary to use the GameState as a key in a HashSet or HashMap.
	 * Remember that if two objects are equal they should hash to the same value.
	 *
	 * @return An integer hashcode that is equal for equal states.
	 */
	@Override
	public int hashCode() {
		int hash = 17;
		int heldWood = 0;
		int heldGold = 0;
		for (Peasant p : game.peasants.values()) {
			heldWood += p.getGold();
			heldGold += p.getWood();
			hash *= 31 + p.getPosition().hashCode();
			/*hash = hash * 31 + p.getGold();
			hash = hash * 31 + p.getWood();
			hash = hash * 31 + p.getPosition().hashCode();*/
		}
		hash = hash * 31 + heldWood;
		hash = hash * 31 + heldGold;
		hash = hash * 31 + currentGold;
		hash = hash * 31 + currentWood;
		return hash;
	}
}
