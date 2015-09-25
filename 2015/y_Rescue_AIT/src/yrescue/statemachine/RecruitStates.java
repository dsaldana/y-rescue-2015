package yrescue.statemachine;

public class RecruitStates {
		private RecruitStates() {}	//cannot instantiate
		
		public static final RecruitState NOT_RECRUITING = new RecruitState("NOT_RECRUITING");	//idle, regarding the recruiting process
		public static final RecruitState REQUESTING = new RecruitState("REQUESTING");			//started recruiting process
		public static final RecruitState COMMITTING = new RecruitState("COMMITTING");			//offering help (waits for accept or dismiss)
		public static final RecruitState ENGAGING = new RecruitState("ENGAGING");				//help offer accepted, will engage in task
	    //public static final State DISMISSED = new State("DISMISSED"); 			//help offer dismissed 
		
		
		// Recruitment states
	   // public static final State RECRUITMENT_GOING_TO_TARGET = new State("RECRUITMENT_GOING_TO_TARGET"); // Recruitment target chosen, going for it
	   // public static final State RECRUITMENT_DOING_TASK = new State("RECRUITMENT_DOING_TASK"); // Recruitment target chosen, going for it
	   // public static final State RECRUITMENT_WAITING_RESPONSE = new State("RECRUITMENT_WAITING_RESPONSE"); // Recruitment target chosen, going for it
	   // public static final State RECRUITMENT_NOTHING = new State("RECRUITMENT_NOTHING"); // Recruitment doing nothing
}
