package PEAsants;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may
 * add your own methods and members. it converts our plan into action
 */
public class PEAgent extends Agent {

	// The plan being executed
	private Stack<StripsAction> plan;

	// maps the real unit Ids to the plan's unit ids
	// when you're planning you won't know the true unit IDs that sepia assigns. So
	// you'll use placeholders (1, 2, 3).
	// this maps those placeholders to the actual unit IDs.
	private Map<Integer, Integer> peasantIdMap;
	private List<Integer> peasants = new ArrayList<Integer>();
	private int townhallId;
	private int peasantTemplateId;
	private int numPeasants = 1;
	private int reqWood;
	private boolean buildingPeasant = false;
	private HashMap<Integer, Boolean> completedAction = new HashMap<Integer, Boolean>(); //tracks whether our last action was a random move
	private HashMap<Integer, Action> lastAction = new HashMap<Integer, Action>(); //tracks our last action that was part of the strips plan.
	private int reqGold;
	public PEAgent(int playernum, Stack<StripsAction> plan, int requiredWood, int requiredGold) {
		super(playernum);
		reqWood = requiredWood;
		reqGold = requiredGold;
		peasantIdMap = new HashMap<Integer, Integer>();
		this.plan = plan;
		System.out.println("size of plan " + plan.size());
		/*while (plan.size() > 0)
			System.out.println(plan.pop().toString()); */
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
		// gets the townhall ID and the peasant ID
		int peasantCount = 1;
		System.out.println("Plan ready");
		/*while (plan.size() > 0)
			System.out.println(plan.pop().toString());
		System.exit(0); */
		for (int unitId : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(unitId);
			String unitType = unit.getTemplateView().getName().toLowerCase();
			if (unitType.equals("townhall")) {
				townhallId = unitId;
			} else if (unitType.equals("peasant")) {
				peasants.add(unitId);
				peasantIdMap.put(peasantCount++, unitId);
				//System.out.println("mapped fake id " + (peasantCount - 1) + " to " + unitId);
			}
		}
		// Gets the peasant template ID. This is used when building a new peasant with
		// the townhall
		peasantTemplateId = stateView.getTemplate(playernum, "Peasant").getID();
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		HashMap<Integer,Action> todo = createSepiaAction(plan.peek(), convertIds(plan.pop().getActorIds(), stateView), stateView); 
			actions.put(peasantIdMap.get(1), todo.get(peasantIdMap.get(1))); // initializes the first action of our plan 
		completedAction.put(peasantIdMap.get(1), true);
		return actions;
	}
	//takes a direction and the unit moving, returns whether the move will succeed or not
	public boolean failedMove(Direction d, int unitID, State.StateView state) {
		UnitView unit = state.getUnit(unitID);
		Position next = new Position(unit.getXPosition() + d.xComponent(), unit.getYPosition() + d.yComponent());
		for (Integer i:peasantIdMap.values()) {
			UnitView unit2 = state.getUnit(i);
			if (next.x == unit2.getXPosition() && next.y == unit2.getYPosition())
				return true;
		}
		return false;
	}

	/**

	 *
	 *
	 * To check a unit's progress on the action they were executing last turn, you
	 * can use the following: historyView.getCommandFeedback(playernum,
	 * stateView.getTurnNumber() - 1).get(unitID).getFeedback() This returns an enum
	 * ActionFeedback. When the action is done, it will return
	 * ActionFeedback.COMPLETED
	 *
	 * Alternatively, you can see the feedback for each action being executed during
	 * the last turn. Here is a short example. if (stateView.getTurnNumber() != 0) {
	 * Map<Integer, ActionResult> actionResults =
	 * historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1); for
	 * (ActionResult result : actionResults.values()) { <stuff> } } Also remember to
	 * check your plan's preconditions before executing!
	 */
	@Override
	public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
		// might need something to check if the next plan requires multiple peasants,
		// but one of them is not available

		// note, we don't check precondition here because we check if the action in the
		// plan has been completed
		/*if (atGoal(stateView)) { //true when we need to perform one more deposit action
			//System.out.println("goal found! one action left");
			//return lastAction;
		}*/
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
			if (stateView.getTurnNumber() > 2) { // we want to skip the first turn
				HashMap<Integer, Action> todo = new HashMap<Integer, Action>(); //todo is the next action, lastAction is the previous action 
				Map<Integer, ActionResult> feedback = historyView.getPrimitiveFeedback(playernum, stateView.getTurnNumber() - 1);
				Map<Integer, ActionResult> feedback2 = historyView.getPrimitiveFeedback(playernum, stateView.getTurnNumber() - 2);
				
				if (feedback.get(townhallId) != null && numPeasants > stateView.getUnitIds(playernum).size() - 1) {
					System.out.println(feedback.get(townhallId));// == ActionFeedback.FAILED) {
					System.out.println("progress " + stateView.getUnit(townhallId).getCurrentDurativeProgress());
					System.out.println("did the production fail?");
					UnitView th = stateView.getUnit(townhallId);
					for (Integer id: peasantIdMap.values()) {
						UnitView unit = stateView.getUnit(id);
						if (Math.abs(unit.getXPosition() - th.getXPosition())< 2 && Math.abs(unit.getYPosition() - th.getYPosition()) < 2) {
							actions.put(unit.getID(), randomMove(stateView, unit.getID()));
					}
					}
					actions.put(townhallId, lastAction.get(townhallId));
					return actions;
				}
				if (plan.size() > 0) {
					int[] actors = convertIds(plan.peek().getActorIds(), stateView);
					System.out.println("actors is " + actors);
					if (actors != null)
						todo = createSepiaAction(plan.peek(), actors, stateView); // in case we get a parallel action we want to assign all of them
					else
						todo = null;
				}
				else 
					todo = lastAction;
				if (todo == null) {					
					System.out.println("NULLNLSKDNFLASKDNF;ALSKNFSDKLFN");
					return actions;
				}
				if (todo != null) {
					if (todo.get(townhallId) != null) { //we can add the production action always as long as it's in the plan
						actions.put(townhallId, todo.get(townhallId));
						lastAction.put(townhallId, todo.get(townhallId));
						System.out.println("Action completed: " + plan.pop().toString());
						System.out.println("sepia version for this action is " + todo.get(townhallId));
						numPeasants++;
						return actions;
						//todo = createSepiaAction(plan.peek(), plan.peek().getActorIds()); // in case we get a parallel action we want to assign all of them
					}
					else {
						boolean movePossible = true;
						for (int unitId : todo.keySet()) { //get everyone that does something					
							//check if the units in the action are available
						if (movePossible) {
							movePossible = false;
							ActionResult result = feedback.get(unitId); //result == null 
							System.out.println("RESULT for unit " + unitId + ": " +  result); //this is the previous turn's result. createSepia is the next turn if possible
							ActionResult result2 = feedback2.get(unitId);
							if (result == null|| result.getFeedback() == ActionFeedback.FAILED){ // ) {// 
								//if there was an actual conflict, assign a random move
								if(result2 != null && result2.getFeedback() == ActionFeedback.FAILED) {//.Failed
									if (stateView.getTurnNumber() > 3) {
										System.out.println("unit id " + unitId + ", adding a random move");
										actions.put(unitId, randomMove(stateView, unitId)); 
										completedAction.put(unitId, false); 
									}
								}
								//these signal the dude is available
								if ((result2 != null && result2.getFeedback() == ActionFeedback.COMPLETED) || result2 == null) { //result2 == null removed bc redundnt BUT now we might end up trapped
									if (!completedAction.get(unitId)) { //if we completed a random move last turn
										completedAction.put(unitId, true); //we are done with our random move, now we need to reinitiate
										actions.put(unitId, lastAction.get(unitId)); //add the last action back in
									}
									else //the only time we allow ourselves to move forward
										movePossible = true; //This is the only scenario where move is possible
								}
							}
							else
								if (result.getAction().getType()== ActionType.FAILEDPERMANENTLY) {
									completedAction.put(unitId, false);
									actions.put(unitId, randomMove(stateView,unitId));
								}
							}
						}
						if (movePossible) {
							//if (plan.peek().preconditionsMet(stateView, playernum, reqGold, reqWood, true, plan.peek())) {
							for (Integer k: todo.keySet()) { //for all of the actors
								actions.put(k, todo.get(k));
								lastAction.put(k, todo.get(k));
							}
							if (plan.size() > 1)
								plan.pop();
							System.out.print("finished the previous move"
									+ "\n beginning " + plan.peek());
						}
					}
				
				}
			}
			
		//	atGoal(stateView);
		return actions;
	}
	
	public boolean atGoal(State.StateView state) {
		int currGold = state.getResourceAmount(playernum, ResourceType.GOLD);
		int currWood = state.getResourceAmount(playernum, ResourceType.WOOD);
		if (currGold >= reqGold && currWood >= reqWood) {
			System.out.println("Good job! We obtained " + currGold + " gold and " + currWood + " wood in " + state.getTurnNumber() + " turns.");
		}
		if (currGold >= 900 && currWood >= 900 && Math.max(currGold, currWood) >= 1000)
			return true;
		return false;
	}
		//converts keys to values. automatically adds newly encountered Ids
		public int[] convertIds(int[] stripID, State.StateView state){
			int[] ids = new int[stripID.length];
			for (int i = 0; i < stripID.length; i++) { //this should include the newly created agent
				if (stripID[i] == townhallId)
					ids[i] = stripID[i];
				else {
					if (peasantIdMap.containsKey(stripID[i]))
						ids[i] = peasantIdMap.get(stripID[i]);
					else {
						//we need to find the id of the newly added unit. 
						List<Integer> units = state.getUnitIds(playernum); 
						boolean found = false;
						if (stripID[0] == townhallId)
							return stripID;
						for (int j = 0;!found && j < units.size(); j++) {
							if (found = (!peasantIdMap.containsValue(units.get(j)) && units.get(j) != townhallId)) {
								ids[i] = units.get(j);
								System.out.println("added new peasant w/ strip ID " + stripID[i] + " and sepia id " + ids[i]);
								peasantIdMap.put(stripID[i], ids[i]);
								completedAction.put(ids[i], true); //tell them that this unit exists and is available
							}		
						} 
						if (!found) {
							System.out.print("STRIP ID VALS: ");
							for (int k: stripID)
								System.out.print(k + ",");
							System.out.print("\nOur ID VALS:");
							for (int k: stripID)
								System.out.print(peasantIdMap.get(k));
							System.out.println("\n"); 
							System.out.println("LIST OF UNITS IN EXISTENCE");
							for (int k : state.getUnitIds(playernum))
								System.out.println(k);
							return null;
						}
					}
				}
			}
			return ids;
		}
		
		public Action randomMove(StateView state, int unitId) {
			UnitView unit = state.getUnit(unitId);
			Position p = new Position(unit.getXPosition(), unit.getYPosition());
			int randX;
			int randY;
			do {
			randX = (int)Math.round(Math.random() * 2) - 1;
			randY = (int)Math.round(Math.random() * 2) - 1;
			} while ((randX == 0 && randY == 0) || state.isUnitAt(p.x + randX, p.y + randY) 
					|| state.isResourceAt(p.x + randX, p.y+ randY));
			//stops once it finds an open position
			Direction d = p.getDirection(new Position(p.x + randX, p.y + randY));
			System.out.println(d);
			return Action.createPrimitiveMove(unitId, d);
		}
	/**
	 * @param action StripsAction
	 * @return SEPIA representation of same action
	 */
	private HashMap<Integer, Action> createSepiaAction(StripsAction action, int[] realId, StateView state) {	
		return action.toSepiaAction(realId); // sends in the actual id
	}

	@Override
	public void terminalStep(State.StateView stateView, History.HistoryView historyView) {
		
	}

	@Override
	public void savePlayerData(OutputStream outputStream) {

	}

	@Override
	public void loadPlayerData(InputStream inputStream) {

	}
}