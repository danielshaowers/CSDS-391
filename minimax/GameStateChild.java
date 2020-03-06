package minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.State;
import java.util.HashMap;
import java.util.Map;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.State;

import java.util.Map;

/**
 * Do not change this class.
 */
public class GameStateChild {
    //* This is set of unit actions that produced the game state
    public Map<Integer, Action> action;
    //* This is the game state resulting from the specified set of actions
    public GameState state;

    public GameStateChild(State.StateView state)
    {
        action = null;
        this.state = new GameState(state);
    }

    public GameStateChild(Map<Integer, Action> action, GameState state)
    {
        this.action = action;
        this.state = state;
    }
}
/**
 * Do not change this class.
 */
/*public class GameStateChild {
    //* This is set of unit actions that produced the game state
    public Map<Integer, Action> action;
    //* This is the game state resulting from the specified set of actions
    public GameState state;
    public Trees trees;

    public GameStateChild(State.StateView state, Trees trees)
    {
        action = null;
        this.state = new GameState(state, trees);
    }
    public GameStateChild(GameState state, Trees trees) {
    	this.state = state;
    	this.trees = trees;
    }

    public GameStateChild(Map<Integer, Action> action, GameState state)
    {
        this.action = action;
        this.state = state;
    }
    public GameStateChild(Map<Integer, Action> action, GameState state, Trees trees) {
    	this.action = action;
    	this.state = state;
    	this.trees = trees;
    }
} */
