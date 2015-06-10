package statemachine;

/**
 * Stores all states of the {@link StateMachine}
 * @author anderson
 *
 */
public class States {
	private States() {}
	
	public static final State RANDOM_WALK = new State("RANDOM_WALK");
    public static final State GOING_TO_TARGET = new State("GOING_TO_TARGET"); 
	
	
	public static class FireFighter {
        public static final State OUT_OF_WATER = new State("OUT_OF_WATER");
		public static final State REFILLING_WATER = new State("FILLING_WATER");
		
		private FireFighter() {}
	}
	
	public static class Ambulance {
        public static final State UNBURYING = new State("UNBURYING");
		public static final State LOADING = new State("LOADING");
		public static final State UNLOADING = new State("UNLOADING");
		public static final State CARRYING_WOUNDED = new State("CARRYING_WOUNDED");
		
		private Ambulance() {}
	}
	
	public static class Policeman {
        public static final State CLEARING = new State("CLEARING");
		public static final State SCOUTING = new State("SCOUTING");
		public static final State AWAITING_ORDERS = new State("AWAITING_ORDERS");
		
		private Policeman() {}
	}
}
