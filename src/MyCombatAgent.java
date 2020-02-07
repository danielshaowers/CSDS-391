import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class MyCombatAgent extends Agent {
	private boolean[] baitComplete = {false, false}; //tracks whether the first and second compound move are complete. no other units act until both true
	private int enemyPlayerNum = 1;
	private Action[] previousTurn; //tracks the action taken in the previous turn. Used to track completed actions
	private int enemyCount; //number of enemies surviving. Used to track enemy deaths
	public MyCombatAgent(int playernum, String[] otherargs) {
		super(playernum);
		if(otherargs.length > 0)
		{
			enemyPlayerNum = new Integer(otherargs[0]);
		}	
	}
	
	@Override
	//sends one footman to attract the attention of one enemy footman, while staying out of sight for other footmen
	public Map<Integer, Action> initialStep(StateView newstate, HistoryView statehistory) {
		  Map<Integer, Action> actions = new HashMap<Integer, Action>();
          List<Integer> myUnitIDs = newstate.getUnitIds(playernum);
          enemyCount = newstate.getUnitIds(enemyPlayerNum).size();
          previousTurn = new Action[myUnitIDs.size()];
          // start by commanding every single unit to attack an enemy unit 
          actions.put(myUnitIDs.get(0), Action.createCompoundMove(myUnitIDs.get(0), 13, 8)); //moves one unit to bait out an enemy soldier
          return actions;
	}
	//helper method for middle step. it waits until the compound move in the initial step is completed,
	//then it commands the footman to return to its team mates.
	private Action bait(StateView newstate){
		List<Integer> myUnitIds = newstate.getUnitIds(playernum);
		Action hide = null;
		UnitView unit = newstate.getUnit(myUnitIds.get(0)); //the footman sent out
		//checks if the SECOND step is complete by examining if our unit's current action and previous action are null
		if (baitComplete[0] && !baitComplete[1] && unit.getCurrentDurativeAction() == null && previousTurn[0] == null) 
			baitComplete[1] = true;
		//checks if the FIRST step is complete by examining if current and previous action are null
		if (!baitComplete[0] && unit.getCurrentDurativeAction() == null && previousTurn[0] == null) { //if the initial step was still being executed, check if it is still being executed		
			baitComplete[0] = true;
			//if the first step is complete, we update our boolean array and issue the next command
			hide = Action.createCompoundMove(myUnitIds.get(0), 8, 9); //return to comrades
		}
		return hide;
	}
	
	@Override
	//repeatedly calls the bait helper method UNTIL both bait steps are complete. Once the first unit completes
	//both movements, one enemy soldier will be drawn towards our units. Then all units target the nearest enemy soldier.
	//once that enemy soldier is killed, bait is called again, so another footman runs out and attracts one more soldier
	public Map<Integer, Action> middleStep(StateView newstate, HistoryView statehistory) {
		Map<Integer, Action> actions = new HashMap<Integer, Action>(); //the actions to be performed by each unit
		List<Integer> myUnitIds = newstate.getUnitIds(playernum);  //full list of all unitIDs
		UnitView unit = newstate.getUnit(myUnitIds.get(0));
		List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);
		Action hide = bait(newstate);  //calls the bait method
		if (hide != null) 	// hide = null when the first movement is incomplete. When movement 1 completes, hide is the command to return to base
			actions.put(myUnitIds.get(0), hide);
		previousTurn[0] = unit.getCurrentDurativeAction(); //updates the current action of our unit.
		if (baitComplete[1]) { //this code only executes after the first unit successfully completes both compound movements to bait out a soldier
		// Used to track enemy unit Ids
			List<Integer> eFootIds = new ArrayList<Integer>();
			List<Integer> eTowerIds = new ArrayList<Integer>(); 		//note to self: the tower cannot move
		//identify the specific enemy units
			for (Integer id: enemyUnitIDs) {
				String type = newstate.getUnit(id).getTemplateView().getName();
				if (type.equals("Footman"))
					eFootIds.add(id);
				else 
					eTowerIds.add(id); //was 65
			}
		//stop when no enemies left
			if(enemyUnitIDs.size() == 0)
				return actions;		
			//command all units to attack the nearest enemy
			for (int i = 0; i < myUnitIds.size(); i++) {	//we use a basic for loop because we need to track iteration # for line 99
				int unitId = myUnitIds.get(i);		//get the current unitId
				unit = newstate.getUnit(unitId);	
				if (unit.getCurrentDurativeAction() == null && previousTurn[i] == null) { //if no action is currently being performed
					int nearestEnemy = nearestEnemy(unit, newstate);	//uses helper method "nearestEnemy" to find nearest enemy
						if (eFootIds.size() != 0) 		//if there are foot soldiers left, we attack the nearest foot soldier
							actions.put(unitId, Action.createCompoundAttack(unitId, enemyUnitIDs.get(nearestEnemy)));
						else 		//otherwise, we attack the tower
							actions.put(unitId, Action.createCompoundAttack(unitId, eTowerIds.get(0)));
				}
				previousTurn[i] = unit.getCurrentDurativeAction();	//now that the turn is complete, we update the previous turn's action
			}
			//Attacks fail if the targeted unit is out of range. To account for this, we check for failures then try again
			for (ActionResult feedback : statehistory.getCommandFeedback(playernum, newstate.getTurnNumber() - 1).values()) {
				if (feedback.getFeedback() == ActionFeedback.FAILED) {
					int unitID = feedback.getAction().getUnitId();
					if (eFootIds.size() != 0) { 
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyUnitIDs.get(nearestEnemy(newstate.getUnit(unitID), newstate))));
					}
					else
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyUnitIDs.get(0)));
				}
			}
		}
		if (enemyCount > enemyUnitIDs.size()) { //if an enemy unit died, then we perform the "bait" once again
			enemyCount = enemyUnitIDs.size();
			actions = new HashMap<Integer, Action>(); //clear any pending actions
			for (Integer i: myUnitIds)
				actions.put(i, Action.createFail(i)); //stop current compound actions
			actions.put(myUnitIds.get(0), Action.createCompoundMove(myUnitIds.get(0), 19-enemyCount, 7)); //we creep closer each time an enemy dies in order to attract the next one
		    baitComplete[0] = false; //reset the flags for bait steps
		    baitComplete[1] = false;
		}	
		return actions;
	}
	//return -1 if no nearby enemies. Finds the nearest enemy relative to the unit named myUnit
	public int nearestEnemy(UnitView myUnit, StateView currState) { 
		List<Integer> enemyUnitIds = currState.getUnitIds(enemyPlayerNum);	//get all enemy units
		//x,y = enemy location. minDist = closest enemy found. closestID = ID of the nearest enemy
		int x; int y; Integer minDist = Integer.MAX_VALUE; int closestID = -1;	
		for (int i = 0; i < enemyUnitIds.size(); i++){	//run through all enemies
			 x = currState.getUnit(enemyUnitIds.get(i)).getXPosition();
			 y = currState.getUnit(enemyUnitIds.get(i)).getYPosition();
			 int dist = Math.abs(myUnit.getXPosition() - x) + Math.abs(myUnit.getYPosition() - y); //find distance
			 if (dist < minDist) { //update the closestID and minDist if a closer enemy is found
				 closestID = i;
				 minDist = dist;
			 }
		}
		return closestID;
	}
		
	@Override
	public void terminalStep(StateView newstate, HistoryView statehistory) {
		System.out.println("Finished the episode");
	}

	@Override
	public void savePlayerData(OutputStream os) {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadPlayerData(InputStream is) {
		// TODO Auto-generated method stub

	}

}

