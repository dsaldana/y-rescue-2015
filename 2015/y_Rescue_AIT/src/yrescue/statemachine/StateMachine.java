package yrescue.statemachine;

import org.apache.log4j.MDC;

//TODO colocar historico de estados

/**
 * Implements a Finite State Machine
 * @author anderson
 *
 */
public class StateMachine {
	private State current;
	
	public StateMachine(State initial){
		setState(initial);
	}
	
	public void setState(State newState){
		current = newState;
		MDC.put("state", current);	//TODO checar se eh consistente p/ todos os agentes
	}
	
	public State currentState(){
		return current;
	}
	
	public State getCurrentState(){
		return currentState();
	}
}
