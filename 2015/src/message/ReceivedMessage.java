package message;

import problem.Problem;

public class ReceivedMessage {
	public MessageType msgType;
	public Problem problem;
	
	public ReceivedMessage(MessageType mType, Problem p) {
		msgType = mType;
		problem = p;
	}
	
	@Override
	public String toString() {
		return "MsgType " + msgType + ", problem" + problem;
	}
}
