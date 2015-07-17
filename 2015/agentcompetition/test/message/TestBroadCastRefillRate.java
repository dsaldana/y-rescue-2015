package test.message;

import static org.junit.Assert.*;
import message.MessageReceiver;
import message.MessageTypes;
import message.ReceivedMessage;
import message.misc.BroadCastRefillRateMessage;
import message.misc.BroadCastRefillRateMessageHandler;

import org.junit.Test;

import problem.BlockedArea;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

public class TestBroadCastRefillRate {

	@Test
	public void testEncoding() {
		
		int testRefillRate = 1500;
		int senderID = 1;
		
		String reportMsg = String.format(
			"%d,%d,%d", 
			MessageTypes.BROADCAST_REFILL_RATE.ordinal(), senderID, testRefillRate
		);
		assertArrayEquals(
			reportMsg.getBytes(), 
			BroadCastRefillRateMessageHandler.encodeMessage(
					new EntityID(senderID), testRefillRate
			)
		);
	}
	
	@Test
	public void testDecoding() {
		AKSpeak msg;
		BroadCastRefillRateMessage received;
		int refillRate = 1987;
		EntityID me = new EntityID(1);
		int msgTime = 5;
		
		//reportMessage
		msg = new AKSpeak(me, msgTime, 1, BroadCastRefillRateMessageHandler.encodeMessage(me, refillRate));
		received = (BroadCastRefillRateMessage) MessageReceiver.decodeMessage(msg);
		
		assertEquals(MessageTypes.BROADCAST_REFILL_RATE, received.msgType);
		assertEquals(msgTime, msg.getTime());
		assertEquals(refillRate, received.getRefillRate());
	}

}
