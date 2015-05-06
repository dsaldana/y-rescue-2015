package agent.platoon;

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
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Line2D;
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
		return "RoboCop " + me().getID();
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

		// List of refuges
		// Collection<StandardEntity> crefuge = model
		// .getEntitiesOfType(StandardEntityURN.REFUGE);

		// Get the closest refugee
		// EntityID closestRefuge = null;
		// double minDistance = Double.MAX_VALUE;
		// for (EntityID idRefuge : refugeIDs) {
		//
		// Pair<Integer, Integer> location =
		// model.getEntity(idRefuge).getLocation(model);
		//
		// Point2D prefuge = new Point2D(location.first(), location.second());
		// double d = GeometryTools2D.getDistance(origin, prefuge);
		//
		// if (d < minDistance) {
		// closestRefuge = idRefuge;
		// }
		// }
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

			// Road r = (Road) model.getEntity(path.get(path.size() - 1));
			// Blockade b = getTargetBlockade(r, -1);
			Refuge b = (Refuge) model.getEntity(path.get(path.size() - 1));

			// if location is refuge, then target accomplished
			if (location() instanceof Refuge) {
				System.out.println("MISION ACCOMPLISHED");
				return;
			}

			// if location is a road, then verify if it is blocked. if not
			// clean, then clean.
			// if (blockedRoads.containsKey(location())) {
			// System.out.println("Blocked road");
			// // return;
			// }
			double dist = Double.MAX_VALUE;
			if (lastPosition != null) {
				 dist = GeometryTools2D.getDistance(lastPosition, current_position);
				System.out.println("distancia percorrida" + dist);
			}
			
			// if the position is the same, then clean
			if (lastPosition != null && dist < 100.0) {
				// Am I near a blockade?
				Blockade target = getTargetBlockade();
				// ----BEGIN Tests if blockade is in range and sends clear
				// command
				Logger.info("Clearing blockade " + target);
				// Communicate the clearing
				sendSpeak(time, 1, ("Clearing " + target).getBytes());

				List<Line2D> lines = GeometryTools2D
						.pointsToLines(GeometryTools2D
								.vertexArrayToPoints(target.getApexes()), true);
				//
				double best = Double.MAX_VALUE;
				Point2D bestPoint = null;
				// Point2D origin = new Point2D(me().getX(), me().getY());

				for (Line2D next : lines) {
					Point2D closest = GeometryTools2D.getClosestPointOnSegment(
							next, current_position);
					double d = GeometryTools2D.getDistance(current_position, closest);
					if (d < best) {
						best = d;
						bestPoint = closest;
					}
				}
				Vector2D v = bestPoint.minus(new Point2D(me().getX(), me()
						.getY()));
				v = v.normalised().scale(1000000);

				sendClear(time, (int) (me().getX() + v.getX()), (int) (me()
						.getY() + v.getY()));

				lastPosition = null;
				return;

				// ---- END Tests if blockade is in range and sends clear
				// command
			}

			lastPosition = current_position;

			// Moving
			Logger.info("Moving to target");
			sendMove(time, path, b.getX(), b.getY());
			Logger.debug("Path: " + path);
			// Logger.debug("Target coordinates: " + b.getX() + ", " +
			// b.getY());
			return;
		}

		//
		// // ---- BEGIN Plan a path and moves to a blockade
		// List<EntityID> path = search.breadthFirstSearch(me().getPosition(),
		// getBlockedRoads());
		// if (path != null) {
		// Logger.info("Moving to target");
		// Road r = (Road) model.getEntity(path.get(path.size() - 1));
		// Blockade b = getTargetBlockade(r, -1);
		// sendMove(time, path, b.getX(), b.getY());
		// Logger.debug("Path: " + path);
		// Logger.debug("Target coordinates: " + b.getX() + ", " + b.getY());
		// return;
		// }
		//
		// Logger.debug("Couldn't plan a path to a blocked road");
		// Logger.info("Moving randomly");
		// sendMove(time, randomWalk());
		// // ---- END Plan a path and moves to a blockade

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
