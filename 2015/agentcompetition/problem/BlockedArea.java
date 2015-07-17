package problem;

import message.MessageTypes;
import rescuecore2.worldmodel.EntityID;

public class BlockedArea extends Problem{
	
	public EntityID roadID;
	public int repairCost;

	public BlockedArea(EntityID roadID, int repairCost, int lastUpdate) {
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
	
	public boolean equals(BlockedArea other){
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
			"%d,%d,%d,%d", MessageTypes.REPORT_BLOCKED_AREA.ordinal(),senderID.getValue(),roadID.getValue(),repairCost
		);
		
		return message.getBytes();
	}
	
	public byte[] encodeEngageMessage(EntityID senderID){
		String message = String.format(
			"%d,%d,%d,%d",	MessageTypes.ENGAGE_BLOCKED_AREA.ordinal(),senderID.getValue(),roadID.getValue(),repairCost
		);
		
		return message.getBytes();
		
	}
	
	public byte[] encodeSolvedMessage(EntityID senderID){
		String message = String.format(
			"%d,%d,%d",	MessageTypes.SOLVED_BLOCKED_AREAS.ordinal(),senderID.getValue(),roadID.getValue()
		);
		
		return message.getBytes();
	}

	@Override
	public String toString() {
		return String.format("BlockedRoad: id=%d, repairCost=%d, updTime=%d", getEntityID().getValue(), repairCost, getUpdateTime());
	}
	
}
