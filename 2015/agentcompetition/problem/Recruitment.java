package problem;

import message.MessageType;
import message.TaskType;
import rescuecore2.worldmodel.EntityID;

/**
 * Encapsulates a problem related to a wounded civilian
 * Attributes are public
 * @author anderson
 *
 */
public class Recruitment extends Problem {

	public EntityID requestEntity;		// the entity that requested the recruitment
	public EntityID responseEntity;		// the entity that sends a response
	public EntityID position;			// id of the place where this request came from
	
	public TaskType taskType;				//how much the civilian is buried
	public MessageType messageType;	//the civilian's HP
	
	/**
	 * Constructs a Recruitment and sets its attributes 
	 * @param humanID
	 * @param humanPosition
	 * @param buriedness
	 * @param hp
	 * @param dmg
	 * @param lastUpdate
	 */
	public Recruitment(EntityID requestEntity, EntityID responseEntity, EntityID position, TaskType taskType, MessageType responseType, int lastUpdate){
		this.requestEntity = requestEntity;
		this.responseEntity = responseEntity;
		
		update(position, taskType, responseType, lastUpdate);
		markUnsolved(lastUpdate);
	}

	@Override
	public int hashCode() {
		return requestEntity.getValue();
	}
	
	/**
	 * Returns whether the ID of the WoundedCivilians are equal
	 * @param other
	 * @return boolean
	 */
	public boolean equals(Recruitment other){
		return requestEntity.equals(other.requestEntity);
	}
	
	/**
	 * Updates the attributes of this wounded civilian
	 * @param buriedness
	 * @param hp
	 * @param dmg
	 * @param time
	 */
	public void update(EntityID position, TaskType taskType, MessageType responseType, int lastUpdate) {
		this.position = position;
		this.taskType = taskType;
		this.messageType = responseType;
		setUpdateTime(lastUpdate);
	}
	
	public TaskType getTaskType(){
		return this.taskType;
	}
	
	public MessageType getMessageType(){
		return this.messageType;
	}
	
	public EntityID getPosition(){
		return this.position;
	}
	
	public EntityID getToEntityID(){
		return this.responseEntity;
	}

	@Override
	public byte[] encodeReportMessage(EntityID senderID) {
		
		// MSG_TYPE, TO, FROM, 
		
		String message = String.format(
			"%d,%d,%d,%d,%d", 
			messageType.ordinal(), senderID.getValue(), requestEntity.getValue(), taskType.ordinal(), position.getValue()
		);
		
		return message.getBytes();
	}
	
	@Override
	public byte[] encodeEngageMessage(EntityID senderID) {
		return encodeReportMessage(senderID);
	}

	@Override
	public byte[] encodeSolvedMessage(EntityID senderID) {
		return encodeReportMessage(senderID);
	}

	@Override
	public EntityID getEntityID() {
		return requestEntity;
	}

	@Override
	public String toString() {
		return String.format("Recruitment: req_id=%s, res_id=%s, pos=%s, task=%s, resp=%s, time=%d", getEntityID(), responseEntity, position, taskType, messageType, getUpdateTime());
	}

}
