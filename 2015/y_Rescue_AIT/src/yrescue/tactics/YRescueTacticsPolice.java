package yrescue.tactics;

import adk.team.action.Action;
import adk.team.action.ActionClear;
import adk.team.action.ActionMove;
import adk.team.action.ActionRest;
import adk.team.util.ImpassableSelector;
import adk.team.util.RouteSearcher;
import adk.sample.basic.event.BasicRoadEvent;
import adk.sample.basic.tactics.BasicTacticsPolice;
import adk.sample.basic.util.BasicRouteSearcher;
import comlib.manager.MessageManager;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import comlib.message.information.MessagePoliceForce;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import yrescue.message.event.MessageBlockedAreaEvent;
import yrescue.problem.blockade.BlockadeUtil;
import yrescue.problem.blockade.BlockedArea;
import yrescue.problem.blockade.BlockedAreaSelector;
import yrescue.problem.blockade.BlockedAreaSelectorProvider;
import yrescue.statemachine.ActionStates;
import yrescue.statemachine.StateMachine;
import yrescue.statemachine.StatusStates;
import yrescue.util.YRescueDistanceSorter;
import yrescue.util.YRescueImpassableSelector;
import adk.sample.basic.util.*;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.log4j.MDC;

public class YRescueTacticsPolice extends BasicTacticsPolice implements BlockedAreaSelectorProvider {

    public ImpassableSelector impassableSelector;
    public RouteSearcher routeSearcher;
    
    public BlockadeUtil blockadeUtil;
    public BlockedAreaSelector blockedAreaSelector;

    public BlockedArea blockedAreaTarget;
    
    //0 -> current
    //1 -> current - 1
    public Point2D[] agentPoint;
    public boolean posInit;
    public boolean beforeMove;

    public int clearRange;
    public int clearWidth;
    
    private StateMachine actionStateMachine;
    private StateMachine statusStateMachine;
    

   
    @Override
    public String getTacticsName() {
        return "Y-Rescue Policeman";
    }

    @Override
    public void registerEvent(MessageManager manager) {
        manager.registerEvent(new BasicRoadEvent(this, this));
        manager.registerEvent(new MessageBlockedAreaEvent(this, this));
    }


    @Override
    public ImpassableSelector initImpassableSelector() {
        return new YRescueImpassableSelector(this);
    }

    @Override
    public RouteSearcher initRouteSearcher() {
        return new BasicRouteSearcher(this);
    }
    
    @Override
    public ImpassableSelector getImpassableSelector(){
    	if (this.impassableSelector == null) {
    		this.impassableSelector = initImpassableSelector();
    		Logger.warn("Warning, impassable selector was null, now a new one was instantiated");
    	}
    	return this.impassableSelector;
    }

    @Override
    public void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager) {
        for (EntityID next : updateWorldInfo.getChangedEntities()) {
            StandardEntity entity = this.getWorld().getEntity(next);
            /*if(entity instanceof Blockade) {
                this.impassableSelector.add((Blockade) entity);
            }
            else*/ if(entity instanceof Civilian) {
                Civilian civilian = (Civilian)entity;
                if(civilian.getBuriedness() > 0) {
                    manager.addSendMessage(new MessageCivilian(civilian));
                }
            }
            else if(entity instanceof Building) {
                Building b = (Building)entity;
                if(b.isOnFire()) {
                    manager.addSendMessage(new MessageBuilding(b));
                }
            }
        }
    }


    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.routeSearcher = this.initRouteSearcher();
        this.impassableSelector = this.initImpassableSelector();
        this.blockedAreaSelector = new BlockedAreaSelector(this);
        this.beforeMove = false;
        this.agentPoint = new Point2D[2];
        this.posInit = true;
        clearRange = 10000;
        clearWidth = 1200;
        
        this.actionStateMachine = new StateMachine(ActionStates.Policeman.AWAITING_ORDERS);
        this.statusStateMachine = new StateMachine(StatusStates.EXPLORING);
        this.blockadeUtil = new BlockadeUtil(this);
        
        MDC.put("agent", this);
        MDC.put("location", location());
        
        Logger.info("Preparation complete!");
    }

    public void ignoreTimeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    	Logger.debug("\nRadio channel: " + manager.getRadioConfig().getChannel());
    }
    
    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    	Logger.info("\nTimestep:" + currentTime);
    	Logger.debug("Radio channel: " + manager.getRadioConfig().getChannel());
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        
        MDC.put("location", location());
        
        Logger.trace("The received message: " + manager.getReceivedMessage());
        
        
        
        //if I am buried, send a message and attempt to clear the entrance to my building
        if(this.me.getBuriedness() > 0) {
            this.beforeMove = false;
            statusStateMachine.setState(StatusStates.BURIED);
            actionStateMachine.setState(ActionStates.IDLE);
            manager.addSendMessage(new MessagePoliceForce(this.me, MessagePoliceForce.ACTION_REST, this.agentID));
            List<EntityID> neighbours = ((Area)this.location).getNeighbours();
            if(neighbours.isEmpty()) {
                return new ActionRest(this);
            }
            if(this.count <= 0) {
                this.count = neighbours.size();
            }
            this.count--;
            Area area = (Area)this.world.getEntity(neighbours.get(this.count));
            Vector2D vector = (new Point2D(area.getX(), area.getY())).minus(this.agentPoint[0]).normalised().scale(1000000);
            actionStateMachine.setState(ActionStates.Policeman.CLEARING);
            return new ActionClear(this, (int) (this.me.getX() + vector.getX()), (int) (this.me.getY() + vector.getY()));
        }
        
        if (this.tacticsAgent.stuck(currentTime)){
        	Logger.info("I'm STUCK! How's that possible?");
        }
        
        
        
        /***************************************
         * 
         * The strategy here First selects the closest blockage and sends the policeman there
         * TODO: define a strategy for the police destination
         */
        
        //this.target = new EntityID(256);
        /*YRescueImpassableSelector yis = (YRescueImpassableSelector) this.impassableSelector;
        Logger.debug("#blocked roads: " + yis.impassableRoadList.size());
        Logger.debug("They are: " + yis.impassableRoadList);
        */
        
        Logger.debug("#blocked roads: " + blockedAreaSelector.blockedAreas.size());
        Logger.debug("They are: " + blockedAreaSelector.blockedAreas.values());
        
        
        if(this.blockedAreaTarget != null) {
            this.blockedAreaTarget = this.blockedAreaSelector.updateTarget(currentTime, this.blockedAreaTarget);    
        } else { // Select a new Target Destination
        	this.blockedAreaTarget = this.blockedAreaSelector.getNewTarget(currentTime);
        }
        
        
        
        // Determines the path to be followed
        List<EntityID> path;
        if(this.blockedAreaTarget == null){
        	path = this.routeSearcher.noTargetMove(currentTime, this.me);
        	Logger.debug("noTargetMove - path: " + path);
        } else {
        	path = this.routeSearcher.getPath(currentTime, this.me, this.blockedAreaTarget.getEntityID());
        	Logger.debug("Path to target: " + path);
        }
        
        /***************************************
         * 
         * Go towards the chosen path
         * TODO: 
         */
        
        // There is a blockage on the way
        if(path != null && path.size() > 0 && checkBlockadeOnWayTo(path)){
    		
    		Area area0 = (Area) this.world.getEntity(this.location.getID());
    		Area area1 = (Area) this.world.getEntity(path.get(0));
    		
    		Point2D target;
    		
    		if(area0 == area1) {
    			target = new Point2D(area0.getX(), area0.getY());
    		}
    		else{
    			Edge frontier = area0.getEdgeTo(area1.getID());
    		
    			target = new Point2D(frontier.getStartX() + (frontier.getEndX() - frontier.getStartX())/2,
    									 frontier.getStartY() + (frontier.getEndY() - frontier.getStartY())/2);
    		}
    		
    		//CHECK IF DISTANCE TO FRONTIER IS SHORT
    		Vector2D agentToTarget = new Vector2D(target.getX() - me().getX(), target.getY() - me().getY());
    		//System.out.println("Distance to midpoint: " + agentToTarget.getLength());
    		if (agentToTarget.getLength() < 1000){
    			System.out.println("Mid point of frontier is very close, will aim to next area's centroid");
    			target = new Point2D(area1.getX(), area1.getY());
    			agentToTarget = new Vector2D(target.getX() - me().getX(), target.getY() - me().getY());
    		}
    		
    		//MAKES SURE THE AGENT WILL SHOOT AT THE MAXIMUM RANGE
    		Vector2D normalagentToTarget = agentToTarget.normalised();
        	Vector2D escalar = normalagentToTarget.scale(clearRange);
        	target = new Point2D(me.getX() + escalar.getX(),me.getY() + escalar.getY());
    		
    		actionStateMachine.setState(ActionStates.Policeman.CLEARING);
    		statusStateMachine.setState(StatusStates.ACTING);
        	return new ActionClear(this, (int)target.getX(), (int)target.getY());
        }else{
        	//System.out.println("blockade on way? " + checkBlockadeOnWayTo(path));
        	actionStateMachine.setState(ActionStates.MOVING_TO_TARGET);
    		statusStateMachine.setState(StatusStates.ACTING);
    		
    		if (blockedAreaTarget == null) {
    			Logger.trace("Null target, moving with " + path);
    			return new ActionMove(this, path);
    		}
    		else {
    			Logger.trace(String.format("Moving to %d,%d of path %s", this.blockedAreaTarget.x, this.blockedAreaTarget.y, path)); 
    			return new ActionMove(this, path, this.blockedAreaTarget.x, this.blockedAreaTarget.y);
    		}
    		
        	
        }
            
        //return new ActionRest(this);
   }
    
    private boolean checkBlockadeOnWayTo(List<EntityID> dest_path) {
		
		//EntityID dest = dest_path.get(0);
	
		Area area0 = (Area) this.world.getEntity(this.location.getID());
		Area area1 = (Area) this.world.getEntity(dest_path.get(0));
		
		
		//System.out.println(""+area0 + " - " + area1);
		Point2D target;
		
		//TODO: melhorar o calculo do alvo (modularizar)
		if(area0 == area1) {
			target = new Point2D(area0.getX(), area0.getY());
		}
		else{
			Edge frontier = area0.getEdgeTo(area1.getID());
		
			target = new Point2D(frontier.getStartX() + (frontier.getEndX() - frontier.getStartX())/2,
									 frontier.getStartY() + (frontier.getEndY() - frontier.getStartY())/2);
		}
		
		//CHECK IF DISTANCE TO FRONTIER IS SHORT
		Vector2D agentToTarget = new Vector2D(target.getX() - me().getX(), target.getY() - me().getY());
		System.out.println("Distance to midpoint: " + agentToTarget.getLength());
		if (agentToTarget.getLength() < 1000){
			System.out.println("Mid point of frontier is very close, will aim to next area's centroid");
			target = new Point2D(area1.getX(), area1.getY());
			agentToTarget = new Vector2D(target.getX() - me().getX(), target.getY() - me().getY());
		}
		Vector2D normalagentToTarget = agentToTarget.normalised();
		Vector2D escalar = normalagentToTarget.scale(clearRange);
		target = new Point2D(me.getX() + escalar.getX(),me.getY() + escalar.getY());
		//System.out.println("frontier: " + frontier);
		Logger.trace("target: " + target);
		ArrayList<Blockade> blockList = new ArrayList<Blockade>(blockadeUtil.getBlockadesInSquare(me().getX(), me().getY(), clearRange));
		Logger.trace("#blockades in square around agent: " + blockList.size());
		Logger.trace("They are: " + blockList.size());
		
		if (blockadeUtil.anyBlockadeInClearArea(blockList, target)){
			Logger.trace("There is a blockade in clear area!");
			return true;
		}
		return false;
	}
    
	

    public String toString(){
    	return "Police:" + this.getID();
    }

	@Override
	public BlockedAreaSelector getBlockedAreaSelector() {
		return blockedAreaSelector;
	}
}
