package search;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;

/**
 * Represents a node for the search techniques of Y-Rescue team
 * @author anderson
 *
 */
public class YNode {
	private int x, y;
	private String label;
	private Pair<Area, Area> parentAreas;
	
	public YNode(int x, int y, Area parent1, Area parent2){
		this.x = x;
		this.y = y;
		parentAreas = new Pair<Area, Area>(parent1, parent2);
		label = String.format("%s-%s", parent1.getID(), parent2.getID());
	}
	
	public boolean belongsTo(Area a) {
		return a == getParentAreas().first() || a == getParentAreas().second();
	}

	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}

	public String getLabel() {
		return label;
	}

	public Pair<Area, Area> getParentAreas() {
		return parentAreas;
	}
	
	/**
	 * Returns whether the nodes have an area in common
	 * @param other
	 * @return
	 */
	public boolean isInSameAreaOf(YNode other){
		return (
			parentAreas.first().equals(other.getParentAreas().first()) ||
			parentAreas.first().equals(other.getParentAreas().second()) ||
			parentAreas.second().equals(other.getParentAreas().first()) ||
			parentAreas.second().equals(other.getParentAreas().second())
		);
	}

	@Override
	/**
	 * YNodes are equal when they have the same coordinate
	 */
	public boolean equals(Object other){
		if (other == null){
			return false;
		}
		if (! (other instanceof YNode)){
			return false;
		}
		YNode otherNode = (YNode) other;
		
		return x == otherNode.getX() && y == otherNode.getY();
	}
	
	@Override
	public int hashCode(){
		return x + y;
	}
	
	@Override
	public String toString(){
		return String.format("YNode %s @ (%d,%d)", label, x, y);
				
	}
}
