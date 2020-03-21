package P4Agent;
//represents gold
public class Resource {
	public boolean isGold;
	public int remaining = 0;
	public int x;
	public int y;
	public int id; 
	
	public Resource(int x, int y, int cheese, int id, boolean gold) {
		isGold = gold;
		this.x = x;
		this.y = y;
		this.id = id;
		remaining = cheese;
	}
	
	public Resource makeCopy() {
		return new Resource(x, y, remaining, id, isGold);
	}
}
