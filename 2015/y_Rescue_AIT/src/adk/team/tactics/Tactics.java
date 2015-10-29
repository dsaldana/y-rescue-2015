package adk.team.tactics;

import adk.launcher.agent.TacticsAgent;
import adk.team.action.Action;
import adk.team.util.provider.WorldProvider;
import comlib.manager.MessageManager;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import yrescue.heatmap.HeatMap;
import yrescue.message.information.MessageBlockedArea;
import yrescue.message.recruitment.RecruitmentManager;
import yrescue.problem.blockade.BlockedArea;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Tactics<E extends Human> implements WorldProvider<E> {

    public boolean pre;

    protected TacticsAgent<?> tacticsAgent;
    public StandardWorldModel world;
    public StandardWorldModel model;
    public Config config;
    public int time;
    public ChangeSet changed;
    
    public static final int VICTIM_REPORT_INTERVAL = 7;
    public static final int BUILDING_REPORT_INTERVAL = 7;
    public static final int BLOCKED_AREA_REPORT_INTERVAL = 3;
    
    public Map<Human, Integer> lastVictimReport;
    public Map<Building, Integer> lastBuildingReport;
    public Map<BlockedArea, Integer> lastBlockedAreaReport;
    
    public int sightDistance;
    public HeatMap heatMap;

    public long startProcessTime;

    public EntityID agentID;
    public StandardEntity location;

    public List<Refuge> refugeList;
    public EntityID target;
    public RecruitmentManager recruitmentManager;

    public abstract String getTacticsName();

    public abstract void preparation(Config config, MessageManager messageManager);

    public abstract void registerEvent(MessageManager manager);

    public abstract Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager);
    
    public abstract Action failsafeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager);
    
    public abstract HeatMap initializeHeatMap();
    

    public void ignoreTimeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    }

    public void registerProvider(MessageManager manager) {
    }
    
    public void registerTacticsAgent(TacticsAgent<?> t) {
    	this.tacticsAgent = t;
    }

    @Override
    public Config getConfig() {
        return this.config;
    }

    @Override
    public int getCurrentTime() {
        return this.time;
    }

    @Override
    public ChangeSet getUpdateWorldData() {
        return this.changed;
    }

    @Override
    public StandardWorldModel getWorld() {
        return this.world;
    }

    @Override
    public EntityID getOwnerID() {
        return this.agentID;
    }

    public StandardEntity getOwnerLocation() {
        return this.location;
    }

    @Override
    public List<Refuge> getRefugeList() {
        return this.refugeList;
    }

	public void sendAfterEvent() {
		
	}
	
	
	public void reportBlockedArea(BlockedArea b, MessageManager manager, int currentTime){
		if(!lastBlockedAreaReport.containsKey(b) || currentTime - lastBlockedAreaReport.get(b) > BLOCKED_AREA_REPORT_INTERVAL){
			Logger.debug("Will report my BlockedArea. " + b);
    		lastBlockedAreaReport.put(b, currentTime);
    		manager.addSendMessage(new MessageBlockedArea(this, b.getOriginID(), b.destinationID));
		}
		else{
			Logger.debug("My blockedArea was reported a short time ago. I won't report it again.");
		}
	}
	
	/**
	 * Sends message about a civilian if it is new or a sufficient interval has passed
	 * @param c
	 * @param manager
	 * @param currentTime
	 */
	public void reportCivilian(Civilian c, MessageManager manager, int currentTime){
        if(c.getBuriedness() > 0) {
        	if(!lastVictimReport.containsKey(c) || currentTime - lastVictimReport.get(c) > VICTIM_REPORT_INTERVAL){
        		Logger.trace("Will report victim. " + c);
        		lastVictimReport.put(c, currentTime);
        		manager.addSendMessage(new MessageCivilian(c));
        	}
        	else{
        		Logger.trace("Won't report victim, cuz victim report interval has not passed." + c);
        	}
        }
        else{
        	Logger.trace("Won't report civilian, cuz it is not buried." + c);
        }
	}
	
	/**
	 * Sends a message 'bout a building when it is on fire and has not been reported a long time
	 * Also, sends message 'bout completely burnt out buildings
	 * @param b
	 * @param manager
	 * @param currentTime
	 */
	public void reportBuilding(Building b, MessageManager manager, int currentTime){
		if(b.isOnFire()) {
			
			if(!lastBuildingReport.containsKey(b) || currentTime - lastVictimReport.get(b) > BUILDING_REPORT_INTERVAL){
				Logger.trace("Will report. " + b);
	    		lastBuildingReport.put(b, currentTime);
				manager.addSendMessage(new MessageBuilding(b));
			}
			
			else{
        		Logger.trace("Won't report building, cuz building report interval has not passed." + b);
        	}
        }
		

        if(b.getFierynessEnum().equals(StandardEntityConstants.Fieryness.BURNT_OUT)){
        	Logger.trace("Removing completely burnt Building from heatMap" + b);
        	heatMap.removeEntityID(b.getID());
        	
        	if(!lastBuildingReport.containsKey(b) || currentTime - lastVictimReport.get(b) > BUILDING_REPORT_INTERVAL){
				Logger.trace("Will report. " + b);
	    		lastBuildingReport.put(b, currentTime);
				manager.addSendMessage(new MessageBuilding(b));
			}
			
			else{
        		Logger.trace("Won't report building, cuz building report interval has not passed." + b);
        	}
        }
	}
}
