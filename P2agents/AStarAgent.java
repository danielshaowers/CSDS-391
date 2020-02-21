package HW2.src.edu.cwru.sepia.agent;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;
import java.lang.Math;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import javax.swing.plaf.synth.SynthSeparatorUI;

public class AStarAgent extends Agent {

	class MapLocation implements Comparable<MapLocation> {
        public int x, y;
        MapLocation cameFrom; //the node leading to our current
        float cost; //cost represents the heuristic value

        public MapLocation(int x, int y, MapLocation cameFrom, float cost)
        {
            this.x = x;
            this.y = y;
            this.cost = cost;
            this.cameFrom = cameFrom;
            
        }
        @Override
        public boolean equals(Object ml) {
        	if (ml instanceof MapLocation)
        		return this.x == ((MapLocation)ml).x && this.y == ((MapLocation)ml).y;
        	return false;
        }
        
        public float getCost() {
        	return cost;
        }
        
        @Override //hash code using bijective theorem
        public int hashCode() {
        	int tmp = ( y +  ((x+1)/2));
            return x +  ( tmp * tmp);
        }
        
        @Override
        public int compareTo(MapLocation compare) {
        	if (compare.getCost() < this.cost)
        		return 1;
        	if (compare.getCost() > this.cost)
        		return -1;
        	return 0;
        }
    }

    Stack<MapLocation> path = new Stack<MapLocation>();
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs

    public AStarAgent(int playernum)
    {
        super(playernum);

        System.out.println("Constructed AstarAgent");
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if(!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                townhallID = unitID;
            }
            else if(unitType.equals("footman"))
            {
                enemyFootmanID = unitID;
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }

        if(townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
    	System.out.println("running middle step. steps left is " + path.size());
        long startTime = System.nanoTime();
        long planTime = 0;
        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        if(shouldReplanPath(newstate, statehistory, path)) {
        	System.out.println("REPLANNING PATH!");
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();

        if(!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {
            // stat moving to the next step in the path
            nextLoc = path.pop();
            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }
        System.out.println("next loc is " + nextLoc);
        if(nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y))
        {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);
            
            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        } else { //there are no more moves in the stack now.
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if(townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }
            if(Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1)
            {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            }
            else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime/1e9);
        System.out.println("Total execution time: " + totalExecutionTime/1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime)/1e9);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {  //return whether there is an enemy footman at the location of our next move
        return state.getUnit(enemyFootmanID) != null && !currentPath.empty() && currentPath.peek().x == state.getUnit(enemyFootmanID).getXPosition() 
        		&& currentPath.peek().y== state.getUnit(enemyFootmanID).getYPosition();
    }

    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }
        System.out.println(startLoc.x);

        return AstarSearch(state, startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * Therefore your you need to find some possible adjacent steps which are in range 
     * and are not trees or the enemy footman.
     * Hint: Set<MapLocation> resourceLocations contains the locations of trees
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    private Stack<MapLocation> AstarSearch(State.StateView state, MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
    	path.clear(); //want to empty stack every time Astar is called
    	int nextposx, nextposy; 	//the next coordinates to move to
    	Hashtable<Integer, MapLocation> closed = new Hashtable<Integer, MapLocation>(); //this becomes unnecessary?
    	PriorityQueue<MapLocation> open = new PriorityQueue<MapLocation>(); //tracks any potential nodes
        MapLocation temp = new MapLocation(0, 0, null, 0); //the map location specified by nextpos
        open.add(start);
        while (open.size() != 0) { 
        	MapLocation current = open.poll(); //get the cheapest node
        	if (current.equals(goal)) //if the goal is found
        		return tracePath(current); //helper method that traces from goal	
        	closed.putIfAbsent(current.hashCode(), current); //add current to closed list
    		for(int x = -1; x < 2; x++) { //gets all neighbors
            	for(int y= -1; y < 2; y++) { 
            		nextposx = current.x + x; //nextpos is the next coordinate we're going to check
            		nextposy = current.y + y;
            		temp = new MapLocation(nextposx, nextposy, current, Float.MAX_VALUE); //set temp to the new coordinate  
            		//skips positions that either don't exist or is current player position. 
            		if (nextposx < xExtent && nextposy < yExtent && !state.isResourceAt(nextposx, nextposy) 
            				&& nextposx > -1 && nextposy > -1 && (enemyFootmanLoc == null || !enemyFootmanLoc.equals(temp))) { 
            			temp.cost = (float) calculateEuclidean(temp, goal) + current.cost; //f = g+h
            			//if current has never been visited, or if cheaper than what's already visited
            			MapLocation hashVal = closed.get(temp.hashCode()); //tries to find temp in hash table
            			//adds to open list if temp is cheaper than other paths to temp in closed list
            			if (hashVal == null || (temp.equals(hashVal) && temp.cost < hashVal.cost))
            					open.add(temp); 
            			}
            	}
    		}
        }
        //System.out.println("No available path.");
		//System.exit(0);
    	return path;    
    }
    
    //calculates euclidean distance given the current node and goal node
    private double calculateEuclidean(MapLocation current, MapLocation goal) {
    	return Math.sqrt(Math.pow(current.x - goal.x, 2) + Math.pow(current.y - goal.y, 2));
		
    }
    //returns whether the target destination is adjacent to our current location
    private boolean isAdjacent(MapLocation current, MapLocation target) {
    	return Math.abs(current.x - target.x) <= 1 && Math.abs(current.y - target.y) <= 1;
    }
    //trace back the path from the goal node		
    private Stack<MapLocation> tracePath(MapLocation goal) {
    	for (MapLocation parent = goal.cameFrom; parent.cameFrom != null; parent = parent.cameFrom) 
    		path.push(parent);
    	return path;
    }

    /**
     * Primitive actions take a direction (e.g. Direction.NORTH, Direction.NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}