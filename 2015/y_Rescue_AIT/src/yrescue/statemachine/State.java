package yrescue.statemachine;

/**
 * Represents a state of the {@link StateMachine}
 * @author anderson
 *
 */
public class State {
	private String stateName;
	
	public State(String name){
		stateName = name;
	}
	
	public String getName(){
		return stateName;
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof State){
			return stateName == ((State)other).getName();
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return stateName.hashCode();
	}
	
	public String toString(){
		return stateName;
	}
}
