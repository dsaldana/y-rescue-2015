package yrescue.statemachine;

public class RecruitState extends State {

	public RecruitState(String name) {
		super(name);
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof RecruitState){
			return getName() == ((RecruitState)other).getName();
		}
		return false;
	}
	
	public String toString(){
		return "Recruitment: " + super.toString();
	}

}
