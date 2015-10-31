package yrescue.problem.blockade;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import yrescue.tactics.YRescueTacticsPolice;
import yrescue.util.YRescueDistanceSorter;
import adk.team.tactics.Tactics;
import adk.team.util.provider.WorldProvider;

/**
 * A collection of useful methods to deal with blockades
 *
 */
public class BlockadeUtil {
	
	YRescueTacticsPolice owner;
	
	public BlockadeUtil(YRescueTacticsPolice owner){ 
		this.owner = owner;
	}	
	
	public static Blockade getClosestBlockadeInMyRoad(Tactics<?> t){
		if(!(t.location instanceof Road)){
			Logger.debug("I'm not in a Road, so there are no blockades");
			return null;
		}
		Road myRoad = (Road) t.location;
		
		Blockade closest = null;
		int closestDistance = Integer.MAX_VALUE;
		Logger.debug("myRoad.getBlockades: " + myRoad.getBlockades());
		if (myRoad.isBlockadesDefined()){
			for (EntityID blockID : myRoad.getBlockades()){
				int distance = t.getWorld().getDistance(t.agentID, blockID);
				if(distance < closestDistance){
					closest = (Blockade) t.getWorld().getEntity(blockID);
					closestDistance = distance;
				}						
			}
		}
		
		return closest;
	}
	
	public static Blockade getClosestBlockade(EntityID area,Tactics<?> agent,int X,int Y){
		Area area0 = (Area) agent.world.getEntity(area);
		List<EntityID> blocks = new ArrayList<>();
		
		if(area0.getBlockades() != null){
			Logger.debug("LIST BLOCKS 1 :::::" + area0.getBlockades());
			blocks.addAll(area0.getBlockades());
		}
		for(EntityID next : area0.getNeighbours()){
			Area area1 = (Area)agent.world.getEntity(next);
			if(area1 == null){
				continue;
			}
			Logger.debug("Area1 is ::::::: " + area1);
			Logger.debug("Blockades are ::::::: " + area1.getBlockades());
			List<EntityID> list1 = area1.getBlockades();
			//blocks.addAll(list1);
			if(list1 != null){
				blocks.addAll(list1);
			}
		}
		Logger.debug("LIST BLOCKS 2 :::::" + blocks);
		List<Vector2D> distances = new ArrayList<>();
		Blockade closest = null;
		double close,far;
		close = Double.MAX_VALUE;
		far = Double.MIN_VALUE;
		for(EntityID next : blocks){
			Blockade next1 = (Blockade)agent.world.getEntity(next);
			Vector2D d = new Vector2D(next1.getX() - X,next1.getY() - Y);
			distances.add(d); 
		}
		//Get closest and furthest blockades on the list.
		for(int i =0; i < distances.size(); i++){
			if(distances.get(i).getLength() < close){
				close = distances.get(i).getLength();
				closest = (Blockade)agent.world.getEntity(blocks.get(i));	
			}
		}
		return closest;
	}
	
	
	
	public static boolean isPassable(Area from, Area to,Tactics<?> agent){
		boolean passable = false;
		List<EntityID> blocks = from.getBlockades();
		List<Vector2D> distances = new ArrayList<>();
		List<Polygon> polygons = new ArrayList<>();
		Blockade closest = null,furthest = null;
		double close,far;
		close = Double.MAX_VALUE;
		far = Double.MIN_VALUE;
		for(EntityID next : blocks){
			Blockade next1 = (Blockade)agent.world.getEntity(next);
			Vector2D d = new Vector2D(next1.getX() - agent.me().getX(),next1.getY() - agent.me().getY());
			distances.add(d); 
		}
		while(distances.size() > 1){
			//Get closest and furthest blockades on the list.
			for(int i =0; i < distances.size(); i++){
				if(distances.get(i).getLength() < close){
					close = distances.get(i).getLength();
					closest = (Blockade)agent.world.getEntity(blocks.get(i));
					
				}
				else if(distances.get(i).getLength() > far){
					far = distances.get(i).getLength();
					furthest = (Blockade)agent.world.getEntity(blocks.get(i));
				}
			}
			Polygon p = yrescue.util.GeometricUtil.getPolygon(closest.getApexes());
			
		}
		
		
		return passable;
	}
	
	public static List<EntityID> getNearBlockades(Tactics<?> agent){
		List<EntityID> blockades = new ArrayList<>();
		
        Area location = (Area)agent.location;
        
        if(location.isBlockadesDefined()){
        	blockades.addAll(location.getBlockades());
        }
        
        for(EntityID neigh : location.getNeighbours()){
        	StandardEntity e = agent.world.getEntity(neigh);
        	
        	if(e instanceof Area){
        		Area a = (Area) e;
        		if(a.isBlockadesDefined()) {
        			blockades.addAll(a.getBlockades());
        		}
        	}
        }
        
        return blockades;
	}
	
	public static Point2D calculateNavigationMove(Tactics<?> stuckAgent){
		
		Point2D agentPoint = new Point2D(stuckAgent.me().getX(), stuckAgent.me().getY());
		
		List<Vector2D> repulsions = new ArrayList<>();
		
		for(EntityID blkID : BlockadeUtil.getNearBlockades(stuckAgent)){
			Blockade b = (Blockade) stuckAgent.world.getEntity(blkID);
			if(b.isPositionDefined()){
				
				Point2D repulsionOrigin = new Point2D(b.getX(), b.getY());
				double distance = GeometryTools2D.getDistance(agentPoint, repulsionOrigin);
				
				Vector2D repulsion = new Vector2D(
						agentPoint.getX() - repulsionOrigin.getX(), agentPoint.getY() - repulsionOrigin.getY()
				);
				if (distance > 0){
					repulsions.add(repulsion.normalised().scale(1.0 / distance));
				}
				else {
					repulsions.add(repulsion.normalised().scale(1.0E12));
				}
				
			}
		}
		
		if(repulsions.size() < 1) {
			Logger.info("Not enough repulsion vectors");
			return null;
		}
		
		Vector2D resultant = repulsions.get(0);
		
		for(int i = 1; i < repulsions.size(); i++){
			resultant.add(repulsions.get(i));
		}
		
		resultant.scale(1000000);
		
		Line2D intendedTrajectory = new Line2D(agentPoint, agentPoint.plus(resultant));
		
		List<Edge> areaEdges = ((Area)stuckAgent.location).getEdges();
		for(Edge e : areaEdges){
			Line2D edgeLine = e.getLine();
			Point2D intersection = GeometryTools2D.getSegmentIntersectionPoint(intendedTrajectory, edgeLine);
			if(intersection != null){
				Logger.debug("Navigation target intersects with edge "+ e);
				Logger.debug("Returning intersection point " + intersection);
				return intersection;
			}
		}
		
		Logger.debug("Navigation target does not intersect with Area edges, returning it: " + agentPoint.plus(resultant));
		return agentPoint.plus(resultant);
		
		/*Precise: raytrace from agent to blockade edge, calculating repulsion
		 List<Line2D> sightLines = new ArrayList<>();
		for(int angle = 0; angle < 360; angle += 10){
			Vector2D vec = new Vector2D(
				Math.cos(Math.toRadians(angle)), 
				Math.sin(Math.toRadians(angle))
			).scale(1000000);
			
			sightLines.add(new Line2D(agentPoint, vec));
		}
		GeometryTools2D.getSegmentIntersectionPoint(l1, l2);
		*/
		
	}
	
	
	public static Point2D calculateStuckMove(Area from, Area to, Tactics<?> agent,int currentTime){
		System.out.println(String.format("Area1=%s, Area2=%s", from, to));
		List<Edge> edges = from.getEdges();
		Edge frontier = from.getEdgeTo(to.getID());
		List<Point2D> midpoints = new ArrayList<>();
		if(frontier == null){
			for(Edge next : edges){
				Point2D midPoint = new Point2D(next.getStartX() + (next.getEndX() - next.getStartX())/2, 
						next.getStartY() + (next.getEndY() - next.getStartY())/2);
				midpoints.add(midPoint);
			}
			return midpoints.get(currentTime%midpoints.size());
		}
		else{
			
			Point2D origin = new Point2D(agent.me().getX(),agent.me().getY());
			Point2D midPoint = new Point2D(frontier.getStartX() + (frontier.getEndX() - frontier.getStartX())/2, 
					frontier.getStartY() + (frontier.getEndY() - frontier.getStartY())/2);
			Vector2D agentToTarget = new Vector2D(midPoint.getX() - agent.me().getX(), midPoint.getY() - agent.me().getY());
			Vector2D normal0 = agentToTarget.getNormal();
			Vector2D normal1 = normal0.scale(-1);
			normal0 = normal0.scale(10000);
			normal1 = normal1.scale(10000);
			
			
			Point2D destination = null;
			double furthestDistance = -1;
			
			for(Edge next : edges){
				if(next.getEnd().equals(frontier.getEnd())||next.getEnd().equals(frontier.getStart())||next.getStart().equals(frontier.getEnd())||next.getStart().equals(frontier.getStart())){
					Line2D parallelEdge = next.getLine();
					Line2D agentNormal0 = new Line2D(origin,normal0);
					Point2D intersection = GeometryTools2D.getIntersectionPoint(parallelEdge, agentNormal0);
					double distance = GeometryTools2D.getDistance(origin, intersection);
					if(distance > furthestDistance ) {
						furthestDistance = distance;
						destination = intersection;
					}
				}
			}
			return destination;
		}
		
	}
	
	/**
	 * Returns true if a blockade in the list is in clearRange around target area
	 * @param blockList
	 * @param target
	 * @return
	 */
	public boolean anyBlockadeInClearArea(ArrayList<Blockade> blockList, Point2D target) {
		boolean result = false;
		java.awt.geom.Area clearArea = getClearArea((int)target.getX(), (int)target.getY(), owner.clearRange, owner.clearWidth);
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
		Vector2D agentToTarget = new Vector2D(targetX - owner.me().getX(), targetY - owner.me().getY());

		if (agentToTarget.getLength() > clearLength)
			agentToTarget = agentToTarget.normalised().scale(clearLength);

		Vector2D backAgent = (new Vector2D(owner.me().getX(), owner.me().getY()))
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
	
	public static Point2D getClosestPointToABlockade(Blockade b,int x,int y){
		double distance = Double.MAX_VALUE;
		Point2D point0 = new Point2D(x,y),pointReturn = null;
		Polygon p = yrescue.util.GeometricUtil.getPolygon(b.getApexes());
		for(int i = 0; i < p.npoints; i++){
			Point2D point1 = new Point2D(p.xpoints[i],p.ypoints[i]);
			Point2D point2 = null;
			if(i == (p.npoints - 1)){
				point2 = new Point2D(p.xpoints[0],p.ypoints[0]);
			}
			else{
				point2 = new Point2D(p.xpoints[i + 1],p.ypoints[i+1]);
			}
			Line2D line = new Line2D(point1,point2);
			Point2D closest = GeometryTools2D.getClosestPointOnSegment(line, point0);
			double d = GeometryTools2D.getDistance(point0, closest);
			if(d < distance){
				pointReturn = closest;
				distance = d;
			}
		}
		return pointReturn;
		
	}
	
	public boolean checkBlockadesAround(WorldProvider<?> provider, Area location, int xPos, int yPos){
		if(!location.isBlockadesDefined()){
			return false;
		}
		List<EntityID> listOfBlockades = location.getBlockades();
		if(listOfBlockades != null){
	    	for(EntityID e : listOfBlockades ){
	    		Blockade b = (Blockade)provider.getWorld().getEntity(e);
	    		//TODO: usar o metodo do dudu
	    		java.awt.Polygon pol = new java.awt.Polygon();
	    		int [] apexes = b.getApexes();
	    		for(int i = 0; i < apexes.length; i++){
	    			int x = apexes[i];
	    			int y = apexes[i+1];
	    			i++;
	    			pol.addPoint(x, y);
	    		}
	    		if(pol.contains(xPos, yPos)) return true;
	    	}
		}
    	return false;
        
    }
	
	
	/**
	 * Returns all blockades contained in the square 
	 * with diagonal from (x - range, y - range) to (x + range, y + range) 
	 * @param x
	 * @param y
	 * @param range
	 * @return
	 */
    public PriorityQueue<Blockade> getBlockadesInSquare(int x, int y, int range) {
    	StandardWorldModel model = owner.getWorld();
		final PriorityQueue<Blockade> result = new PriorityQueue<Blockade>(20, new YRescueDistanceSorter(owner.me(), model));
		//Rectangle r = new Rectangle(x - range, y - range, x + range, y + range);

		Collection<StandardEntity> entities = model.getObjectsInRectangle(x - range, y - range, x + range, y + range);
		for(StandardEntity e : entities){
			if (e instanceof Road){
				Road road = (Road) e;
				if (road.isBlockadesDefined()){					
					for (EntityID blockID : road.getBlockades()){
						if(model.getDistance(owner.me().getID(), blockID) < range){
							result.add((Blockade)model.getEntity(blockID));
						}						
					}
				}
			}			
		}
		return result;
    }
}
