package message;

import rescuecore2.worldmodel.EntityID;

public interface TaskMessageEncoder {
	/**
	 * Encodes a message that reports a problem
	 * @param senderID
	 * @return
	 */
	public byte[] encodeReportMessage(EntityID senderID);
	
	/**
	 * Encodes a message indicating that "senderID will engage in task"
	 * @param senderID
	 * @return
	 */
	public byte[] encodeEngageMessage(EntityID senderID);
	
	/**
	 * Encodes a message indicating that a problem has been solved
	 * @param senderID
	 * @return
	 */
	public byte[] encodeSolvedMessage(EntityID senderID);
}
