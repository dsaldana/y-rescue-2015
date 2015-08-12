package test.message;

import static org.junit.Assert.*;
import message.MessageTypes;

import org.junit.Test;

import problem.BlockedArea;
import problem.BurningBuilding;
import problem.WoundedHuman;
import rescuecore2.worldmodel.EntityID;

public class TestProblemEncoding {

	@Test
	public void testEncodeWoundedHuman() {
		EntityID me = new EntityID(1);
		EntityID humanID = new EntityID(2);
		EntityID humanPos = new EntityID(3);
		int buriedness = 20, hp = 10000, dmg = 30, lastUpdate = 1;
		WoundedHuman h = new WoundedHuman(humanID, humanPos, buriedness, hp, dmg, lastUpdate);
		
		//report message
		String reportMsg = String.format(
			"%d,%d,%d,%d,%d,%d,%d", 
			MessageTypes.REPORT_WOUNDED_HUMAN.ordinal(), me.getValue(), humanID.getValue(),humanPos.getValue(),
			buriedness,hp,dmg,lastUpdate
		);
		assertArrayEquals(reportMsg.getBytes(), h.encodeReportMessage(me));
		
		//engage message
		String engageMsg = String.format(
			"%d,%d,%d,%d,%d,%d,%d", 
			MessageTypes.ENGAGE_WOUNDED_HUMAN.ordinal(), me.getValue(), humanID.getValue(),humanPos.getValue(),
			buriedness,hp,dmg,lastUpdate
		);
		assertArrayEquals(engageMsg.getBytes(), h.encodeEngageMessage(me));


		//engage message
		String solvedMsg = String.format(
			"%d,%d,%d", 
			MessageTypes.SOLVED_WOUNDED_HUMAN.ordinal(), me.getValue(), humanID.getValue()
		);
		assertArrayEquals(solvedMsg.getBytes(), h.encodeSolvedMessage(me));
		
	}
	
	@Test
	public void testEncodeBurningBuilding() {
		EntityID me = new EntityID(1);
		EntityID buildingID = new EntityID(2);
		int brokenness = 20, fieryness = 3, temperature = 150, lastUpdate = 1;
		BurningBuilding b = new BurningBuilding(buildingID, brokenness, fieryness, temperature, lastUpdate);
		
		//report message
		String reportMsg = String.format(
			"%d,%d,%d,%d,%d,%d", 
			MessageTypes.REPORT_BURNING_BUILDING.ordinal(), me.getValue(), buildingID.getValue(),
			brokenness, fieryness, temperature
		);
		assertArrayEquals(reportMsg.getBytes(), b.encodeReportMessage(me));
		
		//engage message
		String engageMsg = String.format(
			"%d,%d,%d,%d,%d,%d", 
			MessageTypes.ENGAGE_BURNING_BUILDING.ordinal(), me.getValue(), buildingID.getValue(),
			brokenness, fieryness, temperature
		);
		assertArrayEquals(engageMsg.getBytes(), b.encodeEngageMessage(me));


		//engage message
		String solvedMsg = String.format(
			"%d,%d,%d", 
			MessageTypes.SOLVED_BURNING_BUILDING.ordinal(), me.getValue(), buildingID.getValue()
		);
		assertArrayEquals(solvedMsg.getBytes(), b.encodeSolvedMessage(me));
	}
	
	@Test
	public void testEncodeBlockedRoad() {
		EntityID me = new EntityID(1);
		EntityID roadID = new EntityID(2);
		int x = 30, y = 2111, lastUpdate = 20;
		BlockedArea b = new BlockedArea(roadID, x, y, lastUpdate);
		
		//report message
		String reportMsg = String.format(
			"%d,%d,%d,%d,%d", 
			MessageTypes.REPORT_BLOCKED_AREA.ordinal(), me.getValue(), roadID.getValue(), x, y
		);
		assertArrayEquals(reportMsg.getBytes(), b.encodeReportMessage(me));
		
		//engage message
		String engageMsg = String.format(
			"%d,%d,%d,%d,%d", 
			MessageTypes.ENGAGE_BLOCKED_AREA.ordinal(), me.getValue(), roadID.getValue(), x, y
		);
		assertArrayEquals(engageMsg.getBytes(), b.encodeEngageMessage(me));


		//engage message
		String solvedMsg = String.format(
			"%d,%d,%d,%d,%d", 
			MessageTypes.SOLVED_BLOCKED_AREAS.ordinal(), me.getValue(), roadID.getValue(), x, y
		);
		assertArrayEquals(solvedMsg.getBytes(), b.encodeSolvedMessage(me));
	}

}
