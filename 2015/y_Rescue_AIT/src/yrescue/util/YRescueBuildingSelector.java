package yrescue.util;

import adk.team.util.BuildingSelector;
import adk.team.util.graph.PositionUtil;
import adk.team.util.provider.WorldProvider;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class YRescueBuildingSelector implements BuildingSelector {

    public WorldProvider<? extends StandardEntity> provider;

    public Set<Building> buildingList;

    public YRescueBuildingSelector(WorldProvider<? extends StandardEntity> user) {
        this.provider = user;
        this.buildingList = new HashSet<>();
    }

    @Override
    public void add(Building building) {
        if (building.isOnFire()) {
            this.buildingList.add(building);
        }
        else {
            this.buildingList.remove(building);
        }
    }

    @Override
    public void add(EntityID id) {
        StandardEntity entity = this.provider.getWorld().getEntity(id);
        if(entity instanceof Building) {
            this.add((Building) entity);
        }
    }

    @Override
    public void remove(Building building) {
    	//System.out.println("C'mon, remove it!");
        this.buildingList.remove(building);
        Logger.trace("Removing building from list:" + building);
        Logger.trace("List now is: " + buildingList);
    }

    @Override
    public void remove(EntityID id) {
        StandardEntity entity = this.provider.getWorld().getEntity(id);
        if(entity instanceof Building) {
            this.buildingList.remove(entity);
        }
    }

    @Override
    public EntityID getNewTarget(int time) {
        StandardEntity result = PositionUtil.getNearTarget(this.provider.getWorld(), this.provider.getOwner(), this.buildingList);
        return result != null ? result.getID() : null;
    }
    
    @Override
    public EntityID getNewTarget(int time, List<EntityID> cluster, List<EntityID> notCluster) {
    	
    	Set<Building> buildingsListCluster = new HashSet<>();
    	for(Building b : this.buildingList){
    		if(cluster.contains(b.getID())){
    			buildingsListCluster.add(b);
    		}
    	}
    	
    	StandardEntity result;
    	if(buildingsListCluster.isEmpty()){
    		result = PositionUtil.getNearTarget(this.provider.getWorld(), this.provider.getOwner(), this.buildingList);
    	}else{
    		result = PositionUtil.getNearTarget(this.provider.getWorld(), this.provider.getOwner(), buildingsListCluster);
    	}
        return result != null ? result.getID() : null;
    }

    @Override
    public EntityID updateTarget(int time, EntityID target) {
    	Building building = (Building)provider.getWorld().getEntity(target);
    	if(this.buildingList.contains(building) && building.isOnFire()){
    		return target;
    	}
    	return getNewTarget(time);
    }
}
