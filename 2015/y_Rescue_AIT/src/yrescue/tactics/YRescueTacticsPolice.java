package yrescue.tactics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.MDC;

import adk.sample.basic.event.BasicRoadEvent;
import adk.sample.basic.tactics.BasicTacticsPolice;
import adk.sample.basic.util.BasicRouteSearcher;
import adk.team.action.Action;
import adk.team.action.ActionClear;
import adk.team.action.ActionMove;
import adk.team.action.ActionRest;
import adk.team.util.ImpassableSelector;
import adk.team.util.RouteSearcher;
import comlib.manager.MessageManager;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import comlib.message.information.MessagePoliceForce;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import yrescue.heatmap.HeatMap;
import yrescue.heatmap.HeatNode;
import yrescue.message.event.MessageBlockedAreaEvent;
import yrescue.problem.blockade.BlockadeUtil;
import yrescue.problem.blockade.BlockedArea;
import yrescue.problem.blockade.BlockedAreaSelector;
import yrescue.problem.blockade.BlockedAreaSelectorProvider;
import yrescue.statemachine.ActionStates;
import yrescue.statemachine.StateMachine;
import yrescue.statemachine.StatusStates;
import yrescue.util.GeometricUtil;
import yrescue.util.YRescueImpassableSelector;

public class YRescueTacticsPolice extends BasicTacticsPolice implements BlockedAreaSelectorProvider {

    public ImpassableSelector impassableSelector;
    public RouteSearcher routeSearcher;
    
    protected HeatMap heatMap = null;
    
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
    
    public Set<EntityID> cleanRefuges;


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
        
        this.cleanRefuges = new HashSet<>();
        
        MDC.put("agent", this);
        MDC.put("location", location());
        
        Logger.info("Preparation complete!");
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

    public void ignoreTimeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    	Logger.debug("\nRadio channel: " + manager.getRadioConfig().getChannel());
    }
    
    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    	Logger.debug("Radio channel: " + manager.getRadioConfig().getChannel());
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        
        MDC.put("location", location());
        
        Logger.info(String.format(
			"HP: %d, B'ness: %d, Dmg: %d, Direction: %d",  
			me.getHP(), me.getBuriedness(), me.getDamage(), me.getDirection()
		));
        
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
            
            if(area != null && this.agentPoint[0] != null) {
	            Vector2D vector = (new Point2D(area.getX(), area.getY())).minus(this.agentPoint[0]).normalised().scale(1000000);
	            actionStateMachine.setState(ActionStates.Policeman.CLEARING);
	            return new ActionClear(this, (int) (this.me.getX() + vector.getX()), (int) (this.me.getY() + vector.getY()));
            }
            else{
            	return new ActionRest(this);
            }
        }
        
        if (this.tacticsAgent.stuck(currentTime)){
        	Logger.warn("I'm STUCK! How's that possible?");
        	Blockade closest = BlockadeUtil.getClosestBlockadeInMyRoad(this);
        	if(closest == null){
        		return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, location.getID()));
        	}
        	return new ActionClear(this, closest);//closest.getX(), closest.getY() );
        }
        
        if(this.stuckClearLoop(currentTime)) {
        	Logger.warn("Warning: clearing the same position for more than 3 timesteps");
        	if(blockedAreaTarget != null){
        		return new ActionMove(this, this.routeSearcher.getPath(currentTime, this.me, this.blockedAreaTarget.getOriginID()));
        	}
        	else{
        		Logger.info("I have no target, using heatmap exploration...");
        		return new ActionMove(this, this.routeSearcher.getPath(currentTime, location.getID(), heatMap.getNodeToVisit()));
        	}
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
        
        if (blockedAreaSelector.blockedAreas.size() == 0){
        	EntityID randomDestination = null;
        	Random ran = new Random();
        	
        	List<EntityID> blockedRefuges = new ArrayList<>();
        	for (Refuge next : refugeList){
        		if (!(cleanRefuges.contains(next.getID()))){
        			blockedRefuges.add(next.getID());
        		}
        	}
        	
        	if (blockedRefuges.size() == 0){
        		Collection<StandardEntity> areas = this.getWorld().getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.BUILDING);
        		int index = ran.nextInt(areas.size());
        		randomDestination = areas.toArray(new StandardEntity[0])[index].getID();
        	} else {
        		
            	int index = ran.nextInt(blockedRefuges.size());
            	randomDestination = blockedRefuges.get(index);
        	}
        	
        	Area a = (Area) this.world.getEntity(randomDestination);
        	
        	this.getBlockedAreaSelector().add(new BlockedArea(randomDestination, null, a.getX(), a.getY()));
        }
        
        if(this.blockedAreaTarget != null) {
            this.blockedAreaTarget = this.blockedAreaSelector.updateTarget(currentTime, this.blockedAreaTarget);    
        } else { // Select a new Target Destination
        	this.blockedAreaTarget = this.blockedAreaSelector.getNewTarget(currentTime);
        }
        
        // Determines the path to be followed
        List<EntityID> path;
        if(this.blockedAreaTarget == null){
        	if (location instanceof Refuge){
        		this.cleanRefuges.add(location.getID());
        		Logger.debug("Refuge cleaned " +location.getID());
        	}
        	
        	
        	System.out.println("The heatmap " +heatMap);
        	path = this.routeSearcher.getPath(currentTime, me, heatMap.getNodeToVisit());// noTargetMove(currentTime, this.me);
        	Logger.debug("HeatMap exploration - path: " + path);
        } else {
        	path = this.routeSearcher.getPath(currentTime, this.me, this.blockedAreaTarget.getOriginID());
        	Logger.debug("Path to target: " + path);
        }
        
        //------Pegar a lista de predios a visitar:
        
        List<EntityID> buildingsToVisit = getBuildingsToVisit(currentTime);
        
        List<EntityID> newPath = new LinkedList<>();
        EntityID origin = location.getID();
        for(EntityID id : buildingsToVisit){
        	newPath.addAll(this.routeSearcher.getPath(currentTime, origin, id));
        	origin = id;
        }
        if(blockedAreaTarget != null){
        	newPath.addAll(this.routeSearcher.getPath(currentTime, origin, blockedAreaTarget.originID));
        }
        
        path = newPath;
        //-----Path ja ajustado com os predios a visitar
        
        Logger.debug("The new path, including surrounded buildings is: " + path);
        
        /**** Go towards the chosen path ****/
        
        if(path != null && path.size() > 0 && checkBlockadeOnWayTo(path, this.blockedAreaTarget)){ // There is a blockage on the way
        	
        	Logger.trace("Will shoot at blockade. My position: " + me.getX() + ", " +me.getY());
    		
        	Point2D target = getTargetPoint(path, blockedAreaTarget);
        	
    		
    		//CHECK IF DISTANCE TO FRONTIER IS SHORT
    		Vector2D agentToTarget = new Vector2D(target.getX() - me().getX(), target.getY() - me().getY());
    		//System.out.println("Distance to midpoint: " + agentToTarget.getLength());
    		/*if (agentToTarget.getLength() < 1000){
    			Logger.warn("Mid point of frontier is very close, will aim to next area's centroid");
    			Area next = (Area) this.world.getEntity(path.get(0));
    			target = new Point2D(next.getX(), next.getY());
    			agentToTarget = new Vector2D(target.getX() - me().getX(), target.getY() - me().getY());
    		}*/
    		
    		//MAKES SURE THE AGENT WILL SHOOT AT THE MAXIMUM RANGE
    		Vector2D normalagentToTarget = agentToTarget.normalised();
        	Vector2D escalar = normalagentToTarget.scale(clearRange);
        	target = new Point2D(me.getX() + escalar.getX(), me.getY() + escalar.getY());
    		
    		actionStateMachine.setState(ActionStates.Policeman.CLEARING);
    		statusStateMachine.setState(StatusStates.ACTING);
        	return new ActionClear(this, (int)target.getX(), (int)target.getY());
        }
        else{
        	//System.out.println("blockade on way? " + checkBlockadeOnWayTo(path));
        	actionStateMachine.setState(ActionStates.MOVING_TO_TARGET);
    		statusStateMachine.setState(StatusStates.ACTING);
    		
    		if (blockedAreaTarget == null) {
    			Logger.trace("Null target, moving with " + path);
    			return new ActionMove(this, path);
    		}
    		else {
    			Logger.trace(String.format("Moving to %d,%d of path %s", this.blockedAreaTarget.xOrigin, this.blockedAreaTarget.yOrigin, path)); 
    			return new ActionMove(this, path, this.blockedAreaTarget.xOrigin, this.blockedAreaTarget.yOrigin);
    		}
        }
        /**** END: Go towards the chosen path ****/
            
        //return new ActionRest(this);
    }
    
    protected Point2D getTargetPoint(List<EntityID> path, BlockedArea bTarget){
		
		Area area0 = (Area) this.world.getEntity(this.location.getID());
		Area area1 = (Area) this.world.getEntity(path.get(0));
		
		Point2D target;
		
		if (area0 == area1) {
			// target = new Point2D(area0.getX(), area0.getY());
			if (bTarget != null) {
				target = new Point2D(bTarget.xOrigin, bTarget.yOrigin);
				Logger.debug("TargetPoint: coordinates of target in current area :)");
			} else {
				target = new Point2D(area0.getX(), area0.getY());
				Logger.debug("TargetPoint: centroid of current area :(");
			}
		} else {
			Edge frontier = area0.getEdgeTo(area1.getID());

			target = new Point2D(
				frontier.getStartX() + (frontier.getEndX() - frontier.getStartX()) / 2,
				frontier.getStartY() + (frontier.getEndY() - frontier.getStartY()) / 2
			);
			Logger.debug("TargetPoint: midpoint of frontier with next Area :)");
		}
		
		return target;
    }
    
    
    
    /**
     * Returns the list of Buildings surrounded by blockades
     * @param currentTime
     * @return
     */
    private List<EntityID> getBuildingsToVisit(int currentTime){
    	List<EntityID> buildings = new ArrayList<>();
    	
    	List<EntityID> neighbors = ((Area) this.location).getNeighbours();
    	
    	for(EntityID neigh : neighbors){
    		List<EntityID> neighborsOfneighbors = ((Area) world.getEntity(neigh)).getNeighbours();
    		
    		for(EntityID neighOfneigh : neighborsOfneighbors){
    			if(((Area) world.getEntity(neighOfneigh)) instanceof Building){
    				Area neighArea = (Area) world.getEntity(neigh);
    				if (neighArea.isBlockadesDefined() && ! neighArea.getBlockades().isEmpty()){
    					buildings.add(neighOfneigh);
    				}
    			}
    		}
    	}
    	return buildings;
    }
    
    private boolean stuckClearLoop(int currentTime){
    	if (tacticsAgent.commandHistory.size() < 4){
    		Logger.info("Insufficient commands in history");
    		return false;
    	}
    	Action lastCmd = null;
    	for(int backtime = 1; backtime <= 4; backtime++){
    		if (lastCmd == null){
    			lastCmd = tacticsAgent.commandHistory.get(currentTime - backtime);
    		}
    		
    		Logger.trace(String.format("backtime=%d, lastCmd=%s, currCmd=%s", backtime, lastCmd, tacticsAgent.commandHistory.get(currentTime - backtime)));
    		
    		if (!lastCmd.equals(tacticsAgent.commandHistory.get(currentTime - backtime))){
    			return false;
    		}
    		lastCmd = tacticsAgent.commandHistory.get(currentTime - backtime);
    		
    	}
    	
    	return true;
    }
    
    private boolean checkBlockadeOnWayTo(List<EntityID> dest_path, BlockedArea bTarget) {
		
		//EntityID dest = dest_path.get(0);
	
		Point2D target = getTargetPoint(dest_path, bTarget);

		//CHECK IF DISTANCE TO FRONTIER IS SHORT
		Vector2D agentToTarget = new Vector2D(target.getX() - me().getX(), target.getY() - me().getY());
		/*Logger.debug("Distance to midpoint: " + agentToTarget.getLength());
		if (agentToTarget.getLength() < 200){
			Area area1 = (Area) this.world.getEntity(dest_path.get(0));
			Logger.warn("Mid point of frontier is very close, will aim to next area's centroid");
			target = new Point2D(area1.getX(), area1.getY());
			agentToTarget = new Vector2D(target.getX() - me().getX(), target.getY() - me().getY());
		}*/
		
		Vector2D normalagentToTarget = agentToTarget.normalised();
		Vector2D escalar = normalagentToTarget.scale(clearRange);
		target = new Point2D(me.getX() + escalar.getX(),me.getY() + escalar.getY());
		//System.out.println("frontier: " + frontier);
		Logger.trace("target: " + target);
		ArrayList<Blockade> blockList = new ArrayList<Blockade>(blockadeUtil.getBlockadesInSquare(me().getX(), me().getY(), clearRange));
		Logger.trace("#blockades in square around agent: " + blockList.size());
		Logger.trace("They are: " + blockList);
		
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
	
	@Override
	public HeatMap initializeHeatMap() {
    	HeatMap heatMap = new HeatMap(this.agentID, this.world);
        for (Entity next : this.getWorld()) {
            if (next instanceof Area) {
            	// Ignore very small areas to explore
            	//if(GeometricUtil.getAreaOfEntity(next.getID(), this.world) < EXPLORE_AREA_SIZE_TRESH) continue;
            	
            	// Ignore non Road areas
            	if(!(next instanceof Road)) continue;
            	
            	heatMap.addEntityID(next.getID(), HeatNode.PriorityLevel.LOW, 0);
            }
        }
        
        return heatMap;
	}

	@Override
	public Action failsafeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
		// Am I near a blockade?
        Blockade target = failSafeGetTargetBlockade();
        if (target != null) {
            Logger.info("Clearing blockade " + target);
            List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(target.getApexes()), true);
            double best = Double.MAX_VALUE;
            Point2D bestPoint = null;
            Point2D origin = new Point2D(me().getX(), me().getY());
            for (Line2D next : lines) {
                Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
                double d = GeometryTools2D.getDistance(origin, closest);
                if (d < best) {
                    best = d;
                    bestPoint = closest;
                }
            }
            Vector2D v = bestPoint.minus(new Point2D(me().getX(), me().getY()));
            v = v.normalised().scale(1000000);
            return new ActionClear(this, (int)(me().getX() + v.getX()), (int)(me().getY() + v.getY()));
        }
        // Plan a path to a blocked area
        EntityID tgt = failSafeGetBlockedRoad();
        if (tgt != null) {
	        List<EntityID> path = routeSearcher.getPath(currentTime, me().getPosition(), tgt);
	        if (path != null) {
	            Logger.info("Moving to target");
	            Road r = (Road)model.getEntity(path.get(path.size() - 1));
	            Blockade b = failSafeGetTargetBlockade(r, -1);
	            Logger.debug("Path: " + path);
	            Logger.debug("Target coordinates: " + b.getX() + ", " + b.getY());
	            return new ActionMove(this, path, b.getX(), b.getY());
	        }
        }
        Logger.debug("Couldn't plan a path to a blocked road.");
	    Logger.info("Moving randomly.");
	    return new ActionMove(this, routeSearcher.noTargetMove(currentTime, this.location.getID()));
        
        
    }

	private EntityID failSafeGetBlockedRoad() {
        Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.ROAD);
        for (StandardEntity next : e) {
            Road r = (Road)next;
            if (r.isBlockadesDefined() && !r.getBlockades().isEmpty()) {
                return r.getID();
            }
        }
        return null;
    }

    private List<EntityID> failSafeGetBlockedRoads() {
        Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.ROAD);
        List<EntityID> result = new ArrayList<EntityID>();
        for (StandardEntity next : e) {
            Road r = (Road)next;
            if (r.isBlockadesDefined() && !r.getBlockades().isEmpty()) {
                result.add(r.getID());
            }
        }
        return result;
    }

    private Blockade failSafeGetTargetBlockade() {
        Logger.debug("Looking for target blockade");
        Area location = (Area)location();
        Logger.debug("Looking in current location");
        Blockade result = failSafeGetTargetBlockade(location, clearRange - 1000);
        if (result != null) {
            return result;
        }
        Logger.debug("Looking in neighbouring locations");
        for (EntityID next : location.getNeighbours()) {
            location = (Area)model.getEntity(next);
            result = failSafeGetTargetBlockade(location, clearRange - 1000);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Blockade failSafeGetTargetBlockade(Area area, int maxDistance) {
        //        Logger.debug("Looking for nearest blockade in " + area);
        if (area == null || !area.isBlockadesDefined()) {
            //            Logger.debug("Blockades undefined");
            return null;
        }
        List<EntityID> ids = area.getBlockades();
        // Find the first blockade that is in range.
        int x = me().getX();
        int y = me().getY();
        for (EntityID next : ids) {
            Blockade b = (Blockade)model.getEntity(next);
            double d = failSafeFindDistanceTo(b, x, y);
            //            Logger.debug("Distance to " + b + " = " + d);
            if (maxDistance < 0 || d < maxDistance) {
                //                Logger.debug("In range");
                return b;
            }
        }
        //        Logger.debug("No blockades in range");
        return null;
    }

    private int failSafeFindDistanceTo(Blockade b, int x, int y) {
        //        Logger.debug("Finding distance to " + b + " from " + x + ", " + y);
        List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
        double best = Double.MAX_VALUE;
        Point2D origin = new Point2D(x, y);
        for (Line2D next : lines) {
            Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
            double d = GeometryTools2D.getDistance(origin, closest);
            //            Logger.debug("Next line: " + next + ", closest point: " + closest + ", distance: " + d);
            if (d < best) {
                best = d;
                //                Logger.debug("New best distance");
            }

        }
        return (int)best;
    }
}