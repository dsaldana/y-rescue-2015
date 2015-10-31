package adk.sample.basic.event;

import adk.team.util.provider.VictimSelectorProvider;
import adk.team.util.provider.WorldProvider;
import comlib.event.information.MessagePoliceForceEvent;
import comlib.manager.MessageReflectHelper;
import comlib.message.information.MessagePoliceForce;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.PoliceForce;

public class BasicPoliceEvent implements MessagePoliceForceEvent {

    private WorldProvider provider;
    private VictimSelectorProvider vsp;

    public BasicPoliceEvent(WorldProvider worldProvider, VictimSelectorProvider victimSelectorProvider) {
        this.provider = worldProvider;
        this.vsp = victimSelectorProvider;
    }

    @Override
    public void receivedRadio(MessagePoliceForce message) {
    	
    	if(!provider.getUpdateWorldData().getChangedEntities().contains(message.getHumanID())){
    		Logger.debug("Policeman data not in my changeset. Adding it as victim");
    		PoliceForce policeForce = MessageReflectHelper.reflectedMessage(this.provider.getWorld(), message);
            this.vsp.getVictimSelector().add(policeForce);
    	}
    	else{
    		Logger.debug("IGNORING: Policeman is in my changeset.");
    	}
    	
    }

    @Override
    public void receivedVoice(MessagePoliceForce message) {
        this.receivedRadio(message);
    }
}
