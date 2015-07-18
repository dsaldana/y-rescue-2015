package test.agentcommand;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import commands.AgentCommand;
import commands.ClearBlockadeCommand;
import commands.ClearDirectionCommand;
import rescuecore2.worldmodel.EntityID;

public class TestClearCommands {

	@Test
	public void testEqualsInListOfParentClass() {
		EntityID blockID = new EntityID(98);
		int x = 390, y = 987;
		
		ClearBlockadeCommand cbc = new ClearBlockadeCommand(blockID);
		ClearDirectionCommand cdc = new ClearDirectionCommand(x, y);
		
		List<AgentCommand> theCommands = new LinkedList<>();
		theCommands.add(cbc);
		theCommands.add(cdc);
		
		//creates new objects with same attributes, should be equal
		cbc = new ClearBlockadeCommand(blockID);
		cdc = new ClearDirectionCommand(x, y);
		
		assertTrue(cbc.equals(theCommands.get(0)));
		assertTrue(cdc.equals(theCommands.get(1)));
		
		assertFalse(cbc.equals(theCommands.get(1)));
		assertFalse(cdc.equals(theCommands.get(0)));
		
		AgentCommand command = cbc;
		assertTrue(command.equals(new ClearBlockadeCommand(blockID)));
		
		command = cdc;
		assertTrue(command.equals(new ClearDirectionCommand(x, y)));
		
		
	}

}
