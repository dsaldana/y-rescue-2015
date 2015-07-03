package message;

public enum MessageType {
	//report types
	REPORT_BLOCKED_ROAD,
	REPORT_WOUNDED_HUMAN,
	REPORT_BURNING_BUILDING,
	
	//engage types
	ENGAGE_BLOCKED_ROAD,
	ENGAGE_WOUNDED_HUMAN,
	ENGAGE_BURNING_BUILDING,
	
	//solved types
	SOLVED_BLOCKED_ROAD,
	SOLVED_WOUNDED_HUMAN,
	SOLVED_BURNING_BUILDING,
	
	//misc types
	BROADCAST_REFILL_RATE	//allows fire fighters to communicate the refill rate
}
