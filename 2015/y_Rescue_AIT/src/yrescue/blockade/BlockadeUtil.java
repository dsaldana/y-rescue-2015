package yrescue.blockade;

import java.util.Collection;

import adk.team.tactics.Tactics;
import adk.team.util.provider.WorldProvider;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

/**
 * A collection of useful methods to deal with blockades
 *
 */
public class BlockadeUtil {
	private BlockadeUtil(){ }	//cannot instantiate
	
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
}
