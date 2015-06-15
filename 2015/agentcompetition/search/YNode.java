package search;

import rescuecore2.standard.entities.Area;

/**
 * Represents a node for the search techniques of Y-Rescue team
 * @author anderson
 *
 */
public class YNode {
	private int x, y;
	private Area parentArea1, parentArea2;
	
	public YNode(int x, int y, Area parent1, Area parent2){
		this.x = x;
		this.y = y;
		parentArea1 = parent1;
		parentArea2 = parent2;
	}
	
	public boolean belongsTo(Area a) {
		return a == parentArea1 || a == parentArea2;
	}

	public int getX() {
		return x;
	}
	
	private int getY() {
		return y;
	}

}
