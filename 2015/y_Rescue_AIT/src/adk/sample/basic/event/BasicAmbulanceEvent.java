package adk.sample.basic.event;

import adk.team.util.provider.VictimSelectorProvider;
import adk.team.util.provider.WorldProvider;
import comlib.event.information.MessageAmbulanceTeamEvent;
import comlib.manager.MessageReflectHelper;
import comlib.message.information.MessageAmbulanceTeam;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Civilian;

public class BasicAmbulanceEvent implements MessageAmbulanceTeamEvent {

    private WorldProvider provider;
    private VictimSelectorProvider vsp;

    public BasicAmbulanceEvent(WorldProvider worldProvider, VictimSelectorProvider victimSelectorProvider) {
        this.provider = worldProvider;
        this.vsp = victimSelectorProvider;
    }

    @Override
    public void receivedRadio(MessageAmbulanceTeam message) {
        if(message.getHumanID().getValue() != this.provider.getOwnerID().getValue() ) {
        	
        	if(!provider.getUpdateWorldData().getChangedEntities().contains(message.getHumanID())){
        		Logger.debug("AmbulanceTeam data not in my changeset. Adding it as victim");
        		AmbulanceTeam ambulanceTeam = MessageReflectHelper.reflectedMessage(this.provider.getWorld(), message);
                this.vsp.getVictimSelector().add(ambulanceTeam);
        	}
        	else{
        		Logger.debug("IGNORING: AmbulanceTeam is in my changeset.");
        	}
        	
        }
    }

    @Override
    public void receivedVoice(MessageAmbulanceTeam message) {
        this.receivedRadio(message);
    }


}
