package agent.platoon;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Map;

import org.apache.log4j.MDC;

import message.MessageEncoder;
import message.MessageReceiver;
import message.MessageType;
import message.ReceivedMessage;
import problem.BlockedRoad;
import problem.BurningBuilding;
import problem.Problem;
import problem.WoundedHuman;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.properties.IntArrayProperty;
import rescuecore2.Constants;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.kernel.comms.StandardCommunicationModel;
import util.LastVisitSorter;
import util.SampleSearch;

/**
   Abstract base class for sample agents.
   @param <E> The subclass of StandardEntity this agent wants to control.
 */
public abstract class AbstractPlatoon<E extends StandardEntity> extends StandardAgent<E> {
    private static final int RANDOM_WALK_LENGTH = 50;

    private static final String SAY_COMMUNICATION_MODEL = StandardCommunicationModel.class.getName();
    private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class.getName();
    
    /**
     * Current timestep
     */
    protected int time;
    
    /**
     * Current changeset
     */
    protected ChangeSet changed;
    
    /**
     * Current listened communication
     */
    protected Collection<Command> heard;

    /**
       The search algorithm.
    */
    protected SampleSearch search;
    
    /**
     * Stores my last location
     */
    protected EntityID lastLocationID;

    /**
       Whether to use AKSpeak messages or not.
    */
    protected boolean useSpeak;

    /**
       Cache of building IDs.
    */
    protected List<EntityID> buildingIDs;

    /**
       Cache of road IDs.
    */
    protected List<EntityID> roadIDs;

    /**
       Cache of refuge IDs.
    */
    protected List<EntityID> refugeIDs;
    
    /**
     * Stores when entities were last visited
     */
    protected Map<EntityID, Integer> lastVisit;
    
    /**
    Cache of refuge IDs.
 */
    protected List<EntityID> hydrantIDs;
    
    /**
    Cache of water source IDs.
     */
    protected List<EntityID> waterSourceIDs;
    private Map<EntityID, Set<EntityID>> neighbours;
    
    /**
     * The following attributes are Maps<problem,boolean>.
     * A problem can be a BlockedRoad, a WoundedCivilian or a BurningBuilding
     * The boolean indicates whether the problem is solved or not.
     * During think(), the agent must update these structures 
     */
    protected Map<EntityID, BlockedRoad> blockedRoads;
    protected Map<EntityID, WoundedHuman> woundedHumans;
    protected Map<EntityID, BurningBuilding> burningBuildings;
    
    /**
     * A list of problems that the agent will send to teammates
     */
    protected List<Problem> problemsToReport;

	
    /**
     * Construct an AbstractRobot.
     */
    protected AbstractPlatoon() {
    	blockedRoads = new HashMap<EntityID, BlockedRoad>();
    	woundedHumans = new HashMap<EntityID, WoundedHuman>();
    	burningBuildings = new HashMap<EntityID, BurningBuilding>();
    	
    	problemsToReport = new ArrayList<Problem>();
    	
    	
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        buildingIDs = new ArrayList<EntityID>();
        roadIDs = new ArrayList<EntityID>();
        refugeIDs = new ArrayList<EntityID>();
        hydrantIDs = new ArrayList<EntityID>();
        waterSourceIDs = new ArrayList<EntityID>();
        
        lastVisit = new HashMap<EntityID, Integer>();
        
        for (StandardEntity next : model) {
            if (next instanceof Building) {
                buildingIDs.add(next.getID());
            }
            if (next instanceof Road) {
                roadIDs.add(next.getID());
            }
            if (next instanceof Refuge) {
                refugeIDs.add(next.getID());
            }
            if (next instanceof Hydrant) {
                hydrantIDs.add(next.getID());
            }
            if (next instanceof Area){ //Building and Road extends area
            	//last visit is 'infinite' for unknown places
            	lastVisit.put(next.getID(), -1);
            }
            
        }
        MDC.put("agent", me());
        MDC.put("location", location());
        
        lastLocationID = location().getID(); //initializes last location as the current
        
        waterSourceIDs.addAll(refugeIDs);
        waterSourceIDs.addAll(hydrantIDs);
        search = new SampleSearch(model);
        neighbours = search.getGraph();
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(SPEAK_COMMUNICATION_MODEL);
        Logger.debug("Communcation model: " + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(useSpeak ? "Using speak model" : "Using say model");
    }
    
    
    /**
     * Updates the agent knowledge then calls the abstract 
     * method doThink() which should be implemented by each agent class.
     * The doThink call is 'protected' by an exception handling, preventing the agent from dying
     * @param time
     * @param changed
     * @param heard
     */
    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
    	try{
    		this.time = time;
    		this.changed = changed;
    		this.heard = heard;
    		
    		MDC.put("location", location());
    		MDC.put("time", time);
    		
    		//Logger.info("Time: " + time);
    		Logger.info(("Heard:" + heard));
    		//Logger.info("" + changed);
    		//Logger.info("" + me().getFullDescription());
    		//model.getDistance(first, second)
    		
    		updateVisitHistory();
    		
    		//IntArrayProperty positionHist = (IntArrayProperty)changed.getChangedProperty(getID(), "urn:rescuecore2.standard:property:positionhistory");
    		//Logger.info("History: " + positionHist + positionHist.getValue());
    		
    		hear(time, heard);
    		updateKnowledge(time, changed);
    		doThink(time, changed, heard);
    		sendMessages(time);
    		
    		lastLocationID = location().getID();	//updates last location
    	}
    	catch (Exception e){
    		Logger.error(("System malfunction! (exception occurred)"), e);
    	}
    }
    
    private void updateVisitHistory(){
    	
    	lastVisit.put(location().getID(), time);	//stores that current location was visited now
    	
    	
    	IntArrayProperty positionHist = (IntArrayProperty) me().getProperty("urn:rescuecore2.standard:property:positionhistory");
    	int[] positionList = positionHist.getValue();

    	Logger.debug(("Position hist: " + positionHist));
    	if (!positionHist.isDefined()) {
    		Logger.info("Empty position list. I'm (possibly stopped) at " + location());
    		return;
    	}
    	
    	for (int i = 0; i < positionList.length; i++){
    		int x = positionList[i];
    		int y = positionList[i+1];
    		i++;
    		
    		Collection<StandardEntity> intersectz = model.getObjectsInRectangle(x, y, x, y);	//obtains the object that intersect with a point where the agent has been
    		
    		//Logger.info(String.format("Found %d entities @ (%d,%d)", intersectz.size(), x, y));
    		
    		for(StandardEntity entity: intersectz){
    			if (entity instanceof Area) {
    				lastVisit.put(entity.getID(), time);
    				//Logger.info("Entity " + entity.getID() + " updated @ " + time);
    			}
    		}
    	}
    }
    
    /**
     * 
     * @param path1
     * @param path2
     * @return The shortest path 
     */
    protected List<EntityID> shortestPath(List<EntityID> path1, List<EntityID> path2)
    {
    	return path2;
    }
    
    /**
     * Processes the messages received
     * @param time
     * @param heard
     */
    protected void hear(int time, Collection<Command> heard) {
    	decodeBlockedRoadMessages(heard);
    	decodeBurningBuildingMessages(heard);
    	decodeWoundedHumanMessages(heard);
    }
    
    /**
     * Sends the messages reporting problems the agent has seen in this cycle
     * @param time
     */
    protected void sendMessages(int time){
		Logger.info(("#burning bldgs:" + burningBuildings.size()));
		Logger.info(("#wounded humans:" + woundedHumans.size()));
		Logger.info(("#blocked roads:" + blockedRoads.size()));
		//Logger.info(("the blk roads:" + blockedRoads.keySet()));
    	Logger.info("#problemsToReport: " + problemsToReport.size());
    	for(Problem p : problemsToReport){
    		Logger.info((String.format("%s will communicate problem %s", me(), p)));
    		byte[] msg = p.encodeReportMessage(getID());
    		sendSay(time, msg);
    		sendSpeak(time, 1, msg);	//TODO implementar alocação de canais
    	}
    	
    	problemsToReport.clear();
    }
    
    /**
     * Returns the time of the last visit to an entity 
     * @param id
     * @return int
     */
    public int lastVisit(EntityID id){
    	if (! lastVisit.containsKey(id)){
    		throw new RuntimeException("ID" + id + "does not refer to a Building or Road");
    	}
    	return lastVisit.get(id);
    }
    
    protected void decodeBlockedRoadMessages(Collection<Command> heard){
    	for(Command next : heard){
    		ReceivedMessage msg = MessageReceiver.decodeMessage(next);
    		if (msg == null || !(msg.problem instanceof BlockedRoad)) continue; //skips 'broken' and wrong type msgs
    		
    		BlockedRoad b = (BlockedRoad) msg.problem;
    		
    		//if-elses to filter message by type
    		if(msg.msgType == MessageType.REPORT_BLOCKED_ROAD){
    			
    			updateIfNewer(b);
    			//else discards message (incoming problem is older than the one I know
    		}
    		else if(msg.msgType == MessageType.SOLVED_BLOCKED_ROAD){
    			updateIfNewer(b);
    			blockedRoads.get(b).markSolved(next.getTime()); //ensures that problem is marked as solved
    		}
    	}
    }
    
    protected void decodeWoundedHumanMessages(Collection<Command> heard){
    	for(Command next : heard){
    		ReceivedMessage msg = MessageReceiver.decodeMessage(next);
    		if (msg == null || !(msg.problem instanceof WoundedHuman)) continue; //skips 'broken' and wrong type msgs
    		
    		WoundedHuman h = (WoundedHuman) msg.problem;
    		
    		//if-elses to filter message by type
    		if(msg.msgType == MessageType.REPORT_WOUNDED_HUMAN){
    			
    			updateIfNewer(h);
    			//else discards message (incoming problem is older than the one I know
    		}
    		else if(msg.msgType == MessageType.SOLVED_WOUNDED_HUMAN){
    			updateIfNewer(h);
    			woundedHumans.get(h).markSolved(next.getTime()); //ensures that problem is marked as solved
    		}
    	}
    }
    
    protected void decodeBurningBuildingMessages(Collection<Command> heard){
    	for(Command next : heard){
    		ReceivedMessage msg = MessageReceiver.decodeMessage(next);
    		if (msg == null || !(msg.problem instanceof BurningBuilding)) continue; //skips 'broken' and wrong type msgs
    		
    		Logger.info((String.format("received msg %s", msg)));
    		
    		BurningBuilding bb = (BurningBuilding) msg.problem;
    		
    		//if-elses to filter message by type
    		if(msg.msgType == MessageType.REPORT_BURNING_BUILDING){
    			
    			updateIfNewer(bb);
    			//else discards message (incoming problem is older than the one I know
    		}
    		else if(msg.msgType == MessageType.SOLVED_BURNING_BUILDING){
    			updateIfNewer(bb);
    			burningBuildings.get(bb).markSolved(next.getTime()); //ensures that problem is marked as solved
    		}
    	}
    }

	/**
	 * Inserts the problem into knowledge base if it does not exists in there
	 * or it is newer than the previously existing one
	 * @param b
	 * @return 
	 */
	private void updateIfNewer(BlockedRoad b) {
		//checks whether I already know this problem or if the incoming problem is more recent
		if(!blockedRoads.containsKey(b) || b.getUpdateTime() > blockedRoads.get(b).getUpdateTime() ){
			blockedRoads.put(b.getEntityID(), b);
			//problemsToReport.add(b);
			Logger.info(String.format("Added %s to problems to report", b));
		}
	}
	
	private void updateIfNewer(WoundedHuman h) {
		//checks whether I already know this problem or if the incoming problem is more recent
		if(!woundedHumans.containsKey(h) || h.getUpdateTime() > woundedHumans.get(h).getUpdateTime() ){
			woundedHumans.put(h.getEntityID(), h);
			//problemsToReport.add(h);
			Logger.info(String.format("Added %s to problems to report", h));
		}
	}
	
	private void updateIfNewer(BurningBuilding bb) {
		//checks whether I already know this problem or if the incoming problem is more recent
		if(!burningBuildings.containsKey(bb) || bb.getUpdateTime() > burningBuildings.get(bb).getUpdateTime() ){
			//updates the model too
			Building b = (Building) model.getEntity(bb.getEntityID());
			b.setBrokenness(bb.brokenness);
			b.setFieryness(bb.fieryness);
			b.setTemperature(bb.temperature);
			//b.setIgnition(! bb.isSolved());
			
			//sanity check 
			if (! b.isBuildingAttributesDefined()) {
				Logger.error((String.format("Building %d attributes not defined!", b.getID().getValue())));
			}
			
			burningBuildings.put(bb.getEntityID(), bb);
			//problemsToReport.add(bb);
			Logger.info(String.format("Added %s to problems to report", bb));
			
			Logger.info(("Updated info of bldg " + b.getID()));
		}
		else {
			Logger.info(("Discarded info of bldg " + bb.getEntityID() + ". Outdated."));
		}
	}
    
    
    /**
     * Incorporate the changes observed in this timestep to the 
     * agent's knowledge
     * @param time
     * @param changed
     */
    private void updateKnowledge(int time, ChangeSet changed){
    	updateBurningBuildings(time, changed);
    	updateBlockedRoads(time, changed);
    	updateWoundedHumans(time, changed);
    }
    
    /**
     * Looks for burning buildings in the ChangeSet and adds/updates them 
     * in knowledge base if necessary
     * @param time
     * @param changed
     */
    private void updateBurningBuildings(int time, ChangeSet changed) {
    	//looks for burning buildings in the changeset
    	for(EntityID id : changed.getChangedEntities()){
        	Entity entity = model.getEntity(id);
        	//if entity is a building, casts it and tests whether it is burning
        	if(entity instanceof Building){
        		Building b = (Building) entity;
        		
        		//if building is burning, adds it to knowledge base or update its entry. 
        		//else, mark it as solved if it was on knowledge base
                if (b.isOnFire()) {
                    updateBurningBuilding(time, b);
                }
                else {
                	//mark as solved if exists (since it is not on fire)
                	if(burningBuildings.containsKey(b.getID())){
                		BurningBuilding notBurningAnymore = burningBuildings.get(b.getID());
                		notBurningAnymore.update(b.getBrokenness(), b.getFieryness(), b.getTemperature(), time);
                		notBurningAnymore.markSolved(time);
                	}
                }
        	}
        }
	}
    
    /**
     * Looks for wounded humans in the changeset and adds or updates them
     * in the knowledge base
     * @param time
     * @param changed
     */
    private void updateWoundedHumans(int time, ChangeSet changed) {
    	for(EntityID id : changed.getChangedEntities()){
        	Entity entity = model.getEntity(id);
        	//if entity is a Human, casts it and tests whether it is burning
        	if(entity instanceof Human){
        		Human h = (Human) entity;
        		
        		//if human is hurt or buried, adds it to knowledge base or update its entry. 
        		//else, mark it as solved if it was on knowledge base
                if (h.getDamage() > 0 || h.getBuriedness() > 0) {
                    updateWoundedHuman(time, h);
                }
                else {
                	//mark as solved if exists (since it is not hurt or buried)
                	if(woundedHumans.containsKey(h.getID())){
                		WoundedHuman notWoundedAnymore = woundedHumans.get(h.getID());
                		notWoundedAnymore.update(h.getPosition(), h.getBuriedness(), h.getHP(), h.getDamage(), time);;
                		notWoundedAnymore.markSolved(time);
                	}
                }
        	}
        }
	}
    
    private void updateBlockedRoads(int time, ChangeSet changed) {
    	for(EntityID id : changed.getChangedEntities()){
        	Entity entity = model.getEntity(id);
        	//if entity is a Road, casts it and tests whether it has blockades
        	if(entity instanceof Road){
        		Road r = (Road) entity;
        		//if road is blocked, adds it to knowledge base or update its entry. 
        		//else, mark it as solved if it was on knowledge base
                if ( r.getBlockades()!= null && !r.getBlockades().isEmpty()) {
                    updateBlockedRoad(time, r);
                }
                else {
                	//mark as solved if exists (since it has no blockades)
                	if(blockedRoads.containsKey(r.getID())){
                		BlockedRoad notBlockedAnymore = blockedRoads.get(r.getID());
                		
                		notBlockedAnymore.update(calculateRepairCost(r.getBlockades()), time);
                		notBlockedAnymore.markSolved(time);
                	}
                }
        	}
        }
		
	}
    
    /**
     * Updates blocked road if it exists in HashMap or adds it if it doesn't exist
     * @param time
     * @param r Road object with current data to be put on the blocked road problem
     */
    private void updateBlockedRoad(int time, Road r) {
    	BlockedRoad blocked;
		if (blockedRoads.containsKey(r.getID())){
			blocked = blockedRoads.get(r.getID());
			blocked.update(calculateRepairCost(r.getBlockades()), time);
		}
		else{
			blocked = new BlockedRoad(r.getID(), calculateRepairCost(r.getBlockades()), time);
			blockedRoads.put(r.getID(), blocked);
		}
		problemsToReport.add(blocked);
	}

	/**
	 * Updates human data if exists in HashMap or adds it if it doesn't exist
	 * @param time
	 * @param h the Human object with current data to be put on the wounded human problem
	 */
	private void updateWoundedHuman(int time, Human h) {
		WoundedHuman wounded;
		if (woundedHumans.containsKey(h.getID())){
			wounded = woundedHumans.get(h.getID());
			wounded.update(h.getPosition(), h.getBuriedness(), h.getHP(), h.getDamage(), time);
		}
		else{
			wounded = new WoundedHuman(h.getID(), h.getPosition(), h.getBuriedness(), h.getHP(), h.getDamage(), time);
			woundedHumans.put(h.getID(), wounded);
		}
		problemsToReport.add(wounded);
	}

	/**
	 * Updates building data if exists in HashMap or adds it if it doesn't exist
	 * @param time
	 * @param b the Building object with current data to be put on the burning building problem
	 */
	private void updateBurningBuilding(int time, Building b) {
		BurningBuilding burning;
		if (burningBuildings.containsKey(b.getID())){
			burning = burningBuildings.get(b.getID());
			burning.update(b.getBrokenness(), b.getFieryness(), b.getTemperature(), time);
		}
		else{
			burning = new BurningBuilding(b.getID(), b.getBrokenness(), b.getFieryness(), b.getTemperature(), time);
			burningBuildings.put(b.getID(), burning);
		}
		problemsToReport.add(burning);
	}
	
	/**
	 * Calculates the total repair costs of the list of blockades
	 * @param blockades
	 * @return
	 */
	public int calculateRepairCost(List<EntityID> blockades){
		int repairCost = 0;
		for (EntityID next : blockades) {
            Blockade b = (Blockade)model.getEntity(next);
            repairCost += b.getRepairCost();
		}
		return repairCost;
	}

    

	/**
     * Does the actual processing. Each agent class must implement this method.
     * @param time
     * @param changed
     * @param heard
     */
    protected abstract void doThink(int time, ChangeSet changed, Collection<Command> heard);

    /**
       Construct a random walk starting from this agent's current location to a random building.
       @return A random walk.
    */
    protected List<EntityID> randomWalk() {
    	return explore();
    	/*
        List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
        Set<EntityID> seen = new HashSet<EntityID>();
        EntityID current = ((Human)me()).getPosition();
        
        for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
            result.add(current);
            seen.add(current);
            List<EntityID> possible = new ArrayList<EntityID>(neighbours.get(current));
            Collections.shuffle(possible, random);
            boolean found = false;
            for (EntityID next : possible) {
                if (seen.contains(next)) {
                    continue;
                }
                current = next;
                found = true;
                break;
            }
            if (!found) {
                // We reached a dead-end.
                break;
            }
        }
        return result;*/
    }
    
    /**
     * Attempts to reach unexplored places (a small random walk enhancement)
     * @return
     */
    protected List<EntityID> explore() {
        List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
        Set<EntityID> seen = new HashSet<EntityID>();
        EntityID current = ((Human)me()).getPosition();
        
        model.getEntitiesOfType(StandardEntityURN.BUILDING, StandardEntityURN.ROAD);
        
        for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
            result.add(current);
            seen.add(current);
            List<EntityID> possible = new ArrayList<EntityID>(neighbours.get(current));
            //Collections.shuffle(possible, random);
            Collections.sort(possible, new LastVisitSorter(this));	//we want the most recent visit to be the last
            //Collections.reverse(possible); 
            
            /*String str_possible = "[";
            for (EntityID p : possible) str_possible += p + ":" + lastVisit(p) + ", ";
            str_possible += "]";
            
            Logger.info("" + me() + "possible:" + str_possible);
            */
            boolean found = false;
            for (EntityID next : possible) {
                StandardEntity e = model.getEntity(next);
            	
                //discards entities already in the path and burning buildings
            	if (seen.contains(next))  {
                    continue;
                }
                //discards the burning building if there are other possibilities
            	//if all possibilities are burning buildings, agent gets stuck
                if (possible.size() > 1 && e instanceof Building && ((Building) e).isOnFire()) {
                	continue;
                }
                
                current = next;
                found = true;
                //Logger.info("selected: " + current);
                break;
            }
            if (!found) {
                // We reached a dead-end.
                break;
            }
        }
        /*
        //removes last entity of path if it is a burning building
        if (result.size() > 0){
        	StandardEntity last = model.getEntity(result.get(result.size() -1));
        	
        	if (last instanceof Building && ((Building) last).isOnFire()) {
        		Logger.info("Removing entity" + last + " from path because it is a burning building.");
        		result.remove(result.size() -1);
        	}
        	
        }
        */
        return result;
    }

	/**
	 * Just opens location() visibility to public 
	 * @return
	 */
    public StandardEntity getLocation() {
    	return location();
	}
}

