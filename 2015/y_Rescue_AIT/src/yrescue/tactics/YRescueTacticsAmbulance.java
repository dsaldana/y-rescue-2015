package yrescue.tactics;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;

import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.MDC;

//import adf.util.map.PositionUtil;
import adk.team.util.graph.PositionUtil;
import adk.sample.basic.event.BasicAmbulanceEvent;
import adk.sample.basic.event.BasicCivilianEvent;
import adk.sample.basic.event.BasicFireEvent;
import adk.sample.basic.event.BasicPoliceEvent;
import adk.sample.basic.tactics.BasicTacticsAmbulance;
import adk.sample.basic.util.BasicRouteSearcher;
import adk.team.action.Action;
import adk.team.action.ActionLoad;
import adk.team.action.ActionMove;
import adk.team.action.ActionRescue;
import adk.team.action.ActionRest;
import adk.team.action.ActionUnload;
import adk.team.util.RouteSearcher;
import adk.team.util.VictimSelector;
import comlib.manager.MessageManager;
import comlib.message.information.MessageAmbulanceTeam;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import comlib.message.information.MessageFireBrigade;
import comlib.message.information.MessagePoliceForce;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import yrescue.heatmap.HeatMap;
import yrescue.heatmap.HeatNode;
import yrescue.kMeans.KMeans;
import yrescue.message.event.MessageEnlistmentEvent;
import yrescue.message.event.MessageRecruitmentEvent;
import yrescue.message.information.MessageBlockedArea;
import yrescue.message.information.MessageRecruitment;
import yrescue.message.information.Task;
import yrescue.message.recruitment.RecruitmentManager;
import yrescue.problem.blockade.BlockedArea;
import yrescue.statemachine.ActionStates;
import yrescue.statemachine.StateMachine;
import yrescue.util.DistanceSorter;
import yrescue.util.GeometricUtil;
import yrescue.util.PathUtil;
import yrescue.util.RouteCacheKey;
import yrescue.util.YRescueVictimSelector;
import yrescue.util.target.HumanTarget;

public class YRescueTacticsAmbulance extends BasicTacticsAmbulance {

	protected final int EXPLORE_TIME_STEP_TRESH = 14;
	protected int EXPLORE_TIME_LIMIT = EXPLORE_TIME_STEP_TRESH;
	protected StateMachine stateMachine = null;
	protected int timeoutAction = 0;
	protected Set<Road> impassableRoadList = new HashSet<>();
	protected int EXPLORE_AREA_SIZE_TRESH = 10000010;
	protected List<EntityID> clusterToVisit;
	protected List<EntityID> clusterCentroids;
	protected EntityID clusterCenter;
	protected Map<RouteCacheKey, List<EntityID>> routeBreadthFirstCache;
	protected List<Integer> targetBurriednessHist;
	
	private final boolean DEBUG = false;
	
	//protected ActionStates.Ambulance states = new ActionStates.Ambulance();
	
    @Override
    public String getTacticsName() {
        return "Y-Rescue Ambulance";
    }

    @Override
    public void preparation(Config config, MessageManager messageManager) {
    	long prepStart = System.currentTimeMillis();
    	
    	this.stateMachine = new StateMachine(ActionStates.Ambulance.EXPLORING);
    	routeBreadthFirstCache = PathUtil.getRouteCache();
    	this.routeSearcher = new BasicRouteSearcher(this, routeBreadthFirstCache);
    	this.victimSelector = new YRescueVictimSelector(this, this.routeSearcher);
    	
    	this.recruitmentManager = new RecruitmentManager();
    	
    	List<Task> myAvailableTask = new LinkedList<Task>();
    	myAvailableTask.add(Task.RESCUE);
    	this.recruitmentManager.initRecruitmentManager(this.agentID, myAvailableTask, messageManager, world);
    	
    	MDC.put("agent", this);
    	
    	clusterToVisit = new LinkedList<EntityID>();
    	List<StandardEntity> ambulanceList = new ArrayList<StandardEntity>(this.getWorld().getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));
    	KMeans kmeans = new KMeans(ambulanceList.size());
    	Map<EntityID, EntityID> kmeansResult = kmeans.calculatePartitions(this.getWorld());
    	
    	clusterCentroids = kmeans.getPartitions();
    	
    	ambulanceList.sort(new Comparator<StandardEntity>() {
			@Override
			public int compare(StandardEntity o1, StandardEntity o2) {
				return Integer.compare(o1.getID().getValue(), o2.getID().getValue());
			}
		});
    	
    	clusterCentroids.sort(new Comparator<EntityID>() {
			@Override
			public int compare(EntityID o1, EntityID o2) {
				return Integer.compare(o1.getValue(), o2.getValue());
			}
		});
    	
    	if(ambulanceList.size() == clusterCentroids.size()){
    		int pos = -1;
    		for(int i = 0; i < ambulanceList.size(); i++){
    			if(me.getID().getValue() == ambulanceList.get(i).getID().getValue()){
    				pos = i;
    				break;
    			}
    		}
    		
    		if(pos != -1){
    			clusterCenter = clusterCentroids.get(pos);
        		final Set<Map.Entry<EntityID, EntityID>> entries = kmeansResult.entrySet();

        		for (Map.Entry<EntityID, EntityID> entry : entries) {
        		    EntityID key = entry.getKey();
        		    EntityID partition= entry.getValue();

        		    if(partition.getValue() == clusterCenter.getValue()){
        		    	clusterToVisit.add(key);
        		    }
        		}	
    		}
    	}

    	Logger.debug("Cluster to visit :" + clusterToVisit);
    	if(clusterToVisit.size() > 0){
    		this.target = clusterCenter; //clusterToVisit.get(0);
    		this.stateMachine = new StateMachine(ActionStates.Ambulance.GOING_TO_CLUSTER_LOCATION);
    	}
    	long secsToProcess = (System.currentTimeMillis() - prepStart);
    	Logger.info(">>> Ambulance ready. Preparation took(ms): " + secsToProcess);
    }

    @Override
    public VictimSelector initVictimSelector() {
        return new YRescueVictimSelector(this);
    }

    @Override
    public RouteSearcher initRouteSearcher() {
        return new BasicRouteSearcher(this, routeBreadthFirstCache);
    }

    @Override
    public void registerEvent(MessageManager manager) {
        manager.registerEvent(new BasicCivilianEvent(this, this, this));
        manager.registerEvent(new BasicAmbulanceEvent(this, this));
        manager.registerEvent(new BasicFireEvent(this, this));
        manager.registerEvent(new BasicPoliceEvent(this, this));
        manager.registerEvent(new MessageRecruitmentEvent(this));
        manager.registerEvent(new MessageEnlistmentEvent(this));
    }

    @Override
    public void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager) {
        for (EntityID next : updateWorldInfo.getChangedEntities()) {
            StandardEntity entity = this.getWorld().getEntity(next);
            //Logger.trace("I'm seeing: " + entity);
            
            if(entity instanceof Civilian) {
            	Civilian c = (Civilian) entity;
            	Logger.trace(String.format("It's a Civilian. HP: %d, B'ness: %d", c.getHP(), c.getBuriedness()));
            	
            	boolean isOnFire = false;
            	if(world.getEntity(c.getPosition()) instanceof Building){
            		Building b = (Building) world.getEntity(c.getPosition());
            		if(b.isOnFire()) isOnFire = true;
            	}
            	
            	if(c.getBuriedness() > 0 && c.getHP() > 0 && !isOnFire) {
	                this.victimSelector.add(c);
	                manager.addSendMessage(new MessageCivilian(c));
	                Logger.trace("  added to victimSelector and sent a message reporting it");
            	}
            	else{
            		this.victimSelector.remove(c);
            	}
            }
            else if(entity instanceof Human) {
            	Human h = (Human) entity;
            	Logger.trace(String.format("It's a Human. HP: %d, B'ness: %d", h.getHP(), h.getBuriedness()));
            	
            	boolean isOnFire = false;
            	if(world.getEntity(h.getPosition()) instanceof Building){
            		Building b = (Building) world.getEntity(h.getPosition());
            		if(b.isOnFire()) isOnFire = true;
            	}
            	
            	if(h.getBuriedness() > 0 && h.getHP() > 0 && !isOnFire){
            		this.victimSelector.add(h);
            		Logger.trace("  added " + h + "to victimSelector.");
            	}
            	else{
            		this.victimSelector.remove(h);
            	}
                
            }
            else if(entity instanceof Building) {
                Building b = (Building) entity;
                if(b.isOnFire()) {
                    manager.addSendMessage(new MessageBuilding(b));
                    Logger.trace("Removing burning building from heatMap " + b);
                    //if(b.isTemperatureDefined() && b.getTemperature() > 40 && b.isFierynessDefined() && b.getFieryness() < 4 && b.isBrokennessDefined() && b.getBrokenness() > 10) {
                    heatMap.removeEntityID(b.getID());
                    //}
                }
                if(b.getFierynessEnum().equals(StandardEntityConstants.Fieryness.BURNT_OUT)){
                	Logger.trace("Removing COMPLETELY burnt building from heatMap" + b);
                	heatMap.removeEntityID(b.getID());
                }
            }
            /*else if(entity instanceof Blockade) {
                Blockade blockade = (Blockade) entity;
                Road r = (Road) this.world.getEntity(blockade.getPosition());
                manager.addSendMessage(new MessageRoad(r, blockade, false));
                this.impassableRoadList.add(r);
            }*/
        }
    }
    
    @Override
    public void ignoreTimeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    	this.organizeUpdateInfo(currentTime, updateWorldData, manager);
    	Logger.info(String.format(
			"AGENT AMBULANCE IGNORE TIME %d HP: %d, B'ness: %d, Dmg: %d, Direction: %d, SmOnBrd? %s, Target: %s", 
			me.getID().getValue(), me.getHP(), me.getBuriedness(), me.getDamage(), me.getDirection(), someoneOnBoard(), this.target
		));
    	
        if(this.me.getBuriedness() > 0) {
        	Logger.info("I'm buried at " + me.getPosition());

        	AmbulanceTeam ambulanceTeam = (AmbulanceTeam) this.me();
            manager.addSendMessage(new MessageAmbulanceTeam(ambulanceTeam, MessageAmbulanceTeam.ACTION_REST, null));
        }
    	
    };
    
    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        MDC.put("location", location());
        
        /* === ---- === *
         *     MISC     *
         * === ---- === */
        
        Logger.info(String.format(
			"AGENT AMBULANCE %d HP: %d, B'ness: %d, Dmg: %d, Direction: %d, SmOnBrd? %s, Target: %s", 
			me.getID().getValue(), me.getHP(), me.getBuriedness(), me.getDamage(), me.getDirection(), someoneOnBoard(), this.target
		));
        
        this.recruitmentManager.setMessageManager(manager);

        /*
        if(me.getID().getValue() != 941331988){
        	if(this.recruitmentManager.getRecruitmentState() == RecruitmentManager.RecruitmentStates.NOTHING){
        		if(this.recruitmentManager.isRecruitmentAvailable()){
        			this.recruitmentManager.setRecruitmentState(RecruitmentManager.RecruitmentStates.ENLISTING);
        			MessageRecruitment msg = this.recruitmentManager.getNearestRecruitment();
        			float utility = (float) world.getDistance(me.getPosition(), new EntityID(msg.positionID));
        			this.recruitmentManager.createNewEnlistmentMsg(msg, utility, time);
            	}
        	}
        	if(this.recruitmentManager.getRecruitmentState() == RecruitmentManager.RecruitmentStates.ENLISTING){
        		if(this.recruitmentManager.isEnlistmentAvailable()){
        			Logger.info("Enlistment agent enlisted - " + this.recruitmentManager.isMyEnlistmentSelected());
        		}
        	}
        }
        */
        
        if(DEBUG){
        	heatMap.writeMapToFile();
        	printHumanTargets();
        	writeClusters();
        }

        /* === -------- === *
         *   Basic actions  *
         * === -------- === */
        
        // Check for buriedness and tries to extinguish fire in a close building
        if(this.me.getBuriedness() > 0) {
        	Logger.info("I'm buried at " + me.getPosition());
        	AmbulanceTeam ambulanceTeam = (AmbulanceTeam) this.me();
            manager.addSendMessage(new MessageAmbulanceTeam(ambulanceTeam, MessageAmbulanceTeam.ACTION_REST, null));
        }
        
        // Check for stuckness
        if (this.tacticsAgent.stuck(currentTime)){
        	//Logger.trace("I'm blocked. Added a MessageBlockedArea");
        	//manager.addSendMessage(new MessageBlockedArea(this, this.location.getID(), this.target));
        	
        	BlockedArea mine = new BlockedArea(location.getID(), this.target, me.getX(), me.getY());
        	try{
        		this.reportBlockedArea(mine, manager, currentTime);
        		
        	}
        	catch(Exception e){
        		// Do nothing
        	}
    	}
        
        // If we are not in the special condition exploring, update target or get a new one 
        if(!this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.EXPLORING) 
        		&& !this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.GOING_TO_CLUSTER_LOCATION)){
	        if(this.target != null && this.world.getEntity(this.target) instanceof Human){
	        	this.victimSelector.updateTarget(currentTime, this.target);
	        }
	        else{
	        	this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
	        }
        }
        
        // Check for damage and remove target if necessary
        if(this.me.getDamage() >= 30) {
        	if(this.target != null && this.world.getEntity(this.target) instanceof Human){
        		if(this.location.getID().getValue() == ((Human) this.world.getEntity(this.target)).getPosition().getValue()){
	        		if(this.world.getEntity(this.location.getID()) instanceof Building){
	        			Building b = (Building) this.world.getEntity(this.location.getID());
	        			if(b.isOnFire()){
	        				((YRescueVictimSelector) this.victimSelector).remove(this.target);
	        				this.target = null;
	        			}
	        		}
        		}
        	}
        	this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_REFUGE);
        }       
          
        Refuge result = PositionUtil.getNearTarget(this.world, this.me, this.getRefuges());
        EntityID results = result.getID();           
        EntityID local = this.location.getID();        
		if ((local == results) && (this.me.getDamage() > 0)) {
			return new ActionRest(this);
		}
        
        /* === ---------------------------------- === *
         *  Possible states and their implementation  *
         * === ---------------------------------- === */
        
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.GOING_TO_CLUSTER_LOCATION)){
        	Logger.info("Going to cluster location..");
        	
    		if(this.target == null || this.target.getValue() == this.location.getID().getValue() || this.tacticsAgent.stuck(time)){
    			this.EXPLORE_TIME_LIMIT = currentTime + this.EXPLORE_TIME_STEP_TRESH;
    			this.stateMachine.setState(ActionStates.Ambulance.EXPLORING);
    		}
    		else{
    			return this.moveTarget(currentTime);
    		}
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.EXPLORING)){
        	Logger.info("Exploring..");
        	if(currentTime < this.EXPLORE_TIME_LIMIT){
        		if(this.target == null || this.target.getValue() == this.location.getID().getValue()){
        			getNewExplorationTarget(currentTime);
                	return moveTarget(currentTime);
        		}
        		
        		if(this.target != null && this.tacticsAgent.stuck(time)){
        			heatMap.updateNode(this.target, time, HeatNode.MAX_HEAT);
        			getNewExplorationTarget(currentTime);
                	return moveTarget(currentTime);
        		}
        		
        		return this.moveTarget(currentTime);
        	}
        	
        	if(!this.tacticsAgent.stuck(time) && (this.target != null && this.target.getValue() != this.location.getID().getValue())){
        		return this.moveTarget(currentTime);
        	}
        	
        	this.target = null;
        	this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.GOING_TO_TARGET)){
        	Logger.info("Going to target..");
        	Human victim = (Human) this.world.getEntity(this.target);
        	
        	if(this.tacticsAgent.stuck(time)){
        		this.stateMachine.setState(ActionStates.Ambulance.EXPLORING);
        		getNewExplorationTarget(currentTime);
            	return moveTarget(currentTime);
        	}
        	else if (victim.getPosition().getValue() != this.location.getID().getValue()) {
                return this.moveTarget(currentTime);
            }
        	
        	this.stateMachine.setState(ActionStates.Ambulance.RESCUING);
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.RESCUING)){
        	Logger.info("Rescuing..");
        	Human victim = (Human) this.world.getEntity(this.target);
        	Logger.info("Target b'ness: " + victim.getBuriedness() + ", dmg: " + victim.getDamage() + ", HP: " + victim.getHP());
        	
        	//if(me.getID().getValue() == 941331988){
	        	//if(this.recruitmentManager.getRecruitmentState() == RecruitmentManager.RecruitmentStates.NOTHING){
	        		//this.recruitmentManager.setRecruitmentState(RecruitmentManager.RecruitmentStates.RECRUITING);
	        		//this.recruitmentManager.createNewRecruitmentMsg(Task.RESCUE, 1, time);
        	/*
	        	}
	        	else if(this.recruitmentManager.getRecruitmentState() == RecruitmentManager.RecruitmentStates.RECRUITING && this.recruitmentManager.isEnlistmentAvailable()){
	        		Logger.info("Recruitment agent enlisted - " + this.recruitmentManager.isEnlistmentAvailable() + this.recruitmentManager.getAgentsEnlisted());
	        	}*/
        	//}
        	
        	if(victim.getPosition().getValue() != this.location.getID().getValue()){
        		Logger.info("Target not in place!, going to target again ...");
        		this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_TARGET);
        		return this.moveTarget(currentTime);
        	}
        	
        	// If we are in the target position and the target is not there
        	HumanTarget ht = ((YRescueVictimSelector) this.victimSelector).getHumanTarget(victim.getID());
        	if(ht != null && ht.getPreviousBurriednessUpdatedTime() == currentTime -1 && ht.getPreviousBurriedness() == ht.getHuman().getBuriedness()){
        		Logger.info("Victim not in previous location anymore (Burriedness stays the same), selecting new target ..");
        		this.victimSelector.remove(this.target);
        		this.target = null;
        		this.stateMachine.setState((ActionStates.Ambulance.SELECT_NEW_TARGET));
        	}
        	else if(victim.getBuriedness() > 0 && victim.getHP() > 0){
        		if(ht != null) ht.updateBurriedness(currentTime);
        		Logger.info("Send rescue command..");
        		return new ActionRescue(this, this.target);
        	}
        	else if (victim.getHP() <= 0){
        		Logger.info("Not rescuing, victim is dead. Will look for new target");
        		this.target = null;
        		victimSelector.remove(victim);
        		this.stateMachine.setState((ActionStates.Ambulance.SELECT_NEW_TARGET));
        	}
        	else{
	        	this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_REFUGE);
	        	
	        	if(victim instanceof Civilian){
	        		Civilian civilian = (Civilian) victim;
	        		manager.addSendMessage(new MessageCivilian(civilian));
		            this.victimSelector.remove(civilian);
	        	}
	        	else if(victim instanceof Human){
	        		this.victimSelector.remove((Human) victim);
	        	}
	        	
	            return new ActionLoad(this, this.target);
        	}
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.GOING_TO_REFUGE)){
        	Logger.info("Going to refugee..");
        	
        	if(!this.someoneOnBoard() && this.me.getDamage() <= 0){
        		Logger.info("Does not need to go to refugee, selecting new target..");
        		this.target = null;
        		this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
        	}
        	else if(this.target != null && (this.world.getEntity(this.target) instanceof Human && ((Human) this.world.getEntity(this.target)).getHP() <= 0)){
        		Logger.info("The human im carrying on is dead, selecting new target..");
        		this.target = null;
        		this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
        		return new ActionUnload(this);
        	}
        	else if(this.tacticsAgent.stuck(currentTime) && this.someoneOnBoard()){
        		Logger.info("I'm carrying someone but I'm stuck, selecting new target");
        		this.target = null;
        		this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
        		return new ActionUnload(this);
        	}
        	else if(this.tacticsAgent.stuck(currentTime)){
        		Logger.info("I'm stuck, selecting new target");
        		this.target = null;
        		this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
        	}
        	else{
	        	if(this.location instanceof Refuge) {
	                if(this.someoneOnBoard()) {
	                	this.target = null;
	                	this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
	                    return new ActionUnload(this);
	                }
	               
	                if(this.me.getDamage() > 0) {
	                    return new ActionRest(this);
	                }
	                else{
	                	this.target = null;
	                	this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
	                }
	            }
	        	else{
	        		return this.moveRefuge(currentTime);
	        	}
        	}
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.SELECT_NEW_TARGET)){
        	Logger.info("Select new target..");
        	
        	//this.recruitmentManager.setRecruitmentState(RecruitmentManager.RecruitmentStates.NOTHING);
        	this.target = this.victimSelector.getNewTarget(currentTime);
            if(this.target == null) {
            	Logger.info("Cannot define a first new target, going to explore!");
            	getNewExplorationTarget(currentTime);
            	return moveTarget(currentTime);
            }
            
            // Begin target basic processing
            do {
                Human victim = (Human) this.world.getEntity(this.target);
                if (victim.getPosition().getValue() != this.location.getID().getValue()) {
                	this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_TARGET);
                    return this.moveTarget(currentTime);
                }
                
                // Threshold to rescue
                if (victim.getHP() <= 100){ 
                	this.victimSelector.remove(victim.getID());
                	this.target = this.victimSelector.getNewTarget(currentTime);
                	continue;
                }
                
                if (victim.getBuriedness() > 0) {
                	this.stateMachine.setState(ActionStates.Ambulance.RESCUING);
                    return new ActionRescue(this, this.target);
                }
                
                // In the case of rescue already
                if (victim instanceof Civilian) {
                	this.stateMachine.setState(ActionStates.Ambulance.CARRYING_WOUNDED);
                    Civilian civilian = (Civilian) victim;
                    manager.addSendMessage(new MessageCivilian(civilian));
                    this.victimSelector.remove(civilian);
                    return new ActionLoad(this, this.target);
                }
                
                // Disaster relief agent
                if (victim instanceof AmbulanceTeam) {
                    AmbulanceTeam ambulanceTeam = (AmbulanceTeam) victim;
                    manager.addSendMessage(new MessageAmbulanceTeam(ambulanceTeam, MessageAmbulanceTeam.ACTION_REST, null));
                    this.victimSelector.remove(ambulanceTeam);
                } else if (victim instanceof FireBrigade) {
                    FireBrigade fireBrigade = (FireBrigade) victim;
                    manager.addSendMessage(new MessageFireBrigade(fireBrigade, MessageFireBrigade.ACTION_REST, null));
                    this.victimSelector.remove(fireBrigade);
                } else if (victim instanceof PoliceForce) {
                    PoliceForce policeForce = (PoliceForce) victim;
                    manager.addSendMessage(new MessagePoliceForce(policeForce, MessagePoliceForce.ACTION_REST, null));
                    this.victimSelector.remove(policeForce);
                }
                // The target has already been rescued. Or in the case of excluded
                this.target = this.victimSelector.getNewTarget(currentTime);
                
            }while (this.target != null);
        }
        
        Logger.info("Cannot define a target or action, going to explore!");
        getNewExplorationTarget(currentTime);
        return moveTarget(currentTime);
    }

	@Override
	public Action failsafeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
		//failSafeUpdateUnexploredBuildings(changed);
		Logger.info("" + me + " going failsafe in timestep " + currentTime);
		
		if (failSafeSomeoneOnBoard()) {
			if (location() instanceof Refuge) {
				Logger.info("FAILSAFE: Unloading");
				return new ActionUnload(this);
			} else {
				return this.moveRefuge(currentTime);
			}
		}
		
		for (Human next : failSafeGetTargets()) {
			if (next.getPosition().equals(location().getID())) {
				if ((next instanceof Civilian) && next.getBuriedness() == 0 && !(location() instanceof Refuge)) {
					Logger.info("FAILSAFE: Loading " + next);
					return new ActionLoad(this, (Civilian) next);
				}
				if (next.getBuriedness() > 0) {
					Logger.info(String.format(
						"FAILSAFE: Rescueing %s. HP: %d, B'ness: %d", next, next.getHP(), next.getBuriedness()
					));
					return new ActionRescue(this, next.getID());
				}
			} else {
				List<EntityID> path = routeSearcher.getPath(currentTime, me().getPosition(), next.getPosition());
				if (path != null) {
					Logger.info("FAILSAFE: Moving to target with " + path);
					return new ActionMove(this, path);
				}
			}
		}
		/*List<EntityID> path = failSafeSearch.breadthFirstSearch(me().getPosition(), unexploredBuildings);
		if (path != null) {
			Logger.info("Searching buildings");
			sendMove(time, path);
			return;
		}*/
		Logger.info("FAILSAFE: Moving randomly");
		return new ActionMove(this, routeSearcher.noTargetMove(currentTime, me.getPosition()));
	}

	/**
	 * Check if someone is on board, use a location with some tolerance
	 */
	public boolean someoneOnBoard() {
		if(this.target != null && /*(this.world.getEntity(this.target) instanceof Civilian ||*/ world.getEntity(this.target) instanceof Human){
			Human h = (Human) this.world.getEntity(this.target);
			
			if(h.isPositionDefined() && h.getPosition().equals(me.getID())){
				Logger.info("" + h + " is on board.");
				return true;
			}
			else {
				Logger.info("Nobody on board.");
				return false;
			}
			/*int traveledDistance = 0;
			int burriedness = 0;
			int hp = 0;
			
			if(h.isTravelDistanceDefined()) traveledDistance = h.getTravelDistance();
			if(h.isBuriednessDefined()) burriedness = h.getBuriedness();
			if(h.isHPDefined()) hp = h.getHP();
					
			return PositionUtil.equalsPoint(this.world.getEntity(this.target).getLocation(world), this.me.getLocation(world), 50) && burriedness <= 0 && hp > 0 && traveledDistance <= 0;*/
		}
		else{
			Logger.warn("WARNING! Target " + target + " is not a Human!");
			return false;
		}
			
    }
	
	/**
	 * Move to target, and do some sanity checks
	 * If the building we are trying to enter is on fire, get a new exploration target 
	 */
	public Action moveTarget(int currentTime) {
        List<EntityID> path = getPathToTarget(currentTime);
        if(path != null && !path.isEmpty()){
        	Entity ent = this.world.getEntity(path.get(path.size() -1));
        	if(ent instanceof Building){
        		Building b = (Building) ent;
        		if(b.isOnFire() || (b.isFierynessDefined() && b.getFierynessEnum().equals(StandardEntityConstants.Fieryness.BURNT_OUT))){
        			Logger.debug("The next building is on FIRE or burnOut, select a new exploration target");
        			//this.heatMap.updateNode(ent.getID(), time);
        			this.heatMap.removeEntityID(ent.getID());
        			//getNewExplorationTarget(currentTime);
        			//path = getPathToTarget(currentTime);
        		}
        	}
        }
        
        return new ActionMove(this, path != null ? path : this.routeSearcher.noTargetMove(currentTime, this.me));
    }
	
	/**
	 * Copied from SampleAgent. Do not change
	 * 
	 * @return
	 */
	private List<Human> failSafeGetTargets() {
		List<Human> targets = new ArrayList<Human>();
		for (StandardEntity next : model.getEntitiesOfType(
				StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE,
				StandardEntityURN.POLICE_FORCE,
				StandardEntityURN.AMBULANCE_TEAM)) {
			Human h = (Human) next;
			if (h == me()) {
				continue;
			}
			if (h.isHPDefined() && h.isBuriednessDefined()
					&& h.isDamageDefined() && h.isPositionDefined()
					&& h.getHP() > 0
					&& (h.getBuriedness() > 0 || h.getDamage() > 0)) {
				Logger.trace(String.format(
					"FAILSAFE: Adding %s to targets. HP: %d, B'ness: %d", h, h.getHP(), h.getBuriedness()
				));
				targets.add(h);
			}
		}
		Collections.sort(targets, new DistanceSorter(location(), model));
		return targets;
	}

	/**
	 * Copied from sample agent. Do not change
	 * 
	 * @return
	 */
	private boolean failSafeSomeoneOnBoard() {
		for (StandardEntity next : model
				.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			if (((Human) next).getPosition().equals(getID())) {
				Logger.debug("FAILSAFE: " + next + " is on board");
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString(){
    	return "Ambulance:" + this.getID();
    }

	@Override
	public HeatMap initializeHeatMap() {
		// Prepare HeatMap
    	HeatMap heatMap = new HeatMap(this.agentID, this.world, this.routeBreadthFirstCache);
        for (Entity next : this.getWorld()) {
            if (next instanceof Area) {
            	// Ignore very small areas to explore
            	if(GeometricUtil.getAreaOfEntity(next.getID(), this.world) < EXPLORE_AREA_SIZE_TRESH) continue;
            	
            	// Ignore non building areas
            	if(!(next instanceof Building)) continue;
            	
            	heatMap.addEntityID(next.getID(), HeatNode.PriorityLevel.VERY_LOW, 0);
            }
        }
        
        return heatMap;
	}
	
	public EntityID getNewExplorationTarget(int currentTime){
		EntityID nodeToVisit = heatMap.getNodeToVisit();
        
		if(nodeToVisit == null){
        	Logger.debug("Random walk");
        	List<EntityID> path = this.routeSearcher.noTargetMove(currentTime, this.me);
        	nodeToVisit = path.get(path.size() -1);
        }
        else{
        	Logger.debug("HeatmMap");
        }
        
		this.target = nodeToVisit;
		this.stateMachine.setState(ActionStates.Ambulance.EXPLORING);
        return this.target;
	}
	
	public void printHumanTargets(){
		((YRescueVictimSelector) this.victimSelector).humanTargetM.updateUtilities(time);
        List<HumanTarget> humanTargets = ((YRescueVictimSelector) this.victimSelector).humanTargetM.getAllHumanTargets();
        for (HumanTarget hum : humanTargets) {
        	Logger.debug("Human Target: "+ hum.getHuman() + " Utility: "+ hum.getUtility() + " Human ID:" + hum.getHuman().getID() + " pos:" + hum.getHuman().getPosition() + " burriedness:" + hum.getHuman().getBuriedness() + " damage:" + hum.getHuman().getDamage() + " HP:" + hum.getHuman().getHP());
        }
	}
	
	public void writeClusters(){
		String clustersfileName = "/tmp/cluster_list_" + me.getID().getValue() + ".txt";
		String centroidsfileName = "/tmp/cluster_centroids.txt";
		
		PrintWriter writer;
		try {
			writer = new PrintWriter(clustersfileName, "UTF-8");
			StringBuilder strBuilder = new StringBuilder();
			
			for (Iterator<EntityID> it = clusterToVisit.iterator(); it.hasNext();) {
				EntityID ent = it.next();
				
				Area area0 = (Area) this.world.getEntity(ent);
				
				strBuilder.append(area0.getX());
				strBuilder.append(" ");
				strBuilder.append(area0.getY());
				strBuilder.append("\n");
			}
			
			writer.println(strBuilder.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		try {
			writer = new PrintWriter(centroidsfileName, "UTF-8");
			StringBuilder strBuilder = new StringBuilder();
			
			for (Iterator<EntityID> it = clusterCentroids.iterator(); it.hasNext();) {
				EntityID ent = it.next();
				
				Area area0 = (Area) this.world.getEntity(ent);
				
				strBuilder.append(area0.getX());
				strBuilder.append(" ");
				strBuilder.append(area0.getY());
				strBuilder.append("\n");
			}
			
			writer.println(strBuilder.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

}
