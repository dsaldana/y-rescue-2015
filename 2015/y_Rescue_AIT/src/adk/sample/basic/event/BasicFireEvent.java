package adk.sample.basic.event;

import adk.team.util.provider.VictimSelectorProvider;
import adk.team.util.provider.WorldProvider;
import comlib.event.information.MessageFireBrigadeEvent;
import comlib.manager.MessageReflectHelper;
import comlib.message.information.MessageFireBrigade;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.FireBrigade;

public class BasicFireEvent implements MessageFireBrigadeEvent {

    private WorldProvider provider;
    private VictimSelectorProvider vsp;

    public BasicFireEvent(WorldProvider worldProvider, VictimSelectorProvider victimSelectorProvider) {
        this.provider = worldProvider;
        this.vsp = victimSelectorProvider;
    }

    @Override
    public void receivedRadio(MessageFireBrigade message) {
    	
    	if(!provider.getUpdateWorldData().getChangedEntities().contains(message.getHumanID())){
    		Logger.debug("FireBrigade data not in my changeset. Adding it as victim");
    		FireBrigade fireBrigade = MessageReflectHelper.reflectedMessage(this.provider.getWorld(), message);
            this.vsp.getVictimSelector().add(fireBrigade);
    	}
    	else{
    		Logger.debug("IGNORING: FireBrigade is in my changeset.");
    	}
    }

    @Override
    public void receivedVoice(MessageFireBrigade message) {
        this.receivedRadio(message);
    }
}
