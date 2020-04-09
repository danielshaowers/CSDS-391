package PEAsants;
//represents gold mine and trees
public class Resource {
	public boolean isGold;
	public int remaining = 0;
	public int x;
	public int y;
	public int id; 
	//tracks the resources on the map, allows us to track how much resource is remaining
	public Resource(int x, int y, int cheese, int id, boolean gold) {
		isGold = gold;
		this.x = x;
		this.y = y;
		this.id = id;
		remaining = cheese;
	}
	
	public Resource makeCopy() { //duplicates resource with all the same values
		return new Resource(x, y, remaining, id, isGold);
	}
}
