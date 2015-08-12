package message.misc;

import message.MessageTypes;
import message.ReceivedMessage;


public class BroadCastRefillRateMessage extends ReceivedMessage {
	
	private int refillRate;
	
	public BroadCastRefillRateMessage(int refillRate){
		super(MessageTypes.BROADCAST_REFILL_RATE, null);
		this.refillRate = refillRate;
	}
	
	@Override
	public String toString() {
		return "MsgType " + msgType + ", refillRate" + refillRate;
	}
	
	
	public int getRefillRate(){
		return refillRate;
	}
	

}
