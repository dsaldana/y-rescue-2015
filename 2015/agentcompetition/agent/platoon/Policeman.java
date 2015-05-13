package agent.platoon;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Area;

/**
 * RoboCop agent. Implements the reinforcement learning scheme
 */
public class Policeman extends AbstractPlatoon<PoliceForce> {
	private static final String DISTANCE_KEY = "clear.repair.distance";

	private int clearRange;

	// testing dav
	private boolean moving;
	private Point2D lastPosition;

	@Override
	public String toString() {
		return "Policeman " + me().getID();
	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.ROAD);
		clearRange = config.getIntValue(DISTANCE_KEY);
	}

	@Override
	protected void doThink(int time, ChangeSet changed,
			Collection<Command> heard) {
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Subscribe to channel 1
			sendSubscribe(time, 1);
		}
		for (Command next : heard) {
			Logger.debug("Heard " + next);
		}
		/*
		 * System.out.println("\nChanged entities:"); for(EntityID id :
		 * changed.getChangedEntities()){ Entity e = model.getEntity(id);
		 * System.out.println(e.getID()+" "+e.getURN()); }
		 */

		System.out.println("\nTime: " + time);
		System.out.println("#burning bldgs:" + burningBuildings.size());
		System.out.println("#wounded humans:" + woundedHumans.size());
		System.out.println("#blocked roads:" + blockedRoads.size());
		System.out.println("the blk roads:" + blockedRoads.keySet());

		// ---- BEGIN Plan a path and moves to a blockade
		// local position
		Point2D current_position = new Point2D(me().getX(), me().getY());

		
		List<EntityID> path = search.breadthFirstSearch(me().getPosition(),
				refugeIDs);

		// Plan to refugee
		// ---- BEGIN Plan a path and moves to a blockade
		// List<EntityID> path = search.breadthFirstSearch(me().getID(),
		// closestRefuge);
		if (time < 3) {
			return;
		}
		if (path != null) {

			Refuge b = (Refuge) model.getEntity(path.get(path.size() - 1));

			// if location is refuge, then target accomplished
			if (location() instanceof Refuge) {
				System.out.println("MISION ACCOMPLISHED");
				return;
			}

			double dist = Double.MAX_VALUE;
			if (lastPosition != null) {
				dist = GeometryTools2D.getDistance(lastPosition,
						current_position);
				System.out.println("distancia percorrida" + dist);
			}

			// if the position is the same, then clean
			if (lastPosition != null && dist < 100.0) {
				Road r1 = (Road) model.getEntity(path.get(0));
				Road r2 = (Road) model.getEntity(path.get(1));

				clearTowardsIntersection(r1, r2, time);
				lastPosition = null;
				return;

				// ---- END Tests if blockade is in range and sends clear
			}

			lastPosition = current_position;

			// Moving
			Logger.info("Moving to target");
			sendMove(time, path, b.getX(), b.getY());
			Logger.debug("Path: " + path + ", coords: " + b.getX() + ", "
					+ b.getY());
			return;
		}
	}

	private void clearTowardsIntersection(Road r1, Road r2, int time) {
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

		Logger.info("Clearing blockade: " + targetx + ", " + targety);
		// Communicate the clearing
		// TODO speak
		// sendSpeak(time, 1, ("Clearing " + target).getBytes());
		sendClear(time, (int) targetx, (int) targety);

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
