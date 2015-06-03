package problem;

import message.MessageType;
import rescuecore2.worldmodel.EntityID;

public class BurningBuilding extends Problem {
	
	public EntityID buildingID;
	public int brokenness;
	public int fieryness;
	public int temperature;
	
	public BurningBuilding(EntityID buildingID, int brokenness, int fieryness, int temperature, int lastUpdate) {
		this.buildingID = buildingID;
		update(brokenness, fieryness, temperature, lastUpdate);
		markUnsolved(lastUpdate);
		
	}
	
	@Override
	public int hashCode() {
		return buildingID.getValue();
	}
	
	/**
	 * Returns whether the IDs of the burning buildings are equal
	 * @param other
	 * @return boolean
	 */
	public boolean equals(BurningBuilding other){
		return buildingID.equals(other);
	}
	
	/**
	 * Updates the attributes of this burning building
	 * @param brokenness
	 * @param fieryness
	 * @param temperature
	 * @param time
	 */
	public void update(int brokenness, int fieryness, int temperature, int time){
		this.brokenness = brokenness;
		this.fieryness = fieryness;
		this.temperature = temperature;
		setUpdateTime(time);
	}
	
	@Override
	public byte[] encodeReportMessage(EntityID senderID) {
		String message = String.format(
			"%d,%d,%d,%d,%d,%d", 
			MessageType.REPORT_BURNING_BUILDING.ordinal(), senderID.getValue(), buildingID.getValue(), 
			brokenness,fieryness,temperature
		);
		
		return message.getBytes();
	}

	@Override
	public byte[] encodeEngageMessage(EntityID senderID) {
		String message = String.format(
			"%d,%d,%d,%d,%d,%d", 
			MessageType.ENGAGE_BURNING_BUILDING.ordinal(), senderID.getValue(), buildingID.getValue(),
			brokenness,fieryness,temperature
		);
		
		return message.getBytes();
	}

	@Override
	public byte[] encodeSolvedMessage(EntityID senderID) {
		String message = String.format(
			"%d,%d,%d", 
			MessageType.SOLVED_BURNING_BUILDING.ordinal(), senderID.getValue(),buildingID.getValue()
		);
		
		return message.getBytes();
	}

	@Override
	public EntityID getEntityID() {
		return buildingID;
	}

	@Override
	public String toString() {
		return String.format("BurningBuilding: id=%d, brk=%d, fir=%d, temp=%d, time=%d", getEntityID().getValue(), brokenness, fieryness, temperature, getUpdateTime()); 
	}

	

}
