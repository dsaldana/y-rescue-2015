package test.message;

import static org.junit.Assert.*;
import message.MessageReceiver;
import message.MessageType;
import message.ReceivedMessage;

import org.junit.Test;

import problem.BlockedRoad;
import problem.BurningBuilding;
import problem.WoundedHuman;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

/**
 * Assumes that message encoding is working properly
 *
 */
public class TestMessageReceiver {

	
	@Test
	public void testReceivingBurningBuilding() {
		AKSpeak msg;
		ReceivedMessage received;
		BurningBuilding decoded;
		EntityID me = new EntityID(1);
		EntityID buildingID = new EntityID(2);
		int msgTime = 5;
		
		BurningBuilding b = new BurningBuilding(buildingID, 10, 3, 150, msgTime);
		
		//reportMessage
		msg = new AKSpeak(me, msgTime, 1, b.encodeReportMessage(me));
		received = MessageReceiver.decodeMessage(msg);
		decoded = (BurningBuilding) received.problem;
		
		assertEquals(MessageType.REPORT_BURNING_BUILDING, received.msgType);
		assertEquals(b.getEntityID(), decoded.getEntityID());
		assertEquals(b.brokenness, decoded.brokenness);
		assertEquals(b.fieryness, decoded.fieryness);
		assertEquals(b.temperature, decoded.temperature);
		assertEquals(msgTime, decoded.getUpdateTime());
		
		//engageMessage
		msg = new AKSpeak(me, msgTime, 1, b.encodeEngageMessage(me));
		received = MessageReceiver.decodeMessage(msg);
		decoded = (BurningBuilding) received.problem;
		assertEquals(MessageType.ENGAGE_BURNING_BUILDING, received.msgType);
		assertEquals(b.getEntityID(), decoded.getEntityID());
		assertEquals(b.brokenness, decoded.brokenness);
		assertEquals(b.fieryness, decoded.fieryness);
		assertEquals(b.temperature, decoded.temperature);
		assertEquals(msgTime, decoded.getUpdateTime());
		
		//solvedMessage
		msg = new AKSpeak(me, msgTime, 1, b.encodeSolvedMessage(me));
		received = MessageReceiver.decodeMessage(msg);
		decoded = (BurningBuilding) received.problem;
		assertEquals(MessageType.SOLVED_BURNING_BUILDING, received.msgType);
		assertEquals(b.getEntityID(), decoded.getEntityID());
		assertTrue(decoded.isSolved());
		assertEquals(msgTime, decoded.getUpdateTime());
	}
	
	@Test
	public void testReceivingWoundedHuman() {
		AKSpeak msg;
		ReceivedMessage received;
		WoundedHuman decoded;
		EntityID me = new EntityID(1);
		int msgTime = 5;
		
		WoundedHuman h = new WoundedHuman(new EntityID(2), new EntityID(3), 20, 8500, 50, msgTime);
		
		//reportMessage
		msg = new AKSpeak(me, msgTime, 1, h.encodeReportMessage(me));
		received = MessageReceiver.decodeMessage(msg);
		decoded = (WoundedHuman) received.problem;
		
		assertEquals(MessageType.REPORT_WOUNDED_HUMAN, received.msgType);
		assertEquals(h.getEntityID(), decoded.getEntityID());
		assertEquals(h.buriedness, decoded.buriedness);
		assertEquals(h.damage, decoded.damage);
		assertEquals(h.health, decoded.health);
		assertEquals(h.position, decoded.position);
		assertEquals(msgTime, decoded.getUpdateTime());
		
		//engageMessage
		msg = new AKSpeak(me, msgTime, 1, h.encodeEngageMessage(me));
		received = MessageReceiver.decodeMessage(msg);
		decoded = (WoundedHuman) received.problem;
		assertEquals(MessageType.ENGAGE_WOUNDED_HUMAN, received.msgType);
		assertEquals(h.getEntityID(), decoded.getEntityID());
		assertEquals(h.buriedness, decoded.buriedness);
		assertEquals(h.damage, decoded.damage);
		assertEquals(h.health, decoded.health);
		assertEquals(h.position, decoded.position);
		assertEquals(msgTime, decoded.getUpdateTime());
		
		//solvedMessage
		msg = new AKSpeak(me, msgTime, 1, h.encodeSolvedMessage(me));
		received = MessageReceiver.decodeMessage(msg);
		decoded = (WoundedHuman) received.problem;
		assertEquals(MessageType.SOLVED_WOUNDED_HUMAN, received.msgType);
		assertEquals(h.getEntityID(), decoded.getEntityID());
		assertTrue(decoded.isSolved());
		assertEquals(msgTime, decoded.getUpdateTime());
	}
	
	@Test
	public void testReceivingBlockedRoad() {
		AKSpeak msg;
		ReceivedMessage received;
		BlockedRoad decoded;
		EntityID me = new EntityID(1);
		int msgTime = 5;
		
		BlockedRoad b = new BlockedRoad(new EntityID(2), 50, msgTime);
		
		//reportMessage
		msg = new AKSpeak(me, msgTime, 1, b.encodeReportMessage(me));
		received = MessageReceiver.decodeMessage(msg);
		decoded = (BlockedRoad) received.problem;
		
		assertEquals(MessageType.REPORT_BLOCKED_ROAD, received.msgType);
		assertEquals(b.getEntityID(), decoded.getEntityID());
		assertEquals(b.repairCost, decoded.repairCost);
		assertEquals(msgTime, decoded.getUpdateTime());
		
		//engageMessage
		msg = new AKSpeak(me, msgTime, 1, b.encodeEngageMessage(me));
		received = MessageReceiver.decodeMessage(msg);
		decoded = (BlockedRoad) received.problem;
		assertEquals(MessageType.ENGAGE_BLOCKED_ROAD, received.msgType);
		assertEquals(b.getEntityID(), decoded.getEntityID());
		assertEquals(b.repairCost, decoded.repairCost);
		assertEquals(msgTime, decoded.getUpdateTime());
		
		//solvedMessage
		msg = new AKSpeak(me, msgTime, 1, b.encodeSolvedMessage(me));
		received = MessageReceiver.decodeMessage(msg);
		decoded = (BlockedRoad) received.problem;
		assertEquals(MessageType.SOLVED_BLOCKED_ROAD, received.msgType);
		assertEquals(b.getEntityID(), decoded.getEntityID());
		assertTrue(decoded.isSolved());
		assertEquals(msgTime, decoded.getUpdateTime());
		
	}

}
