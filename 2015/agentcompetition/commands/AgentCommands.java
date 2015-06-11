package commands;

public class AgentCommands {
	/**
	 * Cannot be instantiated
	 */
	private AgentCommands() {}
	
	public static final AgentCommand MOVE = new AgentCommand("MOVE");		
	public static final AgentCommand REST = new AgentCommand("REST");		
	
	
	public static class FireFighter {
		public static final AgentCommand EXTINGUISH = new AgentCommand("EXTINGUISH");
		
		private FireFighter() {}
	}
	
	public static class Ambulance {
        public static final AgentCommand RESCUE = new AgentCommand("RESCUE");		//remove debris from person
		public static final AgentCommand LOAD = new AgentCommand("LOAD");			//get someone aboard
		public static final AgentCommand UNLOAD = new AgentCommand("UNLOAD");		//get someone off-board
		
		private Ambulance() {}
	}
	
	public static class Policeman {
        public static final AgentCommand CLEAR = new AgentCommand("CLEAR");
		
		private Policeman() {}
	}
}
