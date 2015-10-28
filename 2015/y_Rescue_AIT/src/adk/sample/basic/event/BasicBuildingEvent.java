package adk.sample.basic.event;

import adk.team.util.provider.BuildingSelectorProvider;
import adk.team.util.provider.WorldProvider;
import comlib.event.information.MessageBuildingEvent;
import comlib.manager.MessageReflectHelper;
import comlib.message.information.MessageBuilding;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Building;

public class BasicBuildingEvent implements MessageBuildingEvent{

    private WorldProvider provider;
    private BuildingSelectorProvider bsp;

    public BasicBuildingEvent(WorldProvider worldProvider, BuildingSelectorProvider buildingSelectorProvider) {
        this.provider = worldProvider;
        this.bsp = buildingSelectorProvider;
    }

    @Override
    public void receivedRadio(MessageBuilding message) {
    	Building b = (Building) provider.getWorld().getEntity(message.getBuildingID());
    	if(provider.getUpdateWorldData().getChangedEntities().contains(b.getID())){
    		Logger.trace("Skipping building found in ChangeSet: " + b);
    	}
    	else {
    		Logger.trace("" + b + " not in changeset, updating info and adding to targets");
    		b = MessageReflectHelper.reflectedMessage(this.provider.getWorld(), message);
    	}
    	
        this.bsp.getBuildingSelector().add(b);
    }

    @Override
    public void receivedVoice(MessageBuilding message) {
        this.receivedRadio(message);
    }
}
