package yrescue.statemachine;

public class StatusStates {
		private StatusStates() {}	//cannot instantiate
		
		public static final StatusState BURIED = new StatusState("BURIED");					//buried in building
		public static final StatusState STUCK = new StatusState("BLOCKED");				//surrounded by blockades
		public static final StatusState STUCK_NAVIGATION = new StatusState("STUCK_NAVIGATION");				//surrounded by blockades
		
		public static final StatusState EXPLORING = new StatusState("EXPLORING");		//'exploring'
	    public static final StatusState ACTING = new StatusState("ACTING"); 			//performing a task (is the owner of a task)
	    public static final StatusState FINALIZING = new StatusState("FINALIZING");	//finishing a task (?)
	    public static final StatusState HELPING = new StatusState("HELPING");			//performing a task (is NOT the owner of a task)
		public static final StatusState HURT = new StatusState("HURT");				//suffered damage
	    
		
}
