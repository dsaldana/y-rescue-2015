package yrescue.util;

import adk.team.util.ImpassableSelector;
import adk.team.util.graph.PositionUtil;
import adk.team.util.provider.WorldProvider;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class YRescueImpassableSelector implements ImpassableSelector {

    public Set<Road> impassableRoadList;
    public Set<EntityID> passableRoadList;

    public WorldProvider<? extends StandardEntity> provider;

    public YRescueImpassableSelector(WorldProvider<? extends StandardEntity> user) {
        this.provider = user;
        this.impassableRoadList = new HashSet<>();
        this.passableRoadList = new HashSet<>();
    }

    @Override
    public void add(Road road) {
        if(/*!this.passableRoadList.contains(road.getID()) &&*/ !road.getBlockades().isEmpty()) {
            this.impassableRoadList.add(road);
        }
    }

    @Override
    public void add(Blockade blockade) {
        Road road = (Road)this.provider.getWorld().getEntity(blockade.getPosition());
        //if(!this.passableRoadList.contains(road.getID())) {
        this.impassableRoadList.add(road);
        //}
    }

    @Override
    public void add(EntityID id) {
        StandardEntity entity = this.provider.getWorld().getEntity(id);
        if(entity instanceof Road) {
            this.add((Road)entity);
        }
        else if(entity instanceof Blockade) {
            this.add((Blockade)entity);
        }
    }

    @Override
    public void remove(Road road) {
        this.impassableRoadList.remove(road);
        this.passableRoadList.add(road.getID());
    }

    @Override
    public void remove(Blockade blockade) {
        Road road = (Road)this.provider.getWorld().getEntity(blockade.getPosition());
        if(road.getBlockades().isEmpty()) {
            this.remove(road);
        }
    }

    @Override
    public void remove(EntityID id) {
        StandardEntity entity = this.provider.getWorld().getEntity(id);
        if(entity instanceof Road) {
            this.remove((Road) entity);
        }
        else if(entity instanceof Blockade) {
            this.remove((Blockade) entity);
        }
    }

    @Override
    public EntityID getNewTarget(int time) {
    	    
    	Set<Road> impassableRoadList2 = new HashSet<>() ;
    	for (Road next: this.impassableRoadList){
    		if (next != this.provider.location()){
    			impassableRoadList2.add(next);
    		}
    	}
    	    	
        StandardEntity result = PositionUtil.getNearTarget(this.provider.getWorld(), this.provider.getOwner(), impassableRoadList2);
        return result != null ? result.getID() : null;
    }
    
    @Override
    public EntityID getNewTarget(int time, List<EntityID> cluster, List<EntityID> notCluster) {
    	return getNewTarget(time);
    }

    @Override
    public EntityID updateTarget(int time, EntityID target) {
        /*if(this.passableRoadList.contains(target)) {
            System.out.println("problem solved");
        	return this.getNewTarget(time);
        }*/
        for(StandardEntity police : this.provider.getWorld().getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
            if(target.getValue() == ((PoliceForce)police).getPosition().getValue()) {
            	if(this.provider.getID().getValue() == police.getID().getValue()) {
                    this.remove(target);
                    return this.getNewTarget(time);
                }
            }
        }
        return target;
    }
}
