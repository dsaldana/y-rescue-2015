package adk.sample.basic.event;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import adk.launcher.agent.TacticsAgent;
import adk.team.util.provider.VictimSelectorProvider;
import adk.team.util.provider.WorldProvider;
import comlib.event.information.MessageCivilianEvent;
import comlib.manager.MessageReflectHelper;
import comlib.message.information.MessageCivilian;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import yrescue.tactics.YRescueTacticsAmbulance;

public class BasicCivilianEvent implements MessageCivilianEvent {

    private WorldProvider provider;
    private VictimSelectorProvider vsp;
    private int sightDistance = 30000; // Default value from the sources of robocup simulator Ej: /boot/config/perception.cfg:10:perception.los.max-distance: 30000
    private EntityID entity = null;
    private YRescueTacticsAmbulance ambulance = null;
    Collection<StandardEntity> objectsInRange = null;

    public BasicCivilianEvent(WorldProvider worldProvider, VictimSelectorProvider victimSelectorProvider) {
        this.provider = worldProvider;
        this.vsp = victimSelectorProvider;
        
        this.entity = worldProvider.getOwnerID();
        this.objectsInRange = new LinkedList<StandardEntity>();
    }
    
    public BasicCivilianEvent(WorldProvider worldProvider, VictimSelectorProvider victimSelectorProvider, YRescueTacticsAmbulance ambulance) {
        this.provider = worldProvider;
        this.vsp = victimSelectorProvider;
        
        this.entity = worldProvider.getOwnerID();
        this.ambulance = ambulance;
        this.objectsInRange = new LinkedList<StandardEntity>();
    } 

    @Override
    public void receivedRadio(MessageCivilian message) {
    	if(this.entity == null && this.provider.getOwnerID() != null){ 
        	this.entity = this.provider.getOwnerID();
        	if(this.ambulance != null && this.ambulance.sightDistance > 0){
        		this.sightDistance = this.ambulance.sightDistance;
        	}
        }
    	
    	if(this.entity != null){
    		this.objectsInRange = provider.getWorld().getObjectsInRange(this.entity, this.sightDistance);
    	}
    	
    	if(!provider.getUpdateWorldData().getChangedEntities().contains(message.getHumanID()) && !objectsInRange.contains(message.getHumanID())){
    		Civilian civilian = MessageReflectHelper.reflectedMessage(this.provider.getWorld(), message);
        	this.vsp.getVictimSelector().add(civilian);
    	}
    }

    @Override
    public void receivedVoice(MessageCivilian message) {
        this.receivedRadio(message);
    }
}
