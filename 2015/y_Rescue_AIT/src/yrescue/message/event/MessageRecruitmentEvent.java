package yrescue.message.event;

import adk.team.tactics.Tactics;
import adk.team.util.provider.WorldProvider;
import comlib.event.MessageEvent;
import rescuecore2.log.Logger;
import yrescue.message.information.MessageHydrant;
import yrescue.message.information.MessageRecruitment;
import yrescue.tactics.YRescueTacticsFire;

public class MessageRecruitmentEvent implements MessageEvent<MessageRecruitment>{
	private Tactics<?> tacticsAgent;
	

    public MessageRecruitmentEvent(Tactics<?> tacticsAgent) {
        this.tacticsAgent = tacticsAgent;
    }

    public void receivedRadio(MessageRecruitment message) {
    	Logger.debug("Processing MessageRecruitment: " + message);
    	//firefighter.busyHydrantIDs.put(message.hydrantID, message.timestep_free);
    	//this.basp.getBlockedAreaSelector().add(new BlockedArea(message.originID, message.destinationID, message.xOrigin, message.yOrigin));
    }

    public void receivedVoice(MessageRecruitment message) {
        this.receivedRadio(message);
    }
}
