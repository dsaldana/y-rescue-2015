package adk.sample.basic.util;

import adk.team.util.graph.PositionUtil;
import adk.team.util.VictimSelector;
import adk.team.util.provider.WorldProvider;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class BasicVictimSelector implements VictimSelector {

    public WorldProvider<? extends StandardEntity> provider;

    public Set<Civilian> civilianList;
    public Set<Human> agentList;

    public BasicVictimSelector(WorldProvider<? extends StandardEntity> user) {
        this.provider = user;
        this.civilianList = new HashSet<>();
        this.agentList = new HashSet<>();
    }

    @Override
    public void add(Civilian civilian) {
        if(civilian.getBuriedness() > 0) {
            this.civilianList.add(civilian);
        }
        else {
            this.civilianList.remove(civilian);
        }
    }

    @Override
    public void add(Human agent) {
        if(agent.getBuriedness() > 0) {
            this.agentList.add(agent);
        }
        else {
            this.agentList.remove(agent);
        }
    }

    @Override
    public void add(EntityID id) {
        StandardEntity entity = this.provider.getWorld().getEntity(id);
        if(entity instanceof Civilian) {
            this.add((Civilian)entity);
        }
        else if(entity instanceof Human) {
            this.add((Human)entity);
        }
    }

    @Override
    public void remove(Civilian civilian) {
        this.civilianList.remove(civilian);
    }

    @Override
    public void remove(Human agent) {
        this.agentList.remove(agent);
    }

    @Override
    public void remove(EntityID id) {
        StandardEntity entity = this.provider.getWorld().getEntity(id);
        if(entity instanceof Civilian) {
            this.civilianList.remove(entity);
            //this.agentList.remove(entity);
        }
        else if(entity instanceof Human) {
            this.agentList.remove(entity);
        }
    }

    @Override
    public EntityID getNewTarget(int time) {
        StandardEntity result = PositionUtil.getNearTarget(this.provider.getWorld(), this.provider.getOwner(), this.agentList);
        if(result == null) {
            result = PositionUtil.getNearTarget(this.provider.getWorld(), this.provider.getOwner(), this.civilianList);
        }
        return result != null ? result.getID() : null;
    }
    
    @Override
    public EntityID getNewTarget(int time, List<EntityID> cluster, List<EntityID> notCluster) {
    	return getNewTarget(time);
    }

    @Override
    public EntityID updateTarget(int time, EntityID target) {
        Human victim = (Human)this.provider.getWorld().getEntity(target);
        EntityID victimPositionID = victim.getPosition();
        if(victim.getBuriedness() == 0) {
            for(StandardEntity ambulance : this.provider.getWorld().getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)) {
                if(victimPositionID.getValue() == ((AmbulanceTeam)ambulance).getPosition().getValue()) {
                    if(this.provider.getID().getValue() < ambulance.getID().getValue()) {
                        this.remove(target);
                        return this.getNewTarget(time);
                    }
                }
            }
        }
        return target;
    }
}
