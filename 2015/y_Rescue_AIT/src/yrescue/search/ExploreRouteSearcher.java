package yrescue.search;

import adk.team.util.RouteSearcher;
import adk.team.util.graph.RouteEdge;
import adk.team.util.graph.RouteGraph;
import adk.team.util.graph.RouteManager;
import adk.team.util.graph.RouteNode;
import adk.team.util.provider.WorldProvider;

import com.google.common.collect.Lists;

import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class ExploreRouteSearcher implements RouteSearcher {

    private WorldProvider<? extends Human> provider;
    private RouteManager routeManager;
    private Set<EntityID> visitedEntities = new HashSet<>(); 
    private Map<EntityID, Set<EntityID>> neighbours;
    private EntityID lastIDVisited = null;
    protected boolean DEBUG	= false;

    private Random random;

    public ExploreRouteSearcher(WorldProvider<? extends Human> worldProvider, RouteManager manager) {
        this.provider = worldProvider;
        this.routeManager = manager;
        this.random = new Random((new Date()).getTime());
        initRandomWalk();
    }
    
    public boolean addVisitedEntity(EntityID entityID){
    	return visitedEntities.add(entityID);
    }
    
    private void initRandomWalk() {
        this.neighbours = new LazyMap<EntityID, Set<EntityID>>() {
            @Override
            public Set<EntityID> createValue() {
                return new HashSet<>();
            }
        };
        for (Entity next : this.provider.getWorld()) {
            if (next instanceof Area) {
                Set<EntityID> roadNeighbours = new HashSet<>();
                for(EntityID areaID : ((Area)next).getNeighbours()) {
                    StandardEntity area = this.provider.getWorld().getEntity(areaID);
                    if(area instanceof Road || area instanceof Refuge) {
                        roadNeighbours.add(areaID);
                    }
                }
                this.neighbours.put(next.getID(), roadNeighbours);
            }
        }
    }
    

    @Override
    public List<EntityID> getPath(int time, EntityID startID, EntityID goalID) {
    	return noTargetMove(time, startID);
//    	
//    	System.out.println("In getPath");
//    	StandardWorldModel world = this.provider.getWorld();
//        RouteGraph graph = this.routeManager.getPassableGraph();
//        // check
//        if(startID.getValue() == goalID.getValue()) {
//            return Lists.newArrayList(startID);
//        }
//        // init
//        Set<EntityID> closed = new HashSet<>();
//        List<RouteNode> open = new ArrayList<>();
//        Map<EntityID, EntityID> previousNodeMap = new HashMap<>();
//        Map<EntityID, Double> distanceFromStart = new HashMap<>();
//        if(graph.containsEdge(startID)) {
//            RouteEdge edge = graph.getEdge(startID);
//            RouteNode node = graph.getNode(edge.firstNodeID);
//            if(node.getNeighbours().size() <= 1 && (goalID.getValue() != edge.firstNodeID.getValue())) {
//                closed.add(edge.firstNodeID);
//            }
//            node = graph.getNode(edge.secondNodeID);
//            if(node.getNeighbours().size() <= 1 && (goalID.getValue() != edge.secondNodeID.getValue())) {
//                closed.add(edge.secondNodeID);
//            }
//
//        }
//
//        if(!graph.createPositionNode(world, startID) || !graph.createPositionNode(world, goalID)) {
//            return null;
//        }
//        RouteNode start = graph.getNode(startID);
//        if(start.isSingleNode()) {
//            return null;
//        }
//        RouteNode goal = graph.getNode(goalID);
//        if(goal.isSingleNode()) {
//            return null;
//        }
//        open.add(start);
//        // process
//        while(open.size() != 0) {
//            RouteNode current = open.get(0); //sort
//            EntityID currentID = current.nodeID;
//            //目的地に着いた時
//            if(currentID.getValue() == goalID.getValue()) {
//                List<RouteNode> nodePath = Lists.newArrayList(current);
//                EntityID id = currentID;
//                while(previousNodeMap.containsKey(id)) {
//                    id = previousNodeMap.get(id);
//                    nodePath.add(graph.getNode(id));
//                }
//                Collections.reverse(nodePath);
//                return graph.getPath(nodePath); // create path
//            }
//            // reset
//            open.clear();
//            closed.add(currentID);
//            // search next
//            for(EntityID neighbourID : current.getNeighbours()) {
//                if (closed.contains(neighbourID)) {
//                    continue;
//                }
//                RouteNode neighbour = graph.getNode(neighbourID);
//                double currentDistance = distanceFromStart.containsKey(currentID) ? distanceFromStart.get(currentID) : 0.0D;
//                double neighbourDistance = currentDistance + graph.getEdge(current, neighbour).getDistance();
//                if(!open.contains(neighbour)) {
//                    open.add(neighbour);
//                    previousNodeMap.put(neighbourID, currentID);
//                    distanceFromStart.put(neighbourID, neighbourDistance);
//                }
//            }
//            open.sort(new AStarDistanceComparator(goal, distanceFromStart));
//        }
//        return null;
    }


    @Override
    public List<EntityID> noTargetMove(int time, EntityID from) {
    	visitedEntities.add(from);
    	
    	List<EntityID> result = new ArrayList<>(50);
        Set<EntityID> seen = new HashSet<>();
        EntityID current = from;//this.provider.getOwner().getPosition();
        for (int i = 0; i < 50; ++i) {
            result.add(current);
            seen.add(current);
            List<EntityID> possible = new ArrayList<>(this.neighbours.get(current));
            Collections.shuffle(possible, this.random);
            boolean noTarget = true;
            for (EntityID next : possible) {
            	StandardEntity sEnt = provider.getWorld().getEntity(next);
            	
            	// Calculate the area of the road segment
            	// Visit only the new roads that aren't small segments like small hallways or 'doors'
            	// Do not visit previously visited areas or buildings
            	
            	Area area1 = (Area) sEnt;
            	Shape myshape = area1.getShape();
            	
            	Rectangle2D myrect = myshape.getBounds2D();
            	
            	double[] x = {myrect.getX(), myrect.getX() + myrect.getWidth(), myrect.getX() + myrect.getWidth(), myrect.getX()};
            	double[] y = {myrect.getY(), myrect.getY(), myrect.getY() + myrect.getHeight(), myrect.getY() + myrect.getHeight()};
            	
            	double sum = 0;
            	for (int i2 = 0; i2 < 4 -1; i2++)
            	{
            	    sum = sum + x[i2]*y[i2+1] - y[i2]*x[i2+1];
            	}
            	double calculatedArea = (sum / 2);
            	
            	if(DEBUG) System.out.printf("%d) area: %.0f\n",next.getValue(), (sum / 2));

                if (seen.contains(next) || sEnt instanceof Building || calculatedArea < 170000000 || visitedEntities.contains(next)){ 
                		//(lastIDVisited != null && next.getValue() == lastIDVisited.getValue())) { //|| (visitedEntities.contains(next) && from.getValue() != next.getValue())
                    continue;
                }
                current = next;
                noTarget = false;
                break;
            }
            if (noTarget) {
                break;
            }
        }
        
        //provider.getWorld().getEntity(current) instanceof 
        /*if(result.size() > 0){
        	visitedEntities.add(from);
        }
        
        System.out.println("From:" + from.getValue());
        for(EntityID entity : visitedEntities){
        	System.out.println("Visited ID:" + entity.getValue());
        }*/
        
        lastIDVisited = from;
        if(DEBUG){
	        for(EntityID entity : result){
	        	System.out.println("Result ID:" + entity.getValue());
	        }
        }
        
        return result;
    }
}
