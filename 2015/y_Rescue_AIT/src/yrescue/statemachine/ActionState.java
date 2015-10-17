package yrescue.statemachine;

public class ActionState extends State {

	public ActionState(String name) {
		super(name);
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof ActionState){
			return getName().equals(((ActionState)other).getName());
		}
		return false;
	}
	
	public String toString(){
		return "Action: " + super.toString();
	}

}
