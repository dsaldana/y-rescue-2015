package statemachine;

/**
 * Stores all states of the {@link StateMachine}
 * @author anderson
 *
 */
public class ActionStates {
	/**
	 * Cannot be instantiated
	 */
	private ActionStates() {}
	
	public static final State BURIED = new State("BURIED");		//buried in building
	public static final State BLOCKED = new State("BLOCKED");		//surrounded by blockades
	public static final State RANDOM_WALK = new State("RANDOM_WALK");		//'exploring'
    public static final State GOING_TO_TARGET = new State("GOING_TO_TARGET"); //target chosen, going for it
    
    // Recruitment states
    public static final State RECRUITMENT_GOING_TO_TARGET = new State("RECRUITMENT_GOING_TO_TARGET"); // Recruitment target chosen, going for it
    public static final State RECRUITMENT_DOING_TASK = new State("RECRUITMENT_DOING_TASK"); // Recruitment target chosen, going for it
    public static final State RECRUITMENT_WAITING_RESPONSE = new State("RECRUITMENT_WAITING_RESPONSE"); // Recruitment target chosen, going for it
	
	
	public static class FireFighter {
		public static final State EXTINGUISHING = new State("EXTINGUISHING");		//throwing water in building
        public static final State OUT_OF_WATER = new State("OUT_OF_WATER");		
		public static final State REFILLING_WATER = new State("FILLING_WATER");
		
		private FireFighter() {}
	}
	
	public static class Ambulance {
        public static final State UNBURYING = new State("UNBURYING");		//removing debris from person
		public static final State LOADING = new State("LOADING");			//getting someone aboard
		public static final State UNLOADING = new State("UNLOADING");		//getting someone off-board
		public static final State CARRYING_WOUNDED = new State("CARRYING_WOUNDED");	//transporting a person
		public static final State SEARCHING_BUILDINGS = new State("SEARCHING_BUILDINGS");		
		
		private Ambulance() {}
	}
	
	public static class Policeman {
        public static final State CLEARING = new State("CLEARING");
		public static final State SCOUTING = new State("SCOUTING");
		public static final State AWAITING_ORDERS = new State("AWAITING_ORDERS");
		
		private Policeman() {}
	}
}
