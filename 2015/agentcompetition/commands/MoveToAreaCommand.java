package commands;

import java.util.List;

import rescuecore2.worldmodel.EntityID;

public class MoveToAreaCommand extends AgentCommand {
	List<EntityID> path;
	
	public MoveToAreaCommand(List<EntityID> path) {
		super("MOVE");
		this.path = path;
	}
	
	@Override
	public boolean equals(Object other){
		if (! super.equals(other)) return false;
		
		if (other instanceof MoveToAreaCommand){
			return path.equals(((MoveToAreaCommand)other).path);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() + path.hashCode();
	}
	
	@Override
	public String toString(){
		return super.toString() + " path: " + path;
	}

}
