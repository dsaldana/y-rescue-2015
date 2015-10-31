package yrescue.util;

import adk.sample.basic.util.BasicRouteSearcher;
import adk.team.util.graph.PositionUtil;
import adk.team.util.RouteSearcher;
import adk.team.util.VictimSelector;
import adk.team.util.provider.WorldProvider;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import yrescue.util.target.HumanTarget;
import yrescue.util.target.HumanTargetMapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class YRescueVictimSelector implements VictimSelector {

    public WorldProvider<? extends StandardEntity> provider;

    public Set<Civilian> civilianList;
    public Set<Human> agentList;
    public HumanTargetMapper humanTargetM;
    private RouteSearcher routerSearcher;

    public YRescueVictimSelector(WorldProvider<? extends StandardEntity> user, RouteSearcher routerSearcher) {
        this.provider = user;
        this.civilianList = new HashSet<>();
        this.agentList = new HashSet<>();
        this.routerSearcher = routerSearcher;
        this.humanTargetM = new HumanTargetMapper(this.provider, this.routerSearcher);
    }
    
    public YRescueVictimSelector(WorldProvider<? extends StandardEntity> user) {
        this.provider = user;
        this.civilianList = new HashSet<>();
        this.agentList = new HashSet<>();
        this.humanTargetM = new HumanTargetMapper(this.provider, this.routerSearcher);
        this.routerSearcher = new BasicRouteSearcher((WorldProvider<? extends Human>) this.provider);
    }

    @Override
    public void add(Civilian civilian) {
        if(civilian.getBuriedness() > 0) {
            this.civilianList.add(civilian);
        }
        else {
            this.civilianList.remove(civilian);
        }
        
        this.humanTargetM.addTarget(civilian);
    }

    @Override
    public void add(Human agent) {
    	StandardEntity entity = this.provider.getWorld().getEntity(agent.getID());
        if(agent.getBuriedness() > 0) {
            this.agentList.add(agent);
        }
        else {
            this.agentList.remove(agent);
        }
        
        this.humanTargetM.addTarget(agent);
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

        this.humanTargetM.addTarget(id);
    }

    @Override
    public void remove(Civilian civilian) {
        this.civilianList.remove(civilian);
        this.humanTargetM.removeTarget(civilian);
    }

    @Override
    public void remove(Human agent) {
        this.agentList.remove(agent);
        this.humanTargetM.removeTarget(agent);
    }

    @Override
    public void remove(EntityID id) {
        StandardEntity entity = this.provider.getWorld().getEntity(id);
        if(entity instanceof Civilian) {
            this.civilianList.remove(entity);
        }
        else if(entity instanceof Human) {
            this.agentList.remove(entity);
        }
        this.humanTargetM.removeTarget(id);
    }

    @Override
    public EntityID getNewTarget(int time) {
    	HumanTarget ht = humanTargetM.getBestTarget(time);
    	if(ht != null) return ht.getHuman().getID();
    	
    	StandardEntity result = PositionUtil.getNearTarget(this.provider.getWorld(), this.provider.getOwner(), this.civilianList);
        if(result == null) {
            result = PositionUtil.getNearTarget(this.provider.getWorld(), this.provider.getOwner(), this.agentList);
        }
        return result != null ? result.getID() : null;
    }
    
    @Override
    public EntityID getNewTarget(int time, List<EntityID> cluster, List<EntityID> notCluster) {
        return getNewTarget(time);
    }
    
    public HumanTarget getHumanTarget(EntityID entity){
    	return this.humanTargetM.getTarget(entity);
    }

    @Override
    public EntityID updateTarget(int time, EntityID target) {
        Human victim = (Human) this.provider.getWorld().getEntity(target);
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