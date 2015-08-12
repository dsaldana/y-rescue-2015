package message.misc;

import message.MessageTypes;
import rescuecore.commands.Command;
import rescuecore2.worldmodel.EntityID;

public class BroadCastRefillRateMessageHandler {
	public static byte[] encodeMessage(EntityID senderID, int refillRate){
		String message = String.format(
			"%d,%d,%d", 
			MessageTypes.BROADCAST_REFILL_RATE.ordinal(), senderID.getValue(), refillRate 
		);
			
		return message.getBytes();
	}
	
	public static BroadCastRefillRateMessageHandler decodeMessage(String[] msgParts, Command cmd){
		return null;
	}
	
	private BroadCastRefillRateMessageHandler() {}	//one shall not instantiate
}
