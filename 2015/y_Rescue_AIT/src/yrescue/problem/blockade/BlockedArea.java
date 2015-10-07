package yrescue.problem.blockade;
import rescuecore2.worldmodel.EntityID;

public class BlockedArea{
	
	public EntityID areaID;
	public int x, y;

	public BlockedArea(EntityID areaID, int x, int y) {
		this.areaID = areaID;
		this.x = x;
		this.y = y;
	}
	
	public EntityID getEntityID(){
		return areaID;
	}
	
	@Override
	public int hashCode() {
		return String.format("%s|%s%s", areaID.getValue(), x, y).hashCode();
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof BlockedArea) {
			BlockedArea theOther = (BlockedArea) other;
			return areaID.equals(theOther.areaID) && x == theOther.x && y == theOther.y;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return String.format("BlockedArea: id=%d, x=%d, y=%d", getEntityID().getValue(), x, y);
	}
	
}
