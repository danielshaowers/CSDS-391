package minimax;



import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import javafx.util.Pair;
//import minimax.MapLocation;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;

import com.sun.javafx.scene.traversal.Direction;

public class MinimaxAlphaBeta extends Agent {
	    private final int numPlys;    
	    public MinimaxAlphaBeta(int playernum, String[] args) {
	        super(playernum);
	        if(args.length < 1)
	        {
	            System.err.println("You must specify the number of plys");
	            System.exit(1);
	        }	  
	        numPlys = Integer.parseInt(args[0]);	
	    }

	    @Override
	    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
	    	
	        return middleStep(newstate, statehistory);
	    }

	    @Override
	    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
	        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
	                numPlys,
	                Double.NEGATIVE_INFINITY,
	                Double.POSITIVE_INFINITY); //returns the best child found from alphaBetaSearch
	        return bestChild.action;
	    }

	    @Override
	    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

	    }

	    @Override
	    public void savePlayerData(OutputStream os) {

	    }

	    @Override
	    public void loadPlayerData(InputStream is) {

	    }

	    /**
	     * You will implement this.
	     *
	     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
	     * and book for more information.
	     *
	     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
	     * code into other functions and methods)
	     *
	     * @param node The action and state to search from
	     * @param depth The remaining number of plys under this node
	     * @param alpha The current best value for the maximizing node from this node to the root
	     * @param beta The current best value for the minimizing node from this node to the root
	     * @return The best child of this node with updated values
	     */
	    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
	    {
	    	//calls all the children of the node and returns back node with max value
		    	 return maximumValue(node,depth,alpha,beta).getKey();
	    }
	    
	    public Pair<GameStateChild, Double> maximumValue(GameStateChild node, int depth, double alpha, double beta) {
	    	if (depth == 0)
	    		return new Pair<GameStateChild, Double>(node, node.state.getUtility());
	    	else {
	    		Pair<GameStateChild, Double> max = new Pair<GameStateChild, Double>(null, Double.NEGATIVE_INFINITY); //tracks the max child node and its utility
	    		Pair<GameStateChild, Double> temp = null; 		
	    		//determines node with max value by recursively calling all its children
	    		for (GameStateChild kid:orderChildrenWithHeuristics(node.state.getChildren())) { 
	        		//returns the utility of one leaf and the node that led to it
	        		temp = minimumValue(kid, depth-1, alpha, beta);
	        		if (temp.getValue() > max.getValue()) 
	        			max = new Pair<GameStateChild, Double>(kid, temp.getValue());
	        		
	        		//updates max eval to the highest encountered leaf so far
	        		
	        		//prunes if it's less than the best we've seen
	        		if (max.getValue() >= beta)	
	        			return max;
	        		alpha = Math.max(alpha, max.getValue()); //update if the highest utility we found > anything previously found
	      
	    		}
	        	return max;
	    	}
	    }
	    
	    public Pair<GameStateChild, Double> minimumValue(GameStateChild node, int depth, double alpha, double beta)
	    {
	    	if (depth == 0) 
	    		return new Pair<GameStateChild, Double>(node, node.state.getUtility());   	
	    	else {
	    		Pair<GameStateChild, Double> min = new Pair<GameStateChild, Double>(null, Double.POSITIVE_INFINITY);
	    		Pair<GameStateChild, Double> temp;
	    		//determines node with min value by recursively calling all its children
	    		for (GameStateChild kid : orderChildrenWithHeuristics(node.state.getChildren())) { 
	    		//stores node that team awesome is most likely to choose 
	        		temp = maximumValue(kid,depth-1,alpha,beta);       		
	        		//decides which node has lower utility i.e. which node opponent will choose
	    			if (temp.getValue() < min.getValue())
	    				min = new Pair<GameStateChild, Double>(kid, temp.getValue());	    			
	    			//prunes
	    			if (min.getValue() <= alpha) 
	    				return min;    			
	    			beta = Math.min(min.getValue(), beta);
	    		}
	        	return min; //returns the kid that leads to the best value
	    	}
	    }




	   //higher heuristic if the movement brings us closer to enemy, if we perform an attack, and if utility at the state is high
	    //multiplies heuristic by a negative if it's the enemy's turn bc they're more likely to select low heuristic value moves
	    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
	    { //the point is to order by highest utility without examining the utilities (since that takes further plies)
	        //assign each action+location a heuristic, then insertion based on the values
	    	ArrayList<HeuristicChildPair> heuristic = new ArrayList<HeuristicChildPair>(); 	    	
	    	int i = -1;
	    	int multiplier = 1; //multiply all heuristics w/ this value
	    	for (GameStateChild child: children) { //for each child gamestate
	    		heuristic.add(++i, new HeuristicChildPair(0, child));
	    		List<Integer> units;
	    		HeuristicChildPair currentHeuristic = heuristic.get(i);
	    		if (child.state.getTurnNum() % 2 == 0) // If friendly unit
	    			units = child.state.getUnitIDs();
	    		else {
	    			units = child.state.getEnemyUnitIDs();
	    			multiplier = -1;
	    		} 		
	    		HashMap<Integer, FutureUnit> futureUnits = child.state.getFutureUnits();
	    		for (Integer uID: units) { //check info about the movers
	    			Action action = child.action.get(uID);
	    			FutureUnit fu = futureUnits.get(uID);
	    			int[] enemyDists = child.state.getNearestEnemy().get(uID);
	    			if (action != null) {
	    				if (ActionType.PRIMITIVEATTACK.equals(action.getType())) //beneficial if we attack
	    					currentHeuristic.incrVal(10 * multiplier);  
	    				if (ActionType.PRIMITIVEMOVE.equals(action.getType())) { //not sure how to check direction
	    					edu.cwru.sepia.util.Direction direction = ((DirectedAction) action).getDirection();
	    					FutureUnit nearestEnemy = futureUnits.get(enemyDists[1]);
	    					int yDistToEnemy = fu.getY() - nearestEnemy.getY();
	    					int xDistToEnemy = fu.getX() - nearestEnemy.getX();
	    					//if we're moving in the direction towards the enemy
	    					if (yDistToEnemy < 0 && direction.equals(edu.cwru.sepia.util.Direction.NORTH)
	    						||(yDistToEnemy > 0 && direction.equals(edu.cwru.sepia.util.Direction.SOUTH))
	    						||(xDistToEnemy < 0 && direction.equals(edu.cwru.sepia.util.Direction.EAST))
	    						||(xDistToEnemy > 0 && direction.equals(edu.cwru.sepia.util.Direction.WEST)))
	    						currentHeuristic.incrVal(3 * multiplier); 
	    					currentHeuristic.incrVal((int)child.state.getUtility() * multiplier);		
	    				} 
	    			}  
	    		} 
	    	}  
	    	//insertion sort that uses the heuristic values to sort the list
	    	for (int j = 1; j < children.size(); j++) {
	    		Integer val = heuristic.get(j).getVal();
	    		int k = j;
	    		while (k > 1 && val < heuristic.get(k-1).getVal()) {
	    			children.set(k, children.get(k - 1));
	    			heuristic.get(k).setVal(heuristic.get(--k).getVal());
	    		}
	    		children.set(k, children.get(j));
	    		heuristic.get(k).setVal(val);
	    	}
	    	return children;
	    }
	    //added to pair heuristic to child, which will enable sorting 
	    public class HeuristicChildPair{
	    	int heuristic;
	    	GameStateChild child;
	    	public HeuristicChildPair(int heuristic, GameStateChild child) {
	    		
	    		this.heuristic = heuristic;
	    		this.child = child;
	    	}
	    	
	    	public int getVal() {
	    		return heuristic;
	    	}
	    	
	    	public void incrVal(int val) {
	    		heuristic += val;
	    	}
	    	public void setVal(int val) {
	    		heuristic = val;
	    	}
	    	public int compareTo(Object h) {
	    		if (h instanceof HeuristicChildPair) {
	    			HeuristicChildPair j = (HeuristicChildPair)h;
	    			if (j.heuristic < this.heuristic)
	    					return -1;
	    			if (j.heuristic > this.heuristic)
	    				return 1;
	    		}
	    	return 0;
	    	}
	    }
	    
	       
	  
	}

