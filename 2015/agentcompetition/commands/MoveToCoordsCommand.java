package commands;

import java.util.List;

import rescuecore2.worldmodel.EntityID;

public class MoveToCoordsCommand extends AgentCommand {
	List<EntityID> path;
	int x, y;
	
	public MoveToCoordsCommand(List<EntityID> path, int x, int y) {
		super("MOVE");
		this.path = path;
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object other){
		if (! super.equals(other)) return false;
		
		if (other instanceof MoveToCoordsCommand){
			MoveToCoordsCommand theOther = (MoveToCoordsCommand) other;
			return path.equals(theOther.path) && x == theOther.x  && y == theOther.y;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() + path.hashCode() + x + y;
	}
	
	@Override
	public String toString(){
		return String.format("%s to coords(%d,%d) of path: %s", super.toString(), x, y, path);
	}

}
