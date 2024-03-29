package agent.platoon;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import problem.BlockedArea;
import commands.AgentCommand;
import commands.AgentCommands;
import commands.ClearBlockadeCommand;
import commands.ClearDirectionCommand;
import commands.MoveToAreaCommand;
import commands.MoveToCoordsCommand;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.properties.IntArrayProperty;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Area;
import search.SearchResult;
import statemachine.ActionStates;
import util.Destination;
import util.DistanceSorter;
import util.Geometry;

/**
 * 
 * TODO: implementar blockedPathProblem: o policial tem que limpar o caminho 
 * inteiro de uma origem ate um destino
 *
 */
public class Policeman extends AbstractPlatoon<PoliceForce> {
	private static final String DISTANCE_KEY = "clear.repair.distance";
	private static final String WIDTH_KEY = "clear.repair.rad";
	private static final String RATE_KEY = "clear.repair.rate";

	private int clearRange,	//range that first clear method can reach 
		clearWidth,				//width of the 'shot' to clear road 
		clearRate;				//clear rate in square meters per timestep
	
	boolean randomDestination; //indicates whether policeman is answering call for help or not

	// testing dav
	private boolean moving;
	private Point2D lastPosition;
	
	Destination destination;
	List<EntityID> currentPath;

	@Override
	public String toString() {
		return String.format("Policeman(%s)", me().getID());
	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT);
		clearRange = readConfigIntValue(DISTANCE_KEY, 10000);
		clearWidth = readConfigIntValue(WIDTH_KEY, 1250);// getConfig().getIntValue("clear.repair.rad", 1250);
		clearRate = readConfigIntValue(RATE_KEY, 10);
		
		destination = null;
		currentPath = null;
		randomDestination = true;
	}

	@Override
	protected void doThink(int time, ChangeSet changed,
			Collection<Command> heard) throws Exception {
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Subscribe to channel 1
			sendSubscribe(time, 1);
		}
		
		if(stuckInLoop()){
			Logger.warn("STUCK IN CLEAR/MOVE LOOP! Goin' failsafe...");
			failsafe();
			return;
		}
		
		if(stuck()){
			Logger.info("STUCK! Attempting a clear...");
			doClear(time);
			return;
		}
		
		if (destination == null){
			destination = chooseDestination();
			Logger.info("I had no destination, now it is" + destination);
		}
		
		if (destination.match(location().getID(), me().getX(), me().getY(), 250)){
			
			BlockedArea solved = blockedAreas.get(destination.getAreaID());
			
			Logger.debug("Solved problem " + solved);
			
			if (solved != null) {
				solved.markSolved(time);
				problemsToReport.add(solved);
				destination = chooseDestination();
			}
			
			Logger.info("I have arrived to my d estination. New destination is" + destination);
			Logger.info("Added solved" + solved + " to problemsToReport.");
		}
		
		if (randomDestination && blockedAreas.size() > 0){
			destination = chooseDestination();
			Logger.info("I had a random destination, now I selected one from list of problems: " + destination);
		}
		
		Logger.info("Planning path from " + location().getID() + " to " + destination);
		currentPath = searchStrategy.shortestPath(location().getID(), destination.getAreaID()).getPath();
		
		if (currentPath == null){
			/*
			 * TODO: this happens when agent in area of destination, but not in coordinate
			 * Need to test this and make it move to point; clearing blockades in the way.
			 */
			if (location().getID().equals(destination.getAreaID())){
				Logger.info("I'm in destination area, but not in intended coordinates yet.");
				
				Point2D target = destination.getCoordinatesAsPoint2D();
				
				if (blockadeOnWayTo(target)){
				
					Logger.info(String.format(
						"Blockade found on way to target. Attempting to clear from %d,%d to %s.", 
						me().getX(), me().getY(), destination.getCoordinatesAsPoint2D()
					));
					
					//Point2D origin = new Point2D(me().getX(), me().getY());
					
					//Edge frontier = Geometry.findSmallestEdgeConnecting((Area)location(), (Area)model.getEntity(currentPath.get(0)));
					//Point2D target = Geometry.midpoint(frontier);
					
				    Vector2D v = target.minus(new Point2D(me().getX(), me().getY()));
				    v = v.normalised().scale(1000000);
				    sendClear(time, (int)(me().getX() + v.getX()), (int)(me().getY() + v.getY()));
					return;
				}
				else {
					Logger.info(String.format(
						"Blockade not found on way to target. Attempting to move from %d,%d to %s.", 
						me().getX(), me().getY(), destination.getCoordinatesAsPoint2D()
					));
					//creates a dummy path with only current area in it
					List<EntityID> path = new LinkedList<>();
					path.add(location().getID());
					sendMove(time, path, (int)target.getX(), (int)target.getY());
					return;
				}
			}
			
			Logger.warn(String.format(
					"Could not plan a path to destination %s. I'm at %s, not at destination area. WEIRD! Going failsafe.",
					destination.getAreaID(), location().getID()
			));
			failsafe();
			return;
		}
		
		if (blockadeOnWayTo(currentPath.get(0))){
			Logger.info("Attempting clear from " + location());
			
			//Point2D origin = new Point2D(me().getX(), me().getY());
			
			Edge frontier = Geometry.findSmallestEdgeConnecting((Area)location(), (Area)model.getEntity(currentPath.get(0)));
			Point2D target = Geometry.midpoint(frontier);
			
		    Vector2D v = target.minus(new Point2D(me().getX(), me().getY()));
		    v = v.normalised().scale(1000000);
		    sendClear(time, (int)(me().getX() + v.getX()), (int)(me().getY() + v.getY()));
		    return;
			
			//sendClear(time, x, y);
			
			//doClear(currentPath.get(0), time); //TODO: improve this version of doClear and USE IT. 
			//doClear(time);
		}
		else { //move!
			Logger.info(String.format("No blockade on way to %s. Will follow path %s", currentPath.get(0), currentPath));
			sendMove(time, currentPath);
			return;
		}
		
		//int targetEntity = 254;

		// ---- BEGIN Plan a path and moves to a blockade
		// /////// Plan to go to some area or building
		/*EntityID target = getRoadToClear(); // new EntityID(targetEntity);
		List<EntityID> path = computePath(target);

		/*if (time < 3) {
			return;
		}*

		// FIXME if location is refuge, then target accomplished
		if (location().getID().getValue() == target.getValue()) {
			Logger.info("MISSION ACCOMPLISHED");

			// Collection<StandardEntity> roads =
			// model.getEntitiesOfType(StandardEntityURN.ROAD);

			return;
		}

		if (path != null && path.size() > 0) {
			clearPath(path);
		}*/
	}

	/**
	 * Tests whether 4 among the 8 last commands were clear commands
	 * Attempts to prevent when agent always clears in same place
	 * @return
	 */
	private boolean stuckInLoop() {
		if(commandHistory.size() < 8){
			return false;
		}
		
		Map<AgentCommand, Integer> cmdCount = new HashMap<>(); 
				
		for(int lookedAt = 0; lookedAt < 8; lookedAt++){
			int index = time - lookedAt; //reads most recent command, then second most recent and so on
			AgentCommand cmd = commandHistory.get(index);
			
			Logger.debug("Analysing command " + cmd);
			
			//ignores non- clear or move commands
			if (! (cmd instanceof ClearBlockadeCommand || 
				cmd instanceof ClearDirectionCommand ||
				cmd instanceof MoveToAreaCommand ||
				cmd instanceof MoveToCoordsCommand) )
				 {
				Logger.debug("Ignoring command " + cmd);
				continue;
			}
			
			if (cmdCount.containsKey(cmd)){
				Logger.debug("Counting command " + cmd);
				//increments value if found
				cmdCount.put(cmd, cmdCount.get(cmd) + 1);
				if (cmdCount.get(cmd) >= 4) {
					Logger.info("Found 4 or more instances of command " + cmd);
					return true;
				}
			}
			else {
				//initializes value if not found
				cmdCount.put(cmd, 1);
			}
		}
		Logger.debug("Command count was " + cmdCount);
		return false;
		/*
		for (int count : cmdCount.values()){
			if(count >= 4) return true;
		}*/
		
		/*
		//Logger.info("CommandHistory " + commandHistory);
		int sameClearCount = 0;
		boolean lastWasClear = false;
		for(int lookedAt = 0; lookedAt < 6; lookedAt++){
			int index = time - lookedAt; //reads most recent command, then second most recent and so on
			if (commandHistory.get(index).equals(AgentCommands.Policeman.CLEAR)) {
				if(lastWasClear) {
					sameClearCount++;
				}
				else {
					lastWasClear = true;
				}
			}
			else {
				lastWasClear = false;
				sameClearCount = 0;
			}
		}
		
		Logger.info("From last 6 commands, " + sameClearCount + " were repeated clear.");
		//Logger.info("Stuck in clear loop?");
		return sameClearCount >= 4;
		*/
	}

	/**
	 * Attempts to clear in direction of Area with the given id
	 * TODO: not working yet
	 * @param entityID
	 * @param time
	 */
	private void doClear(EntityID areaID, int time) {
		Blockade target =  findBlockadeOnWayTo(areaID);
		Logger.info("Clearing blockade " + target + " on way to " + areaID);
		if (target != null) {
		    
		    //sendSpeak(time, 1, ("Clearing " + target).getBytes());
//			            sendClear(time, target.getX(), target.getY());
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
		    sendClear(time, (int)(me().getX() + v.getX()), (int)(me().getY() + v.getY()));
		    return;
		}
		
	}

	/**
	 * @param time
	 */
	private void doClear(int time) {
		
		Blockade target =  getTargetBlockade();
		Logger.info("Clearing blockade " + target);
		if (target != null) {
		    
		    //sendSpeak(time, 1, ("Clearing " + target).getBytes());
//			            sendClear(time, target.getX(), target.getY());
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
		    sendClear(time, (int)(me().getX() + v.getX()), (int)(me().getY() + v.getY()));
		    return;
		}
	}

	/**
	 * Returns whether there is a blockade in range of clearance on way 
	 * to neighbor entity ID
	 * @param dest
	 * @return
	 */
	private boolean blockadeOnWayTo(EntityID dest) {
		
		Logger.debug("Will check blockades on way to " + dest);
		
		if (! neighbours.get(location().getID()).contains(dest)){
			Logger.warn(String.format("Destination %s is not neighbor of current location %s. Will calculate a path", dest, location().getID()));
			List<EntityID> path = searchStrategy.shortestPath(location().getID(), dest).getPath();
			dest = path.get(0);
		}
		Edge frontier = Geometry.findSmallestEdgeConnecting((Area)location(), (Area)model.getEntity(dest));
		
		Point2D target = Geometry.midpoint(frontier);
		
		Logger.debug("Will check blockades on way to " + dest);
		
		
		ArrayList<Blockade> blockList = new ArrayList<Blockade>(getBlockadesInRange(me().getX(), me().getY(), clearRange));
		Logger.debug("Blockade list " + blockList + " in range " + clearRange);
		if (anyBlockadeInClearArea(blockList, target))
			return true;
		return false;
	}
	
	/**
	 * Returns whether there is a blockade in range of clearance on way 
	 * to a point. The point must be in the same Area of the agent.
	 * @param coords
	 * @return
	 */
	private boolean blockadeOnWayTo(Point2D coords) {
		
		Logger.debug("Will check blockades on way to " + coords);
		
		/*if (! neighbours.get(location().getID()).contains(dest)){
			Logger.warn(String.format("Destination %s is not neighbor of current location %s. Will calculate a path", dest, location().getID()));
			List<EntityID> path = searchStrategy.shortestPath(location().getID(), dest).getPath();
			dest = path.get(0);
		}
		Edge frontier = Geometry.findSmallestEdgeConnecting((Area)location(), (Area)model.getEntity(dest));
		
		Point2D target = Geometry.midpoint(frontier);
		
		Logger.debug("Will check blockades on way to " + dest);
		*/
		
		ArrayList<Blockade> blockList = new ArrayList<Blockade>(getBlockadesInRange(me().getX(), me().getY(), clearRange));
		Logger.debug("Blockade list " + blockList + " in range " + clearRange);
		if (anyBlockadeInClearArea(blockList, coords))
			return true;
		return false;
	}
	
	private Blockade findBlockadeOnWayTo(EntityID dest) {
		
		Logger.debug("Will check blockades on way to " + dest);
		
		if (! neighbours.get(location().getID()).contains(dest)){
			Logger.warn(String.format("Destination %s is not neighbor of current location %s. Will calculate a path", dest, location().getID()));
			List<EntityID> path = searchStrategy.shortestPath(location().getID(), dest).getPath();
			dest = path.get(0);
		}
		Edge frontier = Geometry.findSmallestEdgeConnecting((Area)location(), (Area)model.getEntity(dest));
		
		Point2D target = Geometry.midpoint(frontier);
		
		Logger.debug("Will check blockades on way to " + dest);
		
		
		//TODO build the 'shot' rectangle and see if it intersects with a blockade in this area
		ArrayList<Blockade> blockList = new ArrayList<Blockade>(getBlockadesInRange(me().getX(), me().getY(), clearRange));
		Logger.debug("Blockade list " + blockList + " in range " + clearRange);

		return findBlockadeInClearArea(blockList, target);
	}
	
	/**
	 * Returns the ID of the first blockade in clear area
	 * @param blockList
	 * @param target
	 * @return
	 */
	private Blockade findBlockadeInClearArea(ArrayList<Blockade> blockList, Point2D target) {
		
		java.awt.geom.Area clearArea = getClearArea((int)target.getX(), (int)target.getY(), clearRange, clearWidth);
		for (Blockade block : blockList) {
			java.awt.geom.Area select = new java.awt.geom.Area(block.getShape());
			select.intersect(clearArea);
			if (!select.isEmpty()) {
				Logger.debug(block + "  has intersect with clear area ");
				return block;
			}
		}
		return null;
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

	public PriorityQueue<Blockade> getBlockadesInRange(int x, int y, int range) {
		final PriorityQueue<Blockade> result = new PriorityQueue<Blockade>(20, new DistanceSorter(me(), model));
		Rectangle r = new Rectangle(x - range, y - range, x + range, y + range);
		
		//getObjectsInRectangle does not returns the blockades!
		Collection<StandardEntity> entities = model.getObjectsInRectangle(x - range, y - range, x + range, y + range);
		Logger.info(String.format("Testing intersection of blockades with rect(%d, %d, %d, %d)", x - range, y - range, x + range, y + range));
		Logger.info(String.format("Found entities: " + entities));
		for(StandardEntity e : entities){
			if (e instanceof Road){
				Road road = (Road) e;
				if (road.isBlockadesDefined()){ // && road.getBlockades().size() > 0){
					Logger.debug("On road " + road);
					
					for (EntityID blockID : road.getBlockades()){
						Logger.debug(String.format("Dist. do block %s: %d ", blockID, model.getDistance(me().getID(), blockID)));	
						if(model.getDistance(me().getID(), blockID) < range){
							Logger.debug("Blockade " + blockID + " intersects");
							result.add((Blockade)model.getEntity(blockID));
						}
						
					}
				}
			}
			
		}
		
		/*index.intersects(r, new IntProcedure() {
			@Override
			public boolean execute(int id) {
				StandardEntity e = getEntity(new EntityID(id));
				if (e != null && e instanceof Road && ((Road) e).isBlockadesDefined()) {
					for (Blockade blockade : ((Road) e).getBlockades()) {
						if (PoliceUtils.isValid(blockade))
							result.add(blockade);
					}
				}
				return true;
			}
		})*/;
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

	private Destination chooseDestination() {
		
		//goes to closest destination in blockedAreas
		BlockedArea closest = null;
		
		for(BlockedArea b : blockedAreas.values()){
			if(b.isSolved()) continue;
			
			if (closest == null || model.getDistance(me().getID(), b.areaID) < model.getDistance(me().getID(), closest.areaID)){
				closest = b; 
			}
		}
		
		if (closest != null) {
			Logger.info("Chosen destination: " + closest);
			randomDestination = false;
			return new Destination(closest.areaID, closest.x, closest.y);
		}
		
		Logger.info(
			String.format(
				"No destination chosen from blockedAreas map of size %d. Will choose randomly.", 
				blockedAreas.size()
			) 
		);
		
		
		Collection<StandardEntity> entities = model.getEntitiesOfType(StandardEntityURN.BUILDING, 
				StandardEntityURN.ROAD, StandardEntityURN.HYDRANT, 
				StandardEntityURN.REFUGE);
		
		Set<EntityID> ids = objectsToIDs(entities);
		EntityID destID = ids.toArray(new EntityID[1])[new Random().nextInt(entities.size())];
		StandardEntity d = model.getEntity(destID);
		Logger.info("Chosen destination: " + d);
		randomDestination = true;
		return new Destination(destID, d.getLocation(model));	//coordinates will be centroid of area
	}

	private List<EntityID> computePath(EntityID entityId) {
		ArrayList<EntityID> destinies = new ArrayList<EntityID>();
		destinies.add(entityId);
		return computePath(destinies);
	}

	/**
	 * Compute the shortest path to some IDs.
	 * 
	 * @param refugeIDs
	 * @return
	 */
	private List<EntityID> computePath(List<EntityID> refugeIDs) {
		return searchStrategy.shortestPath(me().getPosition(), refugeIDs).getPath();
	}

	private void clearPath(List<EntityID> path) {

		// ---- BEGIN Plan a path and moves to a blockade
		// List<EntityID> path = search.breadthFirstSearch(me().getID(),
		// closestRefuge);

		// local position
		Point2D current_position = new Point2D(me().getX(), me().getY());

		Area destination = (Area) model.getEntity(path.get(path.size() - 1));

		double dist = Double.MAX_VALUE;
		if (lastPosition != null) {
			dist = GeometryTools2D.getDistance(lastPosition, current_position);
			System.out.println("distancia percorrida" + dist);
		}

		// if the position is the same, then clean
		if (lastPosition != null && dist < 100.0) {
			Area r1 = (Area) location();
			Area r2 = (Area) model.getEntity(path.get(0));
			Logger.info(String.format("Will clear from %s to %s", r1, r2));
			boolean cleared = clearTowardsIntersection2(r1, r2, time);
			lastPosition = null;
			
			if (cleared) {
				return;
			}
			else {
				Logger.info("Could not clear...");
			}

			// ---- END Tests if blockade is in range and sends clear
		}

		lastPosition = current_position;

		// Moving
		stateMachine.setState(ActionStates.GOING_TO_TARGET);
		Logger.info("Moving to target " + destination);
		sendMove(time, path, destination.getX(), destination.getY());
		
		Logger.info("Path: " + path + ", coords: " + destination.getX() + ", "
				+ destination.getY());
		return;

	}

	private boolean clearTowardsIntersection2(Area r1, Area r2, int time) {
		
		Logger.info("Will clear from " + r1  + " to " + r2);
		Point2D intersection = intersectionPoint(r1, r2);
		Logger.info("Intersection located at " + intersection);

		// / get all the blockades
		Area location = (Area) location();
		List<EntityID> blockades = location.getBlockades();
		
		if (blockades == null) {
			Logger.info("This Area " + location + " is clear! Won't clear.");
			return false;
		}
		
		int x = me().getX();
		int y = me().getY();
		// line from position to the intersection.
		Line2D linearPath = new Line2D(new Point2D(x, y), intersection);

		// boolean cleared = false;
		for (EntityID next : blockades) {
			Blockade b1 = (Blockade) model.getEntity(next);
			double d = findDistanceTo(b1, x, y);

			boolean intersects = isLineShapeIntersecting(linearPath,
					b1.getShape());

			if (intersects && Math.random() > 0.5) {
				// FXIME use the clear range
				// if (d < 0.1 * clearRange && intersects) {
				Logger.info("Clearing blockade: " + intersection.getX() + ", "
						+ intersection.getY());
				// Communicate the clearing
				// TODO speak
				// sendSpeak(time, 1, ("Clearing " + target).getBytes());
				sendClear(time, (int) intersection.getX(),
						(int) intersection.getY());
				// cleared = true;
				return true;
				// break;

			}
			else{
				Logger.info("Won't send clear: intersects=" + intersects);
			}
		}
		return false;

	}
	
	/**
	 * Gets the best blockade that blocks the entrance of a building
	 * @param list
	 * @return
	 */
	protected Blockade bestEntranceBlockade(List<Blockade> list) {
		Logger.debug("Choosing the best blockade from: " + list);
		if (list == null) {
			Logger.warn("List of blockades is null. Will return null Blockade");
			return null;
		}
		Blockade best = null;
		double bestValue = 0;
		for (Blockade blockade : list) {
			if (isValid(blockade)) {
				int value = computeBlockadeValue(blockade);
				if (value > bestValue) {
					bestValue = value;
					best = blockade;
				}
			}
		}
		Logger.info("Best blockade: " + best);
		return best;
	}
	
	
	
	public boolean isValid(Blockade b) {
		if (b == null) {
			System.err.println("null blockade passed to isValid");
			return false;
		}
		//PoliceForceAgent policeForceAgent = (PoliceForceAgent) b.getAgent();
		double blockadeDistance = getBlockadeDistance(b);
		// implemented by salim
		// if (Utils.distance(b.getLocation(), getLocation()) > clearDistance+)
		/*if (b.getLastSenseTime() < policeForceAgent.time() - 2) {
			log.debug(b + " is invalid! it is seen in " + b.getLastSenseTime());
			return false;
		}*/
		if (blockadeDistance >= clearRange) {
			Logger.debug(b + " invalid.  ClearRange:" + clearRange + ", dist. to block: " + blockadeDistance);
			return false;
		}
		// --------------------------
		return true;
	}
	
	/**
	 * Returns the closest point from Policeman to Blockade
	 * @param b
	 * @return
	 */
	public double getBlockadeDistance(Blockade b) {
		if (b == null) {
			Logger.warn("getBlockadeDistance received null blockade.");
			return Double.MAX_VALUE;
		}
		
		Point2D agentLocation =  new Point2D(me().getX(), me().getY());

		double bestDistance = Double.MAX_VALUE;
		List<Line2D> blockadelines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
		for (Line2D line : blockadelines) {
			Point2D closest = GeometryTools2D.getClosestPointOnSegment(line, agentLocation);
			double distance = GeometryTools2D.getDistance(agentLocation, closest);
			if (bestDistance > distance) {
				bestDistance = distance;
			}
		}

		//		return SOSGeometryTools.distance(b.getEdges(), policeForceAgent.me().getPositionPoint());
		return bestDistance;

	}
	
	private int computeBlockadeValue(Blockade blockade) {
		Point2D mypoint =  new Point2D(me().getX(), me().getY());
		Point2D centroid = getBlockadeCentroid(blockade);
		
		double distance = Math.hypot(mypoint.getX() - centroid.getX(), mypoint.getY() - centroid.getY());
		return (int) (1000000 / Math.max(distance, 1));
	}
	
	public Point2D getBlockadeCentroid(Blockade b) {
		return GeometryTools2D.computeCentroid(GeometryTools2D.vertexArrayToPoints(b.getApexes()));
	}


	private void clearTowardsIntersection(Area r1, Area r2, int time) {
		/**
		 * Compute the intersection between the two roads and clear in direction
		 * to the intersection.
		 * 
		 */
		// Bounding rectangle
		Rectangle2D rec1 = r1.getShape().getBounds2D();
		Rectangle2D rec2 = r2.getShape().getBounds2D();

		// center points of rectangle 1.
		Point2D rec1Center = new Point2D(rec1.getCenterX(), rec1.getCenterY());

		Point2D[] ptsR2 = { new Point2D(rec2.getMaxX(), rec2.getMaxY()),
				new Point2D(rec2.getMinX(), rec2.getMaxY()),
				new Point2D(rec2.getMaxX(), rec2.getMinY()),
				new Point2D(rec2.getMinX(), rec2.getMinY()) };

		double[] d1 = { GeometryTools2D.getDistance(rec1Center, ptsR2[0]),
				GeometryTools2D.getDistance(rec1Center, ptsR2[1]),
				GeometryTools2D.getDistance(rec1Center, ptsR2[2]),
				GeometryTools2D.getDistance(rec1Center, ptsR2[3]) };

		// Nearest two points (of the four)
		double minDistance1 = Double.MAX_VALUE;
		double minDistance2 = Double.MAX_VALUE;
		int minDistanceIdx1 = -1;
		int minDistanceIdx2 = -1;
		for (int i = 0; i < d1.length; i++) {
			if (d1[i] < minDistance1) {
				minDistanceIdx1 = i;
				minDistance1 = d1[i];
			} else if (d1[i] > minDistance1 && d1[i] < minDistance2) {
				minDistanceIdx2 = i;
				minDistance2 = d1[i];
			}
		}

		// Am I near a blockade?
		double targetx = (ptsR2[minDistanceIdx1].getX() + ptsR2[minDistanceIdx2]
				.getX()) / 2.0;
		double targety = (ptsR2[minDistanceIdx1].getY() + ptsR2[minDistanceIdx2]
				.getY()) / 2.0;

		stateMachine.setState(ActionStates.Policeman.CLEARING);
		Logger.info("Clearing blockade: " + targetx + ", " + targety);
		// Communicate the clearing
		// TODO speak
		// sendSpeak(time, 1, ("Clearing " + target).getBytes());
		sendClear(time, (int) targetx, (int) targety);
		return;

	}

	private boolean isLineShapeIntersecting(Line2D linearPath, Shape shape) {
		int LINE_DIVISIONS = 10;
		Vector2D lineDir = linearPath.getDirection();

		double length = lineDir.getLength();
		double dl = 1.0 / LINE_DIVISIONS;

		for (int i = 0; i < LINE_DIVISIONS; i++) {
			double t = i * dl;
			Point2D middlePoint = linearPath.getPoint(t);
			if (shape.contains(middlePoint.getX(), middlePoint.getY())) {
				return true;
			}
		}

		return false;
	}

	private Point2D intersectionPoint(Area r1, Area r2) {
		List<Edge> edges1 = r1.getEdges();
		List<Edge> edges2 = r2.getEdges();

		for (Edge edge1 : edges1) {
			if (edge1.getNeighbour() == null) {
				continue;
			}
			if (edge1.getNeighbour().equals(r2.getID())) {
				return edge1.getLine().getPoint(0.5);
			}

		}
		return null;
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.POLICE_FORCE);
	}

	private List<EntityID> getBlockedRoads() {
		Collection<StandardEntity> e = model
				.getEntitiesOfType(StandardEntityURN.ROAD);
		List<EntityID> result = new ArrayList<EntityID>();
		for (StandardEntity next : e) {
			Road r = (Road) next;
			if (r.isBlockadesDefined() && !r.getBlockades().isEmpty()) {
				result.add(r.getID());
			}
		}
		return result;
	}

	private Blockade getTargetBlockade() {
		/**
    	 * 
    	 */
		Logger.debug("Looking for target blockade");
		Area location = (Area) location();
		Logger.debug("Looking in current location");
		Blockade result = getTargetBlockade(location, clearRange);
		if (result != null) {
			return result;
		}
		Logger.debug("Looking in neighbouring locations");
		for (EntityID next : location.getNeighbours()) {
			location = (Area) model.getEntity(next);
			result = getTargetBlockade(location, clearRange);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private Blockade getTargetBlockade(Area area, int maxDistance) {
		// Logger.debug("Looking for nearest blockade in " + area);
		if (area == null || !area.isBlockadesDefined()) {
			// Logger.debug("Blockades undefined");
			return null;
		}

		List<EntityID> ids = area.getBlockades();
		// Find the first blockade that is in range.
		int x = me().getX();
		int y = me().getY();

		for (EntityID next : ids) {
			Blockade b = (Blockade) model.getEntity(next);

			double d = findDistanceTo(b, x, y);
			// Logger.debug("Distance to " + b + " = " + d);
			if (maxDistance < 0 || d < maxDistance) {
				// Logger.debug("In range");
				return b;
			}
		}
		// Logger.debug("No blockades in range");
		return null;
	}

	private int findDistanceTo(Blockade b, int x, int y) {
		// Logger.debug("Finding distance to " + b + " from " + x + ", " + y);
		List<Line2D> lines = GeometryTools2D.pointsToLines(
				GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
		double best = Double.MAX_VALUE;
		Point2D origin = new Point2D(x, y);
		for (Line2D next : lines) {
			Point2D closest = GeometryTools2D.getClosestPointOnSegment(next,
					origin);
			double d = GeometryTools2D.getDistance(origin, closest);
			// Logger.debug("Next line: " + next + ", closest point: " + closest
			// + ", distance: " + d);
			if (d < best) {
				best = d;
				// Logger.debug("New best distance");
			}

		}
		return (int) best;
	}

	@Override
	protected void failsafe() {
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to channel 1
            sendSubscribe(time, 1);
        }
        for (Command next : heard) {
            Logger.debug("Heard " + next);
        }
        // Am I near a blockade?
        Blockade target = failSafeGetTargetBlockade();
        if (target != null) {
            Logger.info("Clearing blockade " + target);
            //sendSpeak(time, 1, ("Clearing " + target).getBytes());
            sendClear(time, target.getID());
            return;
        }
        // Plan a path to a blocked area
        List<EntityID> path = failSafeSearch.breadthFirstSearch(me().getPosition(), getBlockedRoads());
        if (path != null) {
            Logger.info("Moving to target");
            Road r = (Road)model.getEntity(path.get(path.size() - 1));
            Blockade b = failSafeGetTargetBlockade(r, -1);
            sendMove(time, path, b.getX(), b.getY());
            Logger.debug("Path: " + path);
            Logger.debug("Target coordinates: " + b.getX() + ", " + b.getY());
            return;
        }
        Logger.debug("Couldn't plan a path to a blocked road");
        Logger.info("Moving randomly");
        sendMove(time, randomWalk());
        return;
		/*if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to channel 1
            sendSubscribe(time, 1);
        }
        for (Command next : heard) {
            Logger.debug("Heard " + next);
        }
        // Am I near a blockade?
        Blockade target = failSafeGetTargetBlockade();
        if (target != null) {
            Logger.info("Clearing blockade " + target);
            sendSpeak(time, 1, ("Clearing " + target).getBytes());
//            sendClear(time, target.getX(), target.getY());
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
            sendClear(time, (int)(me().getX() + v.getX()), (int)(me().getY() + v.getY()));
            return;
        }
        // Plan a path to a blocked area
        List<EntityID> path = failSafeSearch.breadthFirstSearch(me().getPosition(), failSafeGetBlockedRoads());
        if (path != null) {
            Logger.info("Moving to target");
            Road r = (Road)model.getEntity(path.get(path.size() - 1));
            Blockade b = failSafeGetTargetBlockade(r, -1);
            sendMove(time, path, b.getX(), b.getY());
            Logger.debug("Path: " + path);
            Logger.debug("Target coordinates: " + b.getX() + ", " + b.getY());
            return;
        }
        Logger.debug("Couldn't plan a path to a blocked road");
        Logger.info("Moving randomly");
        sendMove(time, randomWalk());*/
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
        Blockade result = failSafeGetTargetBlockade(location, clearRange);
        if (result != null) {
            return result;
        }
        Logger.debug("Looking in neighbouring locations");
        for (EntityID next : location.getNeighbours()) {
            location = (Area)model.getEntity(next);
            result = failSafeGetTargetBlockade(location, clearRange);
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


		
	/**
	 * Get the blockade that is nearest this agent.
	 * 
	 * @return The EntityID of the nearest blockade, or null if there are no
	 *         blockades in the agents current location.
	 */
	/*
	 * public EntityID getNearestBlockade() { return
	 * getNearestBlockade((Area)location(), me().getX(), me().getY()); }
	 */

	/**
	 * Get the blockade that is nearest a point.
	 * 
	 * @param area
	 *            The area to check.
	 * @param x
	 *            The X coordinate to look up.
	 * @param y
	 *            The X coordinate to look up.
	 * @return The EntityID of the nearest blockade, or null if there are no
	 *         blockades in this area.
	 */
	/*
	 * public EntityID getNearestBlockade(Area area, int x, int y) { double
	 * bestDistance = 0; EntityID best = null;
	 * Logger.debug("Finding nearest blockade"); if (area.isBlockadesDefined())
	 * { for (EntityID blockadeID : area.getBlockades()) {
	 * Logger.debug("Checking " + blockadeID); StandardEntity entity =
	 * model.getEntity(blockadeID); Logger.debug("Found " + entity); if (entity
	 * == null) { continue; } Pair<Integer, Integer> location =
	 * entity.getLocation(model); Logger.debug("Location: " + location); if
	 * (location == null) { continue; } double dx = location.first() - x; double
	 * dy = location.second() - y; double distance = Math.hypot(dx, dy); if
	 * (best == null || distance < bestDistance) { bestDistance = distance; best
	 * = entity.getID(); } } } Logger.debug("Nearest blockade: " + best); return
	 * best; }
	 */
}
