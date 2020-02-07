import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.List;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class FirstClass extends Agent {
/*stateview tells current state of game info, historyview for past info*/
	//stored locations as fields to preserve stack space

	public FirstClass(int playernum) {
		super(playernum);
	}
	
	/*good for special initialization to provide info about the map and first actions*/
	@Override
	public Map<Integer, Action> initialStep(StateView newState, HistoryView history) {
		return middleStep(newState, history);  //runs the middle step upon startup
	}

	@Override
	public void loadPlayerData(InputStream arg0) {
		// TODO Auto-generated method stub

	}
	/*called every other round during an episode. This is where the bulk of your agent logic will go.
	 *  You will be able to look at the current state of the game,  the history of the game and assign actions to your units.
	 *  to improve, I'd like to add a third list tracking what is being worked on currently (get currentdurative action) and total resources
	 *  to find which is the best to next work towards */
	
	@Override
	public Map<Integer, Action> middleStep(StateView newState, HistoryView stateHistory) {
		List<Integer> goldmines = newState.getResourceNodeIds(Type.GOLD_MINE);
		List<Integer> trees = newState.getResourceNodeIds(Type.TREE);
		//sorts the action each unit will perform, empty map if no changes to action
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		List<Integer> townHallIds = new ArrayList<Integer>();
		List<Integer> peasantIds = new ArrayList<Integer>(); //peasant IDs
		List<Integer> barrackIds = new ArrayList<Integer>();
		List<Integer> farmIds = new ArrayList<Integer>();
		List<Integer> footIds = new ArrayList<Integer>();
		List<Integer> myUnitIds = newState.getUnitIds(playernum);  //full list of all unitIDs
		for(Integer id: myUnitIds) { //check the unit type
			//unitview gives us specific info about the unit,like type, health, resources
			UnitView unit = newState.getUnit(id);
			String type = unit.getTemplateView().getName(); //template view gives info that all units share, like name
			if (type.equals("TownHall"))
				townHallIds.add(id);
			if (type.equals("Peasant"))
				peasantIds.add(id);
			if (type.equals("Farm"))
				farmIds.add(id);
			if (type.equals("Barracks"))
				barrackIds.add(id);
			if (type.equals("Footman"))
				footIds.add(id);
		}	
		int currGold = newState.getResourceAmount(playernum, ResourceType.GOLD);
		int currWood = newState.getResourceAmount(playernum, ResourceType.WOOD);
		int thID = townHallIds.get(0); //since there's only one townhall, there's no point making a for loop
		for (Integer pID: peasantIds) {
			Action action = null; //reset the action
			if (newState.getUnit(pID).getCargoAmount() > 0)  //if carrying cargo, drop off at FIRST townhall (not closest)
				action = new TargetedAction(pID, ActionType.COMPOUNDDEPOSIT, thID);
			else {
				if (currGold < currWood || barrackIds.size() == 1) //during upgrades I can make this better. check what other agents doing
					action = new TargetedAction(pID, ActionType.COMPOUNDGATHER, goldmines.get(0));
				else //if wood <=gold, gather gold
					action = new TargetedAction(pID, ActionType.COMPOUNDGATHER, trees.get(0));
			}
			actions.put(pID, action); //add the decided action into the action map for each peasant. without, ours would do nothing
					Integer build = null; //represents the templateID of the item to be built
					if (currGold >= 500 && currWood >= 250 && farmIds.size() == 0) {
						build = newState.getTemplate(playernum, "Farm").getID(); //get the buildID for the farm
						actions.put(pID, Action.createPrimitiveBuild(pID, build));
						}
					else
						if (currGold >= 700 && currWood >= 400 && barrackIds.size() == 0) {
							build = newState.getTemplate(playernum, "Barracks").getID();
							actions.put(pID, Action.createPrimitiveBuild(pID, build));		
						}
						else 
								if (currGold >= 600 && barrackIds.size() == 1) { //we can only create footmen when barracks are built 
									build=newState.getTemplate(playernum, "Footman").getID();
									actions.put(barrackIds.get(0), Action.createCompoundProduction(barrackIds.get(0), build));
								}
		}
		return actions;
		
	}

	@Override
	public void savePlayerData(OutputStream arg0) {
	}

	@Override
	public void terminalStep(StateView arg0, HistoryView arg1) {
		System.out.println("final gold is " + arg0.getResourceAmount(playernum, ResourceType.GOLD));
	}

}
