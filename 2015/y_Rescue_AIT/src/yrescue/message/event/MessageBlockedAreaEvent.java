package yrescue.message.event;

import comlib.event.MessageEvent;
import adk.team.util.provider.WorldProvider;
import yrescue.message.information.MessageBlockedArea;
import yrescue.problem.blockade.BlockedArea;
import yrescue.problem.blockade.BlockedAreaSelectorProvider;

public class MessageBlockedAreaEvent implements MessageEvent<MessageBlockedArea>{

    private WorldProvider<?> provider;
    private BlockedAreaSelectorProvider basp;

    public MessageBlockedAreaEvent(WorldProvider<?> worldProvider, BlockedAreaSelectorProvider blockedAgentSelectorProvider) {
        this.provider = worldProvider;
        this.basp = blockedAgentSelectorProvider;
    }

    public void receivedRadio(MessageBlockedArea message) {
    	this.basp.getBlockedAreaSelector().add(new BlockedArea(message.roadID, message.x, message.y));
    }

    public void receivedVoice(MessageBlockedArea message) {
        this.receivedRadio(message);
    }
}


