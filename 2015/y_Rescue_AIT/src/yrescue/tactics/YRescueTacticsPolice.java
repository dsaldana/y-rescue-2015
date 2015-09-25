package yrescue.tactics;

import adk.team.action.Action;
import adk.team.action.ActionClear;
import adk.team.action.ActionMove;
import adk.team.action.ActionRest;
import adk.team.tactics.TacticsPolice;
import adk.team.util.ImpassableSelector;
import adk.team.util.RouteSearcher;
import adk.team.util.graph.PositionUtil;
import adk.team.util.provider.ImpassableSelectorProvider;
import adk.team.util.provider.RouteSearcherProvider;
import adk.sample.basic.event.BasicRoadEvent;
import adk.sample.basic.util.BasicImpassableSelector;
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
import yrescue.statemachine.ActionStates;
import yrescue.statemachine.StateMachine;
import yrescue.statemachine.StatusStates;
import yrescue.util.YRescueDistanceSorter;
import adk.sample.basic.event.BasicRoadEvent;
import adk.sample.basic.util.*;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import com.sun.org.apache.xpath.internal.SourceTree;

public class YRescueTacticsPolice extends BasicTacticsPolice {

    public ImpassableSelector impassableSelector;
    public RouteSearcher routeSearcher;

    //0 -> current
    //1 -> current - 1
    public Point2D[] agentPoint;
    public boolean posInit;
    public boolean beforeMove;

    private int clearRange;
    private int clearWidth;
    
    private StateMachine actionStateMachine;
    private StateMachine statusStateMachine;

   
    @Override
    public String getTacticsName() {
        return "Y-Rescue Policeman";
    }

    @Override
    public void registerEvent(MessageManager manager) {
        manager.registerEvent(new BasicRoadEvent(this, this));
    }


    @Override
    public ImpassableSelector initImpassableSelector() {
        return new BasicImpassableSelector(this);
    }

    @Override
    public RouteSearcher initRouteSearcher() {
        return new BasicRouteSearcher(this);
    }

    @Override
    public void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager) {
        for (EntityID next : updateWorldInfo.getChangedEntities()) {
            StandardEntity entity = this.getWorld().getEntity(next);
            if(entity instanceof Blockade) {
                this.impassableSelector.add((Blockade) entity);
            }
            else if(entity instanceof Civilian) {
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
        this.beforeMove = false;
        this.agentPoint = new Point2D[2];
        this.posInit = true;
        clearRange = 10000;
        clearWidth = 1200;
        this.actionStateMachine = new StateMachine(ActionStates.Policeman.AWAITING_ORDERS);
        this.statusStateMachine = new StateMachine(StatusStates.EXPLORING);
    }

    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    	System.out.println("\nTimestep:" + currentTime);
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        
        
        /***************************************
         * 
         * The strategy here First selects the closest blockage and sends the policeman there
         * TODO: define a strategy for the police destination
         */
        
        // Update target Destination
        EntityID oldTarget;
        this.target = new EntityID(297);
        /*if(this.target != null) {
        	oldTarget = this.target;
            this.target = this.impassableSelector.updateTarget(currentTime, this.target);
        } else { // Select a new Target Destination
        	this.target = this.impassableSelector.getNewTarget(currentTime);
        }*/
        
        // Determines the path to be followed
        List<EntityID> path;
        if(this.target == null || this.target == this.location().getID()) { //if there is no more known targets -> select a random target
        	path = this.routeSearcher.noTargetMove(currentTime, this.me);
        } else {
        	path = this.routeSearcher.getPath(currentTime, this.me, this.target);
        }
        System.out.println(path);
        
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
    		System.out.println("Distance to midpoint: " + agentToTarget.getLength());
    		if (agentToTarget.getLength() < 1000){
    			System.out.println("Mid point of frontier is very close, will aim to next area's centroid");
    			target = new Point2D(area1.getX(), area1.getY());
    		}
        	
    		
    		actionStateMachine.setState(ActionStates.Policeman.CLEARING);
    		statusStateMachine.setState(StatusStates.ACTING);
        	return new ActionClear(this, (int)target.getX(), (int)target.getY());
        }else{
        	System.out.println("blockade on way? " + checkBlockadeOnWayTo(path));
        	actionStateMachine.setState(ActionStates.MOVING_TO_TARGET);
    		statusStateMachine.setState(StatusStates.ACTING);
        	return new ActionMove(this, path);
        }
            
        //return new ActionRest(this);
   }
    
    private boolean checkBlockadeOnWayTo(List<EntityID> dest_path) {
		
		//EntityID dest = dest_path.get(0);
	
		Area area0 = (Area) this.world.getEntity(this.location.getID());
		Area area1 = (Area) this.world.getEntity(dest_path.get(0));
		
		System.out.println(""+area0 + " - " + area1);
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
		}
		
		//System.out.println("frontier: " + frontier);
		System.out.println("target: " + target);
		ArrayList<Blockade> blockList = new ArrayList<Blockade>(getBlockadesInRange(me().getX(), me().getY(), clearRange));
		System.out.println("blocklist: " + blockList);
		if (anyBlockadeInClearArea(blockList, target))
			return true;
		return false;
	}
    
	/**
	 * Returns true if a blockade in the list is in clearRange around target area
	 * @param blockList
	 * @param target
	 * @return
	 */
	private boolean anyBlockadeInClearArea(ArrayList<Blockade> blockList, Point2D target) {
		boolean result = false;
		java.awt.geom.Area clearArea = getClearArea((int)target.getX(), (int)target.getY(), clearRange, clearWidth);
		for (Blockade block : blockList) {
			java.awt.geom.Area select = new java.awt.geom.Area(block.getShape());
			select.intersect(clearArea);
			if (!select.isEmpty()) {
				Logger.debug(block + "  has intersect with clear area ");
				result = true;
			}
		}
		return result;
	}
	/**
	 * from clear.Geometry
	 */
	public java.awt.geom.Area getClearArea(int targetX, int targetY, int clearLength, int clearRad) {
		clearLength = clearLength - 200;
		clearRad = clearRad - 200;
		Vector2D agentToTarget = new Vector2D(targetX - me().getX(), targetY - me().getY());

		if (agentToTarget.getLength() > clearLength)
			agentToTarget = agentToTarget.normalised().scale(clearLength);

		Vector2D backAgent = (new Vector2D(me().getX(), me().getY()))
				.add(agentToTarget.normalised().scale(-450));
		Line2D line = new Line2D(backAgent.getX(), backAgent.getY(),
				agentToTarget.getX(), agentToTarget.getY());

		Vector2D dir = agentToTarget.normalised().scale(clearRad);
		Vector2D perpend1 = new Vector2D(-dir.getY(), dir.getX());
		Vector2D perpend2 = new Vector2D(dir.getY(), -dir.getX());

		Point2D points[] = new Point2D[] {
				line.getOrigin().plus(perpend1),
				line.getEndPoint().plus(perpend1),
				line.getEndPoint().plus(perpend2),
				line.getOrigin().plus(perpend2) };
		int[] xPoints = new int[points.length];
		int[] yPoints = new int[points.length];
		for (int i = 0; i < points.length; i++) {
			xPoints[i] = (int) points[i].getX();
			yPoints[i] = (int) points[i].getY();
		}
		return new java.awt.geom.Area(new Polygon(xPoints, yPoints, points.length));
	}

    public PriorityQueue<Blockade> getBlockadesInRange(int x, int y, int range) {
		final PriorityQueue<Blockade> result = new PriorityQueue<Blockade>(20, new YRescueDistanceSorter(me(), model));
		Rectangle r = new Rectangle(x - range, y - range, x + range, y + range);

		Collection<StandardEntity> entities = model.getObjectsInRectangle(x - range, y - range, x + range, y + range);
		for(StandardEntity e : entities){
			if (e instanceof Road){
				Road road = (Road) e;
				if (road.isBlockadesDefined()){					
					for (EntityID blockID : road.getBlockades()){
						if(model.getDistance(me().getID(), blockID) < range){
							result.add((Blockade)model.getEntity(blockID));
						}						
					}
				}
			}			
		}
		return result;
    }
}
