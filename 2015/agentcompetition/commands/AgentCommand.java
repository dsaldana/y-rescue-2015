package commands;

public class AgentCommand {
	private String cmdName;
	
	public AgentCommand(String name){
		cmdName = name;
	}
	
	public String getName(){
		return cmdName;
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof AgentCommand){
			return cmdName == ((AgentCommand)other).getName();
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return cmdName.hashCode();
	}
	
	public String toString(){
		return cmdName;
	}
}
