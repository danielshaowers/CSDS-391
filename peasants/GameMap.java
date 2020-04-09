package PEAsants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class GameMap {
	public int freeAgents;
	public HashMap<Integer, Peasant> peasants;
	public HashMap<Integer, Resource> resources;
	public int currentFood;
	public int currentGold;
	public int currentWood;
	public Position townhall;

	public GameMap(int agents, HashMap<Integer, Peasant> peasants, HashMap<Integer, Resource> resources, int currentFood, int currentGold, int currentWood) {
		freeAgents = agents; //count of agents
		this.peasants = peasants;
		this.resources = resources;
		this.currentGold = currentGold;
		this.currentWood = currentWood;
		this.currentFood = currentFood;
	}

	public GameMap() {
	}

	// inputs: p = the peasants that are performing the actions
	//any peasants that dont perform the action have their turn number decreased to show who will next be able to move.
	//if everyone's turn number is >1, then the next to move will be the one with the lowestt turn number, so we decrease everyone's turn count
	//...by the minimum turn count
	public void updateTurns(int[] p, int cost) {
		int min = Integer.MAX_VALUE;
		if (peasants.size() == 1)
			peasants.get(p[0]).setTurns(0);
		else { 
			for (Peasant peas : peasants.values()) { //for all peasants, give a list of the ones that are doing things and incr. their cost
				boolean busy = false;
				for (int i = 0; !busy && i < p.length; i++) {
					if (busy = p[i] == peas.getId()) //so it stops if the peasant is on the list, and sets the turns = cost of action
						peas.setTurns(cost);
				} // if busy was never set to true, then the current agent is already doing something and won't act
				if (!busy) {
					peas.setTurns(peas.getTurns() - cost); // the number of turns after this action completes
				}
				min = Math.min(min, peas.getTurns());
			}
			if (min >= 1) {
				for (Peasant peas : peasants.values()) 
					peas.setTurns(peas.getTurns() - min);
			}
		}
	}

	// returns every combination of two available peasants
	public ArrayList<ArrayList<int[]>> availablePairs() {
		List<Peasant> list = new ArrayList<Peasant>(peasants.values()); // list of all peasants
		ArrayList<int[]> pairs = new ArrayList<int[]>();
		ArrayList<ArrayList<int[]>> allPairs = new ArrayList<ArrayList<int[]>>(); // index corresponds to how many units
		ArrayList<int[]> available = new ArrayList<int[]>();
		ArrayList<int[]> triplets = new ArrayList<int[]>();
		int count = 0;
		for (Peasant p : list) {
			if (p.getTurns() <= 0) {
				available.add(new int[] { p.getId() }); // adds the peasant to our list of available bois
			}
		}
		allPairs.add(0, available);
		if (available.size() >= 2) {
		// now we add all pairs to index 0 of allPairs
			for (int i = 0; i < available.size(); i++) { //for all peasants that aren't doing anything
				for (int j = i + 1; j < available.size(); j++) { //get every combination of them iteratively
					pairs.add(new int[] { available.get(i)[0], available.get(j)[0] });
				}
			}
			allPairs.add(1, pairs);
			if (available.size() > 2) {
				triplets.add(new int[available.size()]);
				for (int i = 0; i < available.size(); i++)
					triplets.get(0)[i] = available.get(i)[0];
				allPairs.add(2, triplets);
			}
		}
		return allPairs;
	}

	public GameMap copyAll() {
		HashMap<Integer, Peasant> next = new HashMap<Integer, Peasant>();
		for (Peasant p : peasants.values()) {
			next.put(p.getId(), p.makeCopy());
		}
		HashMap<Integer, Resource> nextR = new HashMap<Integer, Resource>();
		for (Resource r : resources.values()) {
			nextR.put(r.id, r.makeCopy());
		}
		return new GameMap(freeAgents, next, nextR, currentFood, currentGold, currentWood);
	}

	// public Resource findNearest(Peasant p) //not sure if I would want this. game
	// should find it by itself
}
