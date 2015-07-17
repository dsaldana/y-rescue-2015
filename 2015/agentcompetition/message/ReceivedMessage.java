package message;

import problem.Problem;

public class ReceivedMessage {
	public MessageTypes msgType;
	public Problem problem;
	
	public ReceivedMessage(MessageTypes mType, Problem p) {
		msgType = mType;
		problem = p;
	}
	
	@Override
	public String toString() {
		return "MsgType " + msgType + ", problem" + problem;
	}
}
