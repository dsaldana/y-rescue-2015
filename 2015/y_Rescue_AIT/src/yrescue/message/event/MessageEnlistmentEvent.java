package yrescue.message.event;

import adk.team.tactics.Tactics;
import comlib.event.MessageEvent;
import rescuecore2.log.Logger;
import yrescue.message.information.MessageEnlistment;

public class MessageEnlistmentEvent implements MessageEvent<MessageEnlistment>{
	private Tactics<?> tacticsAgent;
	

    public MessageEnlistmentEvent(Tactics<?> tacticsAgent) {
        this.tacticsAgent = tacticsAgent;
    }

    public void receivedRadio(MessageEnlistment message) {
    	Logger.debug("Processing MessageEnlistment: " + message);
    	this.tacticsAgent.recruitmentManager.addReceivedEnlistmentMessage(message);
    }

    public void receivedVoice(MessageEnlistment message) {
        this.receivedRadio(message);
    }

}
