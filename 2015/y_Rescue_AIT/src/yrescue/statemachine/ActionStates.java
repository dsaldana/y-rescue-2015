package yrescue.statemachine;

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
	
	public static final ActionState MOVING_TO_TARGET = new ActionState("MOVING_TO_TARGET");
	
	
	public static class FireFighter {
		public static final ActionState EXTINGUISHING = new ActionState("EXTINGUISHING");		//throwing water in building
        public static final ActionState OUT_OF_WATER = new ActionState("OUT_OF_WATER");		
		public static final ActionState REFILLING_WATER = new ActionState("FILLING_WATER");
		
		private FireFighter() {}
	}
	
	public static class Ambulance {
        public static final ActionState UNBURYING = new ActionState("UNBURYING");		//removing debris from person
		public static final ActionState LOADING = new ActionState("LOADING");			//getting someone aboard
		public static final ActionState UNLOADING = new ActionState("UNLOADING");		//getting someone off-board
		public static final ActionState CARRYING_WOUNDED = new ActionState("CARRYING_WOUNDED");	//transporting a person
		public static final ActionState SEARCHING_BUILDINGS = new ActionState("SEARCHING_BUILDINGS");		
		
		private Ambulance() {}
	}
	
	public static class Policeman {
        public static final ActionState CLEARING = new ActionState("CLEARING");
		public static final ActionState SCOUTING = new ActionState("SCOUTING");
		public static final ActionState AWAITING_ORDERS = new ActionState("AWAITING_ORDERS");
		
		private Policeman() {}
	}
}
