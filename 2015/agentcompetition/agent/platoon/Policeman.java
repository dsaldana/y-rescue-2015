package agent.platoon;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import commands.AgentCommands;
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
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Area;
import statemachine.ActionStates;

/**
 * RoboCop agent. Implements the reinforcement learning scheme
 */
public class Policeman extends AbstractPlatoon<PoliceForce> {
	private static final String DISTANCE_KEY = "clear.repair.distance";
	private static final String WIDTH_KEY = "clear.repair.rad";
	private static final String RATE_KEY = "clear.repair.rate";

	private int clearRange,	//range that first clear method can reach 
		clearWidth,				//width of the 'shot' to clear road 
		clearRate;				//clear rate in square meters per timestep 

	// testing dav
	private boolean moving;
	private Point2D lastPosition;

	@Override
	public String toString() {
		return String.format("Policeman(%s)", me().getID());
	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.ROAD);
		clearRange = readConfigIntValue(DISTANCE_KEY, 10000);
		clearWidth = readConfigIntValue(WIDTH_KEY, 1250);// getConfig().getIntValue("clear.repair.rad", 1250);
		clearRate = readConfigIntValue(RATE_KEY, 10);
	}

	@Override
	protected void doThink(int time, ChangeSet changed,
			Collection<Command> heard) throws Exception {
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Subscribe to channel 1
			sendSubscribe(time, 1);
		}
		/*for (Command next : heard) {
			Logger.debug("Heard " + next);
		}*/
		
		
		if(stuck()){
			Blockade target = getTargetBlockade();
	        if (target != null) {
	            Logger.info("STUCK! Clearing blockade " + target);
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
		
		//int targetEntity = 254;

		// ---- BEGIN Plan a path and moves to a blockade
		// /////// Plan to go to some area or building
		EntityID target = getRoadToClear(); // new EntityID(targetEntity);
		List<EntityID> path = computePath(target);

		/*if (time < 3) {
			return;
		}*/

		// FIXME if location is refuge, then target accomplished
		if (location().getID().getValue() == target.getValue()) {
			Logger.info("MISSION ACCOMPLISHED");

			// Collection<StandardEntity> roads =
			// model.getEntitiesOfType(StandardEntityURN.ROAD);

			return;
		}

		if (path != null && path.size() > 0) {
			clearPath(path);
		}
	}

	private EntityID getRoadToClear() {
		
		return new EntityID(255);
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
            sendSpeak(time, 1, ("Clearing " + target).getBytes());
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
