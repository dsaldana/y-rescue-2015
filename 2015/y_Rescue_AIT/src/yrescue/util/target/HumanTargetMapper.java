package yrescue.util.target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import adk.sample.basic.util.BasicRouteSearcher;
import adk.team.util.RouteSearcher;
import adk.team.util.provider.WorldProvider;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import yrescue.search.YRescueRouteSearcher;
import yrescue.util.YRescueVictimSelector;

public class HumanTargetMapper {
	private WorldProvider provider;
	private Map<EntityID, HumanTarget> humanMap;
	private Human owner;
	private RouteSearcher routeSearcher;
	
	public HumanTargetMapper(WorldProvider provider) {
		this.humanMap = new HashMap<>();
		this.provider = provider;
		this.owner = (Human) provider.getOwner();
		this.routeSearcher = new BasicRouteSearcher(this.provider);
	}
	
	public HumanTargetMapper(WorldProvider provider, RouteSearcher routesearcher) {
		this.humanMap = new HashMap<>();
		this.provider = provider;
		this.owner = (Human) provider.getOwner();
		this.routeSearcher = routesearcher;
	}
	
	public void addTarget(Civilian civilian){
		if(civilian.getBuriedness() > 0) {
            this.humanMap.put(civilian.getID(), new HumanTarget((Human) civilian, HumanTarget.HumanTypes.CIVILIAN, provider, this.routeSearcher));
        }
        else {
            this.humanMap.remove(civilian.getID());
        }
	}
	
	public void addTarget(Human agent) {
		StandardEntity entity = this.provider.getWorld().getEntity(agent.getID());
        if(agent.getBuriedness() > 0) {
            if(entity instanceof PoliceForce){
            	this.humanMap.put(agent.getID(), new HumanTarget((Human) agent, HumanTarget.HumanTypes.POLICE, provider, this.routeSearcher));
            }
            else if(entity instanceof AmbulanceTeam){
            	this.humanMap.put(agent.getID(), new HumanTarget((Human) agent, HumanTarget.HumanTypes.AMBULANCE, provider, this.routeSearcher));
            }
            else {
            	this.humanMap.put(agent.getID(), new HumanTarget((Human) agent, HumanTarget.HumanTypes.FIREMAN, provider, this.routeSearcher));
            }
        }
        else {
            this.humanMap.remove(agent.getID());
        }
	}
	
	public void addTarget(EntityID id) {
		StandardEntity entity = this.provider.getWorld().getEntity(id);
        if(entity instanceof Civilian) {
            this.humanMap.put(entity.getID(), new HumanTarget((Human) entity, HumanTarget.HumanTypes.CIVILIAN, provider, this.routeSearcher));
        }
        else if(entity instanceof Human) {
            if(entity instanceof PoliceForce){
            	this.humanMap.put(entity.getID(), new HumanTarget((Human) entity, HumanTarget.HumanTypes.POLICE, provider, this.routeSearcher));
            }
            else if(entity instanceof AmbulanceTeam){
            	this.humanMap.put(entity.getID(), new HumanTarget((Human) entity, HumanTarget.HumanTypes.AMBULANCE, provider, this.routeSearcher));
            }
            else {
            	this.humanMap.put(entity.getID(), new HumanTarget((Human) entity, HumanTarget.HumanTypes.FIREMAN, provider, this.routeSearcher));
            }
        }
	}
	
	public List<HumanTarget> getAllHumanTargets(){
		return new ArrayList<HumanTarget>(this.humanMap.values());
	}
	
    public void removeTarget(Civilian civilian) {
        this.humanMap.remove(civilian.getID());
    }

    public void removeTarget(Human agent) {
        this.humanMap.remove(agent.getID());
    }

    public void removeTarget(EntityID id) {
        StandardEntity entity = this.provider.getWorld().getEntity(id);
        this.humanMap.remove(entity.getID());
    }
    
    public HumanTarget getBestTarget(int time){
    	updateUtilities(time);
    	List<HumanTarget> humanList = getAllHumanTargets();
    	humanList = orderHumanTargetList(humanList);
    	
    	if(humanList.size() <= 0) return null;
    	return humanList.get(0);
    }
    
    public HumanTarget getTarget(EntityID ent){
    	return humanMap.get(ent);
    }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<HumanTarget> orderHumanTargetList(List<HumanTarget> htList) {

		Collections.sort(htList, new Comparator() {

			public int compare(Object o1, Object o2) {

				Float x1 = ((HumanTarget) o1).getUtility();
				Float x2 = ((HumanTarget) o2).getUtility();
				int sComp = x1.compareTo(x2);

				return sComp;
			}
		});

		return htList;
	}
	
	public void updateUtilities(int time){
		List<HumanTarget> ambulanceList = new LinkedList<HumanTarget>();
    	List<HumanTarget> humanList = new ArrayList<HumanTarget>(this.humanMap.values());
    	
    	for(HumanTarget ht : humanList){
    		if(ht.getHumanType().equals(HumanTarget.HumanTypes.AMBULANCE)){
    			ambulanceList.add(ht);
    		}
    	}
    	
    	for(HumanTarget ht : humanList){
    		if(!ht.getHuman().isHPDefined() || ht.getHuman().getHP() <= 100) removeTarget(ht.getHuman().getID());
    		else{
    			if(this.routeSearcher instanceof BasicRouteSearcher && ((BasicRouteSearcher) this.routeSearcher).getMapCache() == null ||  ((BasicRouteSearcher) this.routeSearcher).getMapCache().isEmpty()){
    				ht.updateUtility(time, this.owner, ambulanceList, true);
    			}
    			else{
    				ht.updateUtility(time, this.owner, ambulanceList, false);
    			}
    			
    		}
    	}
	}
	
}
