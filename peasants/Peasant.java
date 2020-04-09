package PEAsants;
//represents a peasant. allows us to track changes in new gamestates easily
public class Peasant {

		private int id;
		private Position position;
		private int gold = 0;
		private int wood = 0;
		private int turnsRemaining = 0;
		public Peasant(int id, Position pos, int goldAmt, int woodAmt, int turnsRemaining) {
			this.id = id;
			position = pos;
			gold = goldAmt;
			wood = woodAmt;
			this.turnsRemaining = turnsRemaining;
		}
		public int getTurns() {
			return turnsRemaining;
		}
		public void setTurns(int turns) {
			turnsRemaining = turns;
		}
	
		public Peasant makeCopy() { //duplicates peasant with same values but different object
			return new Peasant(id, position, gold, wood, turnsRemaining);
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
			if (obj instanceof Peasant) {
				Peasant peasant = (Peasant) obj;
				return peasant.getId() == id && peasant.getWood() == wood && 
						peasant.getGold() == gold && peasant.getPosition().equals(position);
			}
			return false;
		}
		@Override 
		public int hashCode() {
			return 31*(gold + wood + position.hashCode()); 
		}
}
