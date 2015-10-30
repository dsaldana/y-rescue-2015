package yrescue.tactics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;



import org.apache.log4j.MDC;

import com.vividsolutions.jts.geom.Geometry;

import adk.sample.basic.event.BasicRoadEvent;
import adk.sample.basic.tactics.BasicTacticsPolice;
import adk.sample.basic.util.BasicRouteSearcher;
import adk.team.action.Action;
import adk.team.action.ActionClear;
import adk.team.action.ActionMove;
import adk.team.action.ActionRest;
import adk.team.util.ImpassableSelector;
import adk.team.util.RouteSearcher;
import adk.team.util.graph.PositionUtil;
import comlib.manager.MessageManager;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import comlib.message.information.MessagePoliceForce;
import jdk.nashorn.internal.runtime.linker.JavaAdapterFactory;
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
import rescuecore2.standard.entities.GasStation;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.properties.IntArrayProperty;
import yrescue.heatmap.HeatMap;
import yrescue.heatmap.HeatNode;
import yrescue.kMeans.KMeans;
import yrescue.message.event.MessageBlockedAreaEvent;
import yrescue.problem.blockade.BlockadeUtil;
import yrescue.problem.blockade.BlockedArea;
import yrescue.problem.blockade.BlockedAreaSelector;
import yrescue.problem.blockade.BlockedAreaSelectorProvider;
import yrescue.statemachine.ActionStates;
import yrescue.statemachine.StateMachine;
import yrescue.statemachine.StatusStates;
import yrescue.util.GeometricUtil;
import yrescue.util.PathUtil;
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
    public List<EntityID> visitedBuildingsandDoors;
    
    //Stores when entities were last visited
    //protected Map<EntityID, Integer> lastVisit;
    //protected CircularFifoQueue<EntityID> lastVisitQueue = new CircularFifoQueue<EntityID>(20);
    
    protected List<EntityID> clusterToVisit;
	protected EntityID clusterCenter;

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
        this.visitedBuildingsandDoors = new ArrayList<EntityID>();
        
        MDC.put("agent", this);
        MDC.put("location", location());
        
        clusterToVisit = new LinkedList<EntityID>();
    	List<StandardEntity> policeList = new ArrayList<StandardEntity>(this.getWorld().getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
    	KMeans kmeans = new KMeans(policeList.size());
    	Map<EntityID, EntityID> kmeansResult = kmeans.calculatePartitions(this.getWorld());
    	
    	List<EntityID> partitions = kmeans.getPartitions();
    	
    	policeList.sort(new Comparator<StandardEntity>() {
			@Override
			public int compare(StandardEntity o1, StandardEntity o2) {
				return Integer.compare(o1.getID().getValue(), o2.getID().getValue());
			}
		});
    	
    	partitions.sort(new Comparator<EntityID>() {
			@Override
			public int compare(EntityID o1, EntityID o2) {
				return Integer.compare(o1.getValue(), o2.getValue());
			}
		});
    	
    	if(policeList.size() == partitions.size()){
    		int pos = -1;
    		for(int i = 0; i < policeList.size(); i++){
    			if(me.getID().getValue() == policeList.get(i).getID().getValue()){
    				pos = i;
    				break;
    			}
    		}
    		
    		if(pos != -1){
    			clusterCenter = partitions.get(pos);
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

    	Logger.info("Cluster to visit :" + clusterToVisit);
    	if(clusterToVisit.size() > 0){
    		Building b = (Building) world.getEntity(clusterCenter);
    		BlockedArea ba = new BlockedArea(clusterCenter, clusterCenter, b.getX(), b.getY());
    		this.blockedAreaTarget = ba;
    		this.blockedAreaSelector.add(ba);
    	}
        
        Logger.info("Preparation complete!");
    }
    
    private void updateVisitHistory(){
    	  	
    	IntArrayProperty positionHist = (IntArrayProperty) me().getProperty("urn:rescuecore2.standard:property:positionhistory");
    	int[] positionList = positionHist.getValue();

    	Logger.debug(("Position hist: " + positionHist));
    	if (!positionHist.isDefined()) {
    		Logger.debug("Empty position list. I'm (possibly stopped) at " + location());
    		return;
    	}
    	
    	for (int i = 0; i < positionList.length; i++){
    		int x = positionList[i];
    		int y = positionList[i+1];
    		i++;
    		
    		Collection<StandardEntity> intersectz = model.getObjectsInRectangle(x, y, x, y);	//obtains the object that intersect with a point where the agent has been
    		
    		//Logger.info(String.format("Found %d entities @ (%d,%d)", intersectz.size(), x, y));
    		Logger.trace("Visited Areas" +intersectz);
    		for(StandardEntity entity: intersectz){
    			if (entity instanceof Area) {
    				//updates
    				visitedBuildingsandDoors.add(entity.getID());	
    			}
    		}
    	}
    }
    

    @Override
    public void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager) {
    	updateVisitHistory();
    	
    	//marks building as visited
    	if (location instanceof Building){
    		heatMap.updateNode(location.getID(), currentTime);
    	}
    	
    	//also mark the building as visited if standing on its door and way is cleared
    	for (EntityID id : ((Area)location).getNeighbours()){
    		Area neighbor = (Area) world.getEntity(id);
    		
    		if(neighbor instanceof Building){
    			List<EntityID> mineAndBuilding = new ArrayList<>();
    			mineAndBuilding.add(location.getID());
    			mineAndBuilding.add(neighbor.getID());
    		
				//if there is no blockade on way to building, mark it as visited
				if(!checkBlockadeOnWayTo(mineAndBuilding, new BlockedArea(neighbor.getID(), neighbor.getID(), neighbor.getX(), neighbor.getY()))){
					Logger.debug("Marked a building as visited without entering it. " + neighbor);
					heatMap.updateNode(neighbor.getID(), currentTime);
					break;
				}
    		}
    	}
    	
    	
    	
    	for (EntityID next : updateWorldInfo.getChangedEntities()) {
            StandardEntity entity = this.getWorld().getEntity(next);
            /*if(entity instanceof Blockade) {
                this.impassableSelector.add((Blockade) entity);
            }
            else*/ 
            if(entity instanceof Civilian) {
            	this.reportCivilian((Civilian) entity, manager, currentTime);
            }
            else if(entity instanceof Building) {
                Building b = (Building)entity;
                if(b.isOnFire()) {
                    manager.addSendMessage(new MessageBuilding(b));
                }

                if(b.getFierynessEnum().equals(StandardEntityConstants.Fieryness.BURNT_OUT)){
                	Logger.trace("Removing completely burnt Building from heatMap" + b);
                	heatMap.removeEntityID(b.getID());
                }
                /*else {
                	heatMap.updateNode(b.getID(), currentTime);
                }*/
            }
        }
    }

    public void ignoreTimeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    	Logger.debug("\nRadio channel: " + manager.getRadioConfig().getChannel());
    	if(this.me.getBuriedness() > 0) {
            this.buriednessAction(manager);
        }
    }
    
    
    public boolean checkBlockadesAround(){
    	if(location instanceof Area){
    		Area temp = (Area)this.location;
    		List<EntityID> listOfBlockades = temp.getBlockades();
    		if(listOfBlockades != null){
		    	for(EntityID e : listOfBlockades ){
		    		Blockade b = (Blockade)world.getEntity(e);
		    		java.awt.Polygon pol = new java.awt.Polygon();
		    		int [] apexes = b.getApexes();
		    		for(int i = 0; i < apexes.length; i++){
		    			int x = apexes[i];
		    			int y = apexes[i+1];
		    			i++;
		    			pol.addPoint(x, y);
		    		}
		    		if(pol.contains(me.getX(), me.getY())) return true;
		    	}
    		}
    	}
    	return false;
        
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
        
        Logger.trace("The heatmap " +heatMap);
    	if(heatMap == null){
    		Logger.warn("WARNING: null heatmap. Will build a new one");
    		heatMap = initializeHeatMap();
    	}
        
        //heatMap.writeMapToFile();
        
        Logger.trace("The received message: " + manager.getReceivedMessage());
        
        //if I am buried, send a message and attempt to clear the entrance to my building
        if(this.me.getBuriedness() > 0) {
            return this.buriednessAction(manager);
        }
        
        Logger.debug("                          BLOCKADE AROUND TESTING!!!");
        if(checkBlockadesAround()){
        	Blockade closest = BlockadeUtil.getClosestBlockadeInMyRoad(this);
        	Logger.debug("                          BLOCKADE AROUND DETECTED!!!");
        	return new ActionClear(this,closest);
        }
        
        if (this.tacticsAgent.stuck(currentTime)){
        	Logger.warn("I'm STUCK! How's that possible?");
        	Blockade closest = BlockadeUtil.getClosestBlockadeInMyRoad(this);
        	if(closest == null){
        		return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, location.getID()));
        	}
        	return new ActionClear(this, closest);//closest.getX(), closest.getY() );
        }
        
        if(this.me.getDamage() >= 100) { 
        	return moveRefuge(currentTime);
        	
        }
        
        //Ensuring the police stays in the refuge until the damage drops to 0
        Refuge result = PositionUtil.getNearTarget(this.world, this.me, this.getRefuges());
        EntityID results = result.getID();           
        EntityID local = this.location.getID();        
		if ((local == results) && (this.me.getDamage() > 0)) {
			return new ActionRest(this);
		}
        
        if(this.stuckClearLoop(currentTime)) {
        	Logger.warn("Warning: clearing the same position for more than 3 timesteps");
        	EntityID theTarget = null;
        	if(blockedAreaTarget != null){
        		theTarget =  this.blockedAreaTarget.getOriginID();
        		
        	}
        	else{
        		Logger.info("I have no target, using heatmap exploration...");
        		theTarget = heatMap.getNodeToVisit();
        	}
        	List<EntityID> path = this.routeSearcher.getPath(currentTime, location.getID(), theTarget);
    		PathUtil.makeSafePath(this, path);
    		return new ActionMove(this, path);
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
        
        Logger.info("#blocked roads: " + blockedAreaSelector.blockedAreas.size());
        Logger.debug("They are: " + blockedAreaSelector.blockedAreas.values());
        
        /*
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
                int index = ran.nextInt(4);
                int index2;
                //Logger.debug("\n"+"\n"+"\n");
                //Logger.debug("INDEX " +index);
                //Logger.debug("\n"+"\n"+"\n");
                if (index == 0 && (this.getWorld().getEntitiesOfType(StandardEntityURN.ROAD).size() != 0)){ //ROADS
                	Collection<StandardEntity> areas0 = this.getWorld().getEntitiesOfType(StandardEntityURN.ROAD);
                    index2 = ran.nextInt(areas0.size());
                    randomDestination = areas0.toArray(new StandardEntity[0])[index2].getID();
                    //Logger.debug("\n"+"\n"+"\n");
                    //Logger.debug("ROAD " +randomDestination);
                    //Logger.debug("\n"+"\n"+"\n");
                }
                if (index == 1 && (this.getWorld().getEntitiesOfType(StandardEntityURN.BUILDING).size()!= 0)){ //BUILDINGS
                    Collection<StandardEntity> areas1 = this.getWorld().getEntitiesOfType(StandardEntityURN.BUILDING);
                    index2 = ran.nextInt(areas1.size());
                    randomDestination = areas1.toArray(new StandardEntity[0])[index2].getID();
                    //Logger.debug("\n"+"\n"+"\n");
                    //Logger.debug("BUILDING " +randomDestination);
                    //Logger.debug("\n"+"\n"+"\n");
                }
                if (index == 2 && (this.getWorld().getEntitiesOfType(StandardEntityURN.GAS_STATION).size() != 0)){ //GAS_STATIONS
                    Collection<StandardEntity> areas2 = this.getWorld().getEntitiesOfType(StandardEntityURN.GAS_STATION);
                    index2 = ran.nextInt(areas2.size());
                    randomDestination = areas2.toArray(new StandardEntity[0])[index2].getID();
                    //Logger.debug("\n"+"\n"+"\n");
                    //Logger.debug("GAS_STATION " +randomDestination);
                    //Logger.debug("\n"+"\n"+"\n");
                }
                if (index == 3 && (this.getWorld().getEntitiesOfType(StandardEntityURN.HYDRANT).size() != 0)){ // HYDRANTS
                    Collection<StandardEntity> areas3 = this.getWorld().getEntitiesOfType(StandardEntityURN.HYDRANT);
                    index2 = ran.nextInt(areas3.size());
                    randomDestination = areas3.toArray(new StandardEntity[0])[index2].getID();
                    //Logger.debug("\n"+"\n"+"\n");
                    //Logger.debug("HYDRANT " +randomDestination);
                    //Logger.debug("\n"+"\n"+"\n");
                }
            } else {
        		
            	int index = ran.nextInt(blockedRefuges.size());
            	randomDestination = blockedRefuges.get(index);
        	}
        	
        	Area a = (Area) this.world.getEntity(randomDestination);
        	
        	this.getBlockedAreaSelector().add(new BlockedArea(randomDestination, null, a.getX(), a.getY()));
        }*/
       // java.awt.geom.Arc2D
    
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
        	
        	
        	//System.out.println("The heatmap " +heatMap);
        	if(heatMap == null){
        		Logger.warn("WARNING: null heatmap. Will build a new one");
        		heatMap = initializeHeatMap();
        	}
        	path = this.routeSearcher.getPath(currentTime, me, heatMap.getNodeToVisit());// noTargetMove(currentTime, this.me);
        	Logger.debug(String.format("HeatMap exploration. Tgt: %s; path: %s",  heatMap.getNodeToVisit(), path));
        } else {
        	path = this.routeSearcher.getPath(currentTime, this.me, this.blockedAreaTarget.getOriginID());
        	Logger.debug("Path to target: " + path);
        }
        
        PathUtil.makeSafePath(this, path);
        
        /*
        //------Pegar a lista de predios a visitar:
        
        //TODO: Melhorar a selecão de prédios pra visitar.
        
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
        */
       
        
        /**** Go towards the chosen path ****/
        
        List<EntityID> neighbours = new ArrayList<EntityID>();
        Area area0 = (Area) this.world.getEntity(this.location.getID());
        neighbours  = area0.getNeighbours();
        for(EntityID A : neighbours){
        	if((Area) world.getEntity(A) instanceof Building){
        		this.visitedBuildingsandDoors.add(A);
        		this.visitedBuildingsandDoors.add(this.location.getID());
        		List<EntityID> path2 = new ArrayList<EntityID>();
        		path2.add(A);
        		if(checkBlockadeOnWayTo(path2, blockedAreaTarget))	{
        			Point2D target = getTargetPoint(path2, blockedAreaTarget);
        			Vector2D agentToTarget = new Vector2D(target.getX() - me().getX(), target.getY() - me().getY());
        			
        			agentToTarget = agentToTarget.normalised();
        			agentToTarget = agentToTarget.scale(clearRange);
        			target = new Point2D(target.getX() + agentToTarget.getX(), target.getY() + agentToTarget.getY());
        			
        		Logger.info("Door is blockade.");
        			
        			actionStateMachine.setState(ActionStates.Policeman.CLEARING);
            		statusStateMachine.setState(StatusStates.ACTING);
                	return new ActionClear(this, (int)target.getX(), (int)target.getY());
        		}
        			
        		
        	}
        }
        
        
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
    		
    		
    		///Testing if the cop will visit burning buildings
    		PathUtil.makeSafePath(this, path);
    		   		
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
    
    @Override
    public Action moveRefuge(int currentTime) {
        Refuge result = PositionUtil.getNearTarget(this.world, this.me, this.getRefuges());
        List<EntityID> path = routeSearcher.getPath(currentTime, this.me(), result);
        
        Logger.trace(String.format("moveRefuge called. dest=%s, path=%s, me=%s", result, path, this.me()));
        
        return new ActionMove(this, path != null ? path : routeSearcher.noTargetMove(currentTime, this.me()));
    }
    
    private Action buriednessAction(MessageManager manager) {
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
    		if(this.visitedBuildingsandDoors.contains(neighbors)) continue;
    		for(EntityID neighOfneigh : neighborsOfneighbors){
    			if(((Area) world.getEntity(neighOfneigh)) instanceof Building){
    				Area neighArea = (Area) world.getEntity(neigh);
    				if (neighArea.isBlockadesDefined() && ! neighArea.getBlockades().isEmpty() && !this.visitedBuildingsandDoors.contains(neighArea.getID())){
    					buildings.add(neighOfneigh);
    				}
    			}
    		}
    	}
    	return buildings;
    }
    
    private boolean stuckClearLoop(int currentTime){
    	if (tacticsAgent.commandHistory.size() < 4){
    		Logger.debug("Insufficient commands in history");
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
    	this.heatMap = new HeatMap(this.agentID, this.world);
        for (Entity next : this.getWorld()) {
            if (next instanceof Building) {
            	// Ignore very small areas to explore
            	//if(GeometricUtil.getAreaOfEntity(next.getID(), this.world) < EXPLORE_AREA_SIZE_TRESH) continue;
            	
            	// Ignore non Road areas
            	//if(!(next instanceof Road)) continue;
            	
            	if (next instanceof Refuge && this.clusterToVisit.contains(next.getID())){
            		heatMap.addEntityID(next.getID(), HeatNode.PriorityLevel.LOW, 0);
            	}
            	
            	else if (next instanceof GasStation && this.clusterToVisit.contains(next.getID())){
            		heatMap.addEntityID(next.getID(), HeatNode.PriorityLevel.LOW, 0);
            	}
            	
            	else {
            		heatMap.addEntityID(next.getID(), HeatNode.PriorityLevel.VERY_LOW, 0);
            	}
            }
        }
        
        return heatMap;
	}

	@Override
	public Action failsafeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
		// Am I near a blockade?
        Blockade target = failSafeGetTargetBlockade();
        if (target != null) {
            Logger.info("FAILSAFE: Clearing blockade " + target);
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
	            Logger.info("FAILSAFE: Moving to target");
	            Road r = (Road)model.getEntity(path.get(path.size() - 1));
	            Blockade b = failSafeGetTargetBlockade(r, -1);
	            Logger.debug("FAILSAFE: Path: " + path);
	            Logger.debug("FAILSAFE: Target coordinates: " + b.getX() + ", " + b.getY());
	            return new ActionMove(this, path, b.getX(), b.getY());
	        }
        }
        Logger.debug("FAILSAFE: Couldn't plan a path to a blocked road.");
	    Logger.info("FAILSAFE: Moving randomly.");
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
        Logger.debug("FAILSAFE: Looking for target blockade");
        Area location = (Area)location();
        Logger.debug("FAILSAFE: Looking in current location");
        Blockade result = failSafeGetTargetBlockade(location, clearRange - 1000);
        if (result != null) {
            return result;
        }
        Logger.debug("FAILSAFE: Looking in neighbouring locations");
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
