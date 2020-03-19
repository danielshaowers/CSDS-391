package P4Agent;
//represents gold
public class Nacho {
	public boolean isGold;
	public int cheeseRemaining = 0;
	public int x;
	public int y;
	public int id; 
	
	public Nacho(int x, int y, int cheese, int id, boolean gold) {
		isGold = gold;
		this.x = x;
		this.y = y;
		this.id = id;
		cheeseRemaining = cheese;
	}
	
	public Nacho makeCopy() {
		return new Nacho(x, y, cheeseRemaining, id, isGold);
	}
}
