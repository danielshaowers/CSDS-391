package P4Agent;
//represents a peasant. rename later
public class Daniel {

		private int id;
		private Position position;
		private int gold = 0;
		private int wood = 0;
		private int cost = 0;
		public Daniel(int id, Position pos, int goldAmt, int woodAmt, int cost) {
			this.id = id;
			position = pos;
			gold = goldAmt;
			wood = woodAmt;
			this.cost = cost;
		}
	
		public Daniel makeCopy() {
			return new Daniel(id, position, gold, wood, cost);
		}
		public int getCost() {
			return cost;
		}
		public void setCost(int c) {
			this.cost = c;
		}
		public int getId() {
			return id;
		}
		
		public void setId(int id) {
			this.id = id;
		}
		
		public Position getPosition() {
			return position;
		}
		public void setPosition(Position position) {
			this.position = position;
		}
		
		public int getGold() {
			return gold;
		}
		
		public void setGold(int numGold) {
			this.gold = numGold;
		}
		
		public int getWood() {
			return wood;
		}
		
		public void setWood(int numWood) {
			this.wood = numWood;
		}
		
		public boolean hasResource() {
			return getGold() + getWood() > 0;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Daniel) {
				Daniel peasant = (Daniel) obj;
				return peasant.getId() == id && peasant.getWood() == wood && 
						peasant.getGold() == gold && peasant.getPosition().equals(position);
			}
			return false;
		}
		@Override 
		public int hashCode() {
			return 31 * (id + gold + wood + position.hashCode()); 
		}
}
