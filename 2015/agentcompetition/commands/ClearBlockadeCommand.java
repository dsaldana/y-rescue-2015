package commands;

import rescuecore2.worldmodel.EntityID;

public class ClearBlockadeCommand extends AgentCommand {
	EntityID blockID;
	
	public ClearBlockadeCommand(EntityID blockID) {
		super("CLEAR");
		this.blockID = blockID;
	}
	
	@Override
	public boolean equals(Object other){
		if (! super.equals(other)) return false;
		
		if (other instanceof ClearBlockadeCommand){
			return blockID.equals(((ClearBlockadeCommand)other).blockID);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() + blockID.hashCode();
	}
	
	@Override
	public String toString(){
		return super.toString() + " Blockade: " + blockID;
	}
}
