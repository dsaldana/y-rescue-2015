package yrescue.message.event;

import adk.team.util.provider.WorldProvider;
import comlib.event.MessageEvent;
import yrescue.message.information.MessageHydrant;
import yrescue.tactics.YRescueTacticsFire;

public class MessageHydrantEvent implements MessageEvent<MessageHydrant>{
	private YRescueTacticsFire firefighter;

    public MessageHydrantEvent(YRescueTacticsFire firefighter) {
        this.firefighter = firefighter;
    }

    public void receivedRadio(MessageHydrant message) {
    	firefighter.busyHydrants.put(message.hydrantID, message.timestep_free);
    	//this.basp.getBlockedAreaSelector().add(new BlockedArea(message.originID, message.destinationID, message.xOrigin, message.yOrigin));
    }

    public void receivedVoice(MessageHydrant message) {
        this.receivedRadio(message);
    }
}
