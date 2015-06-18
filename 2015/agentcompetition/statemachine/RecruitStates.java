package statemachine;

public class RecruitStates {
		private RecruitStates() {}	//cannot instantiate
		
		public static final State NOT_RECRUITING = new State("NOT_RECRUITING");	//idle, regarding the recruiting process
		public static final State REQUESTING = new State("REQUESTING");			//started recruiting process
		public static final State COMMITTING = new State("COMMITTING");			//offering help (waits for accept or dismiss)
		public static final State ENGAGING = new State("ENGAGING");				//help offer accepted, will engage in task
	    //public static final State DISMISSED = new State("DISMISSED"); 			//help offer dismissed 
		
}
