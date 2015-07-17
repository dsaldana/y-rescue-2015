package util;

import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

public class Destination {
	EntityID destinationArea;
	Pair<Integer, Integer> destPoint;
	
	/**
	 * If you don't want to inform x and y, use negative values
	 * @param destArea
	 * @param destX
	 * @param destY
	 */
	public Destination(EntityID destArea, int destX, int destY){
		destinationArea = destArea;
		destPoint = new Pair<Integer, Integer>(destX, destY);
	}
	
	/**
	 * If you don't want to inform x and y, use negative values
	 * @param destArea
	 * @param destX
	 * @param destY
	 */
	public Destination(EntityID destArea, Pair<Integer, Integer> destPoint){
		destinationArea = destArea;
		this.destPoint = destPoint;
	}
	
	
	/**
	 * Returns whether the areaID and coordinates match this destination with a small tolerance
	 * @param areaID
	 * @param destX
	 * @param destY
	 * @return
	 */
	public boolean match(EntityID areaID, int destX, int destY, int tolerance){
		return match(areaID, new Pair<Integer, Integer>(destX, destY), tolerance);
		
	}
	
	/**
	 * Returns whether the areaID and coordinates match this destination with a small tolerance
	 * @param areaID
	 * @param point
	 * @return
	 */
	public boolean match(EntityID areaID, Pair<Integer, Integer> point, int tolerance){
		if (! destinationArea.equals(areaID)){
			return false;
		}
		
		if (destPoint.first() < 0 || destPoint.second() < 0) return true;
		
		return Geometry.distance(destPoint, point) < tolerance;
		
	}
	
	/**
	 * Returns the ID of the destination area
	 * @return
	 */
	public EntityID getAreaID() {
		return destinationArea;
	}
	
	@Override
	public String toString(){
		return "Destination ID " + destinationArea + ", point: " + destPoint;
	}
	
	/*
	@Override
	public boolean equals(Object other){
		if(! (other instanceof Destination)){
			return false;
		}
		Destination d = (Destination) other;
		if (dest)
	}*/
}
