package yrescue.blockade;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;

import adk.team.tactics.Tactics;
import adk.team.util.provider.WorldProvider;
import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import yrescue.tactics.YRescueTacticsPolice;

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
		Road myRoad = (Road) t.location;
		
		Blockade closest = null;
		int closestDistance = Integer.MAX_VALUE;
		
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
}
