package yrescue.message.event;

import adk.team.tactics.Tactics;
import comlib.event.MessageEvent;
import rescuecore2.log.Logger;
import yrescue.message.information.MessageRecruitment;

public class MessageRecruitmentEvent implements MessageEvent<MessageRecruitment>{
	private Tactics<?> tacticsAgent;
	

    public MessageRecruitmentEvent(Tactics<?> tacticsAgent) {
        this.tacticsAgent = tacticsAgent;
    }

    public void receivedRadio(MessageRecruitment message) {
    	Logger.debug("Processing MessageRecruitment: " + message);
    	this.tacticsAgent.recruitmentManager.addReceivedRecruitmentMessage(message);
    }

    public void receivedVoice(MessageRecruitment message) {
        this.receivedRadio(message);
    }
}
