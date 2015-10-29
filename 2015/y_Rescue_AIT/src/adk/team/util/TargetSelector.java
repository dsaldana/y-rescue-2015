package adk.team.util;

import java.util.List;

import rescuecore2.worldmodel.EntityID;

public interface TargetSelector {

    public void add(EntityID id);

    public void remove(EntityID id);

    public EntityID getNewTarget(int time);
    
    public EntityID getNewTarget(int time, List<EntityID> cluster, List<EntityID> notCluster);

    public EntityID updateTarget(int time, EntityID target);
}