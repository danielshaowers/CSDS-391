package minimax;

import java.util.ArrayList;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

	
	public class FutureUnit{
	private int x;
	private int y;
	private int id;
	private int hp;
	private boolean good = true;
	private int turnNum;
	
	public FutureUnit (int xLoc, int yLoc, int id, int hp, int turnNum, boolean good) {
		x = xLoc;
		y = yLoc;
		this.id = id;
		this.hp = hp;
		this.turnNum = turnNum;
		this.good = good;
	}
	
	public int getTurn() {
		return turnNum;
	}
	public void attacked(Action attack, int damage) {
		if (attack.getType().equals(ActionType.PRIMITIVEATTACK)) {
			this.hp = hp - damage;
		}
	}
	//duplicates current values but increases turnNum by 1
	public FutureUnit duplicate() {
		return new FutureUnit(x, y, id, hp, turnNum + 1, good);
	}
	public void moved(Action move) {
		if (move.getType().equals(ActionType.PRIMITIVEMOVE)) {
			DirectedAction da = (DirectedAction) move;
			x = x + da.getDirection().xComponent();
			y = y + da.getDirection().yComponent();
		}
	}
	
	@Override
	public boolean equals(Object id) {
		return ((Integer)id).equals(id);
	}
	
	public boolean getGood() {
		return good;
	}
	
	public int getX() {
		return x;
	}

	public void setXY(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getHp() {
		return hp;
	}

	public void setHp(int hp) {
		this.hp = hp;
	}
	
}
