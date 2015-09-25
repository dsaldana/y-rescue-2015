package yrescue.statemachine;

public class StatusState extends State {

	public StatusState(String name) {
		super(name);
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof StatusState){
			return getName() == ((StatusState)other).getName();
		}
		return false;
	}
	
	public String toString(){
		return "Status: " + super.toString();
	}

}
