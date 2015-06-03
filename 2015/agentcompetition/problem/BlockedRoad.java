package problem;

import message.MessageType;
import rescuecore2.worldmodel.EntityID;

public class BlockedRoad extends Problem{
	
	public EntityID roadID;
	public int repairCost;

	public BlockedRoad(EntityID roadID, int repairCost, int lastUpdate) {
		this.roadID = roadID;
		update(repairCost, lastUpdate);
		markUnsolved(lastUpdate);
	}
	
	@Override
	public EntityID getEntityID(){
		return roadID;
	}
	
	@Override
	public int hashCode() {
		return roadID.getValue();
	}
	
	public boolean equals(BlockedRoad other){
		return roadID.equals(other.roadID);
	}
	
	/**
	 * Updates the attributes of this blocked road
	 * @param repairCost
	 * @param time
	 */
	public void update(int repairCost, int time){
		this.repairCost = repairCost;
		setUpdateTime(time);
	}
	
	public byte[] encodeReportMessage(EntityID senderID){
		String message = String.format(
			"%d,%d,%d,%d", MessageType.REPORT_BLOCKED_ROAD.ordinal(),senderID.getValue(),roadID.getValue(),repairCost
		);
		
		return message.getBytes();
	}
	
	public byte[] encodeEngageMessage(EntityID senderID){
		String message = String.format(
			"%d,%d,%d,%d",	MessageType.ENGAGE_BLOCKED_ROAD.ordinal(),senderID.getValue(),roadID.getValue(),repairCost
		);
		
		return message.getBytes();
		
	}
	
	public byte[] encodeSolvedMessage(EntityID senderID){
		String message = String.format(
			"%d,%d,%d",	MessageType.SOLVED_BLOCKED_ROAD.ordinal(),senderID.getValue(),roadID.getValue()
		);
		
		return message.getBytes();
	}

	@Override
	public String toString() {
		return String.format("BlockedRoad: id=%d, repairCost=%d, updTime=%d", getEntityID().getValue(), repairCost, getUpdateTime());
	}
	
}
