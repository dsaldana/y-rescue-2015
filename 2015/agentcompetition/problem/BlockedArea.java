package problem;

import message.MessageTypes;
import rescuecore2.worldmodel.EntityID;

public class BlockedArea extends Problem{
	
	public EntityID areaID;
	public int x, y;

	public BlockedArea(EntityID areaID, int x, int y, int lastUpdate) {
		this.areaID = areaID;
		this.x = x;
		this.y = y;
		setUpdateTime(lastUpdate);
		markUnsolved(lastUpdate);
	}
	
	@Override
	public EntityID getEntityID(){
		return areaID;
	}
	
	@Override
	public int hashCode() {
		return areaID.getValue() + x + y;
	}
	
	public boolean equals(BlockedArea other){
		return areaID.equals(other.areaID) && x == other.x && y == other.y;
	}
	
	/**
	 * Updates the attributes of this blocked road
	 * @param repairCost
	 * @param time
	 *
	public void update(int repairCost, int time){
		this.repairCost = repairCost;
		setUpdateTime(time);
	}*/
	
	public byte[] encodeReportMessage(EntityID senderID){
		String message = String.format(
			"%d,%d,%d,%d,%d", 
			MessageTypes.REPORT_BLOCKED_AREA.ordinal(), senderID.getValue(), areaID.getValue(), x, y
		);
		
		return message.getBytes();
	}
	
	public byte[] encodeEngageMessage(EntityID senderID){
		String message = String.format(
			"%d,%d,%d,%d,%d",	
			MessageTypes.ENGAGE_BLOCKED_AREA.ordinal(), senderID.getValue(), areaID.getValue(), x, y
		);
		
		return message.getBytes();
		
	}
	
	public byte[] encodeSolvedMessage(EntityID senderID){
		String message = String.format(
			"%d,%d,%d,%d,%d", 
			MessageTypes.SOLVED_BLOCKED_AREAS.ordinal(), senderID.getValue(), areaID.getValue(), x, y
		);
		
		return message.getBytes();
	}

	@Override
	public String toString() {
		return String.format("BlockedArea: id=%d, x=%d, y=%d, updTime=%d", getEntityID().getValue(), x, y, getUpdateTime());
	}
	
}
