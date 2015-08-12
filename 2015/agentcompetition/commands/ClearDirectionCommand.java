package commands;

import rescuecore2.worldmodel.EntityID;

public class ClearDirectionCommand extends AgentCommand {
	int x, y;
	public ClearDirectionCommand(int x, int y) {
		super("CLEAR");
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object other){
		if (! super.equals(other)) return false;
		
		if (other instanceof ClearDirectionCommand){
			ClearDirectionCommand theOther = (ClearDirectionCommand) other;
			
			return x == theOther.x && y == theOther. y;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return String.format("%d%d%d", super.hashCode(), x, y).hashCode();
	}
	
	@Override
	public String toString(){
		return super.toString() + " Coords: " + x + "," + y;
	}
}
