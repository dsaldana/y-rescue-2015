package yrescue.problem.blockade;
import rescuecore2.worldmodel.EntityID;

public class BlockedArea{
	
	public EntityID originID;
	public EntityID destinationID;
	public int xOrigin, yOrigin;

	public BlockedArea(EntityID originID, EntityID destinationID, int xOrigin, int yOrigin) {
		this.originID = originID;
		this.destinationID = destinationID;
		this.xOrigin = xOrigin;
		this.yOrigin = yOrigin;
	}
	
	public EntityID getOriginID(){
		return originID;
	}
	
	@Override
	public int hashCode() {
		return String.format("%s|%s|%s|%s", originID.getValue(), destinationID.getValue(), xOrigin, yOrigin).hashCode();
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof BlockedArea) {
			BlockedArea theOther = (BlockedArea) other;
			return originID.equals(theOther.originID) && xOrigin == theOther.xOrigin && yOrigin == theOther.yOrigin && destinationID.equals(theOther.destinationID);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return String.format("BlockedArea: origin=%s, x=%d, y=%d, destination=%s", originID, xOrigin, yOrigin, destinationID);
	}
	
}
