package agent.platoon;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.log4j.MDC;

import commands.AgentCommand;
import commands.AgentCommands;
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
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.kernel.comms.StandardCommunicationModel;
import search.NeighborhoodGraph;
import search.SearchStrategy;
import search.sample.SampleSearch;
import search.ysearch.YGraphWrapper;
import search.ysearch.YSearch;
import statemachine.StateMachine;
import statemachine.ActionStates;
import util.LastVisitSorter;

/**
   Abstract base class for sample agents.
   @param <E> The subclass of StandardEntity this agent wants to control.
 */
public abstract class AbstractPlatoon<E extends StandardEntity> extends StandardAgent<E> {
    private static final int RANDOM_WALK_LENGTH = 60;

    private static final String SAY_COMMUNICATION_MODEL = StandardCommunicationModel.class.getName();
    private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class.getName();
    
    
    private static final String SIGHT_RANGE_KEY = "perception.los.max-distance";
    protected int sightRange;
    
    
    // Current timestep
    protected int time;
    
    // Current changeset
    protected ChangeSet changed;
    
    // Current listened communication
    protected Collection<Command> heard;
    
    //the platoon agents of each type
    protected Collection<StandardEntity> firefighters, policemen, ambulances;
    
    //the center agents of each type
    protected Collection<StandardEntity> fireStations, policeOffices, hospitals;
    
    // Agent state machine
    protected StateMachine stateMachine;
    
    protected boolean commandIssued; //register whether the agent issued a command in current timestep

    // The search strategy
    protected SearchStrategy searchStrategy;
    
    // The "failsafe' sample search stratety
    protected SampleSearch failSafeSearch;
    
    /**
       The search algorithm.
    */
    //protected SampleSearch search;
    
    // The new awesome search graph
    protected YGraphWrapper searchGraph;
    
    // Stores my last location
    protected EntityID lastLocationID;
    
    // Agent command history
    protected Map<Integer, AgentCommand> commandHistory;

    // Whether to use AKSpeak messages or not.
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
    protected CircularFifoQueue<EntityID> lastVisitQueue = new CircularFifoQueue<EntityID>(20);
    
    /**
     * Cache of refuge IDs.
     */
    protected List<EntityID> hydrantIDs;
    
    /**
    Cache of water source IDs.
     */
    protected List<EntityID> waterSourceIDs;
    protected Map<EntityID, Set<EntityID>> neighbours;
    
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
     * Construct an AbstractPlatoon.
     */
    protected AbstractPlatoon() {
    	blockedRoads = new HashMap<EntityID, BlockedRoad>();
    	woundedHumans = new HashMap<EntityID, WoundedHuman>();
    	burningBuildings = new HashMap<EntityID, BurningBuilding>();
    	
    	commandHistory = new HashMap<Integer, AgentCommand>();
    	
    	problemsToReport = new ArrayList<Problem>();
    	
    	stateMachine = new StateMachine(ActionStates.RANDOM_WALK);
    	
    	
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        Logger.info("postConnect of AbstractPlatoon");
        
        sightRange = config.getIntValue(SIGHT_RANGE_KEY);
        
        buildingIDs = new ArrayList<EntityID>();
        roadIDs = new ArrayList<EntityID>();
        refugeIDs = new ArrayList<EntityID>();
        hydrantIDs = new ArrayList<EntityID>();
        waterSourceIDs = new ArrayList<EntityID>();
        
        lastVisit = new HashMap<EntityID, Integer>();
        
        
        firefighters = getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE);
        fireStations = getEntitiesOfType(StandardEntityURN.FIRE_STATION);
        
        ambulances = getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM);
        hospitals = getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE);
        
        policemen = getEntitiesOfType(StandardEntityURN.POLICE_FORCE);
        policeOffices = getEntitiesOfType(StandardEntityURN.POLICE_OFFICE);
        
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
        MDC.put("agent", this);
        MDC.put("location", location());
        
        lastLocationID = location().getID(); //initializes last location as the current
        
        waterSourceIDs.addAll(refugeIDs);
        waterSourceIDs.addAll(hydrantIDs);
        
        //search = new SampleSearch(model);
        //searchGraph = new YGraphWrapper(model);
        neighbours = NeighborhoodGraph.buildNeighborhoodGraph(model);
        
        failSafeSearch = new SampleSearch(neighbours);
        
        try {
        	searchStrategy = new YSearch(model);
        	Logger.info("Using YSearch strategy");
        	//searchStrategy = new SampleSearch(neighbours);
        	
        	Logger.info("TEST!");
        	Logger.info("Dij:" + searchStrategy.shortestPath(new EntityID(297), new EntityID(273)).getPath());
        	Logger.info("BFS:" + new SampleSearch(neighbours).shortestPath(new EntityID(297), new EntityID(273)).getPath());
        }
        catch (Exception e) {
        	//falls back to safe, simpler SampleSearch
        	Logger.error("Could not create YSearch instance.", e);
        	searchStrategy = failSafeSearch;
        	Logger.info("Using fail-safe SampleSearch");
        }
        //Logger.info("\n"+searchGraph.dumpNodes());
        
        Logger.info(String.format(
    		"I can count %d firefighters, %d ambulances and %d policemen", 
    		firefighters.size(), ambulances.size(), policemen.size()
    	));
        
        Logger.info(String.format(
    		"I can count %d fire stations, %d hospitals and %d police offices", 
    		fireStations.size(), hospitals.size(), policeOffices.size()
    	));
        
        
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(SPEAK_COMMUNICATION_MODEL);
        Logger.debug("Communcation model: " + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(useSpeak ? "Using speak model" : "Using say model");
    }
    
    /**
     * Performs save retrieval of entities
     * @param urn
     * @return
     */
    protected Collection<StandardEntity> getEntitiesOfType(StandardEntityURN urn){
    	try{
    		return model.getEntitiesOfType(urn);
        	//Logger.info("I can count "+firefighters+" firefighters");
        }
        catch (Exception e){
        	//prints error message and returns empty list
        	Logger.error("Cannot retrieve list of " + urn, e);
        	return new LinkedList<StandardEntity>();
        }
    	
    }
    
    /**
     * Reads a value from config. In case of error, returns the default value
     * @param key
     * @param defaultValue
     * @return 
     */
    protected int readConfigIntValue(String key, int defaultValue){
    	int value = defaultValue;
    	
    	try{
    		value = config.getIntValue(key);
    	}
    	catch (Exception e){
    		Logger.error(String.format("Cannot read config key %s. Will return default: %d", key, value), e);
    		Logger.info("Check the keys: " + config.getAllKeys());
    	}
    	
    	return value;
    }
    
    /**
     * Overrides methods to provide registration of actions
     */
    
    @Override
    protected void sendRest(int time){
    	commandHistory.put(time, AgentCommands.REST);
    	Logger.info("Sending REST command");
    	super.sendRest(time);
    	commandIssued = true;
    }
    
    @Override
    protected void sendMove(int time, List<EntityID> path){
    	commandHistory.put(time, AgentCommands.MOVE);
    	Logger.info("Sending MOVE command with: " + path);
    	super.sendMove(time, path);
    	commandIssued = true;
    }
    
    @Override
    protected void sendExtinguish(int time, EntityID target, int water){
    	commandHistory.put(time, AgentCommands.FireFighter.EXTINGUISH);
    	Logger.info(String.format("Sending EXTINGUISH command with: %s, %d", target, water));
    	super.sendExtinguish(time, target, water);
    	commandIssued = true;
    }
    
    @Override
    protected void sendRescue(int time, EntityID target){
    	commandHistory.put(time, AgentCommands.Ambulance.RESCUE);
    	Logger.debug(String.format("Sending RESCUE command with: %s", target));
    	super.sendRescue(time, target);
    	commandIssued = true;
    }
    
    @Override
    protected void sendLoad(int time, EntityID target){
    	commandHistory.put(time, AgentCommands.Ambulance.LOAD);
    	Logger.debug(String.format("Sending LOAD command with: %s", target));
    	super.sendLoad(time, target);
    	commandIssued = true;
    }

    @Override
    protected void sendUnload(int time){
    	commandHistory.put(time, AgentCommands.Ambulance.UNLOAD);
    	Logger.debug(String.format("Sending UNLOAD command"));
    	super.sendUnload(time);
    	commandIssued = true;
    }
    
    @Override
    protected void sendClear(int time, EntityID target){
    	commandHistory.put(time, AgentCommands.Policeman.CLEAR);
    	Logger.debug(String.format("Sending CLEAR command with: %s", target));
    	super.sendClear(time, target);
    	commandIssued = true;
    }
    
    @Override
    protected void sendClear(int time, int destX, int destY){
    	commandHistory.put(time, AgentCommands.Policeman.CLEAR);
    	Logger.debug(String.format("Sending EXTINGUISH command with: %d, %d", destX, destY));
    	super.sendClear(time, destX, destY);
    	commandIssued = true;
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
    		Logger.info(String.format("------- START OF TIMESTEP %d -------", time));
    		this.time = time;
    		this.changed = changed;
    		this.heard = heard;
    		commandIssued = false;
    		
    		MDC.put("location", location());
    		MDC.put("time", time);
    		
    		Logger.info("Heard:" + heard);
    		
    		updateVisitHistory();
    		
    		//IntArrayProperty positionHist = (IntArrayProperty)changed.getChangedProperty(getID(), "urn:rescuecore2.standard:property:positionhistory");
    		//Logger.info("History: " + positionHist + positionHist.getValue());
    		
    		hear(time, heard);
    		updateKnowledge(time, changed);
    		doThink(time, changed, heard);
    		sendMessages(time);
    		
    		lastLocationID = location().getID();	//updates last location
    		Logger.info(String.format("------- END OF TIMESTEP %d -------\n", time));
    	}
    	catch (Exception e){
    		Logger.error(("System malfunction! (exception occurred). Going 'failsafe' behavior."), e);
    		if (! commandIssued){
    			failsafe();
    		}
    	}
    }
    
    /**
     * The behavior to execute when something goes wrong with normal behavior
     */
    protected abstract void failsafe();
    
    private void updateVisitHistory(){
    	
    	lastVisit.put(location().getID(), time);	//stores that current location was visited now
    	lastVisitQueue.add(location().getID());
    	
    	
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
    		sendSpeak(time, 1, msg);	//TODO implementar aloca����o de canais
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
    			
    			updateFromMessage(b);
    			//else discards message (incoming problem is older than the one I know
    		}
    		else if(msg.msgType == MessageType.SOLVED_BLOCKED_ROAD){
    			updateFromMessage(b);
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
    			
    			updateFromMessage(h);
    			//else discards message (incoming problem is older than the one I know
    		}
    		else if(msg.msgType == MessageType.SOLVED_WOUNDED_HUMAN){
    			updateFromMessage(h);
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
    			
    			updateFromMessage(bb);
    			//else discards message (incoming problem is older than the one I know
    		}
    		else if(msg.msgType == MessageType.SOLVED_BURNING_BUILDING){
    			updateFromMessage(bb);
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
	private void updateFromMessage(BlockedRoad b) {
		if (model.getDistance(me().getID(), b.getEntityID()) < sightRange){
			Logger.debug(String.format("Road %s data received, but ignored because it's in sight range.", b.getEntityID()));
			return;
		}
		
		//checks whether I already know this problem or if the incoming problem is more recent
		if(!blockedRoads.containsKey(b) || b.getUpdateTime() > blockedRoads.get(b).getUpdateTime() ){
			blockedRoads.put(b.getEntityID(), b);
			//problemsToReport.add(b);
			Logger.info(String.format("Added %s to problems to report", b));
		}
	}
	
	private void updateFromMessage(WoundedHuman h) {
		if (model.getDistance(me().getID(), h.getEntityID()) < sightRange){
			Logger.debug(String.format("Human %s data received, but ignored because it's in sight range.", h.getEntityID()));
			return;
		}
		
		//checks whether I already know this problem or if the incoming problem is more recent
		if(!woundedHumans.containsKey(h) || h.getUpdateTime() > woundedHumans.get(h).getUpdateTime() ){
			woundedHumans.put(h.getEntityID(), h);
			//problemsToReport.add(h);
			Logger.info(String.format("Added %s to problems to report", h));
		}
	}
	
	private void updateFromMessage(BurningBuilding bb) {
		
		if (model.getDistance(me().getID(), bb.getEntityID()) < sightRange){
			Logger.debug(String.format("Building %s data received, but ignored because it's in sight range.", bb.getEntityID()));
			return;
		}
		
		//checks whether I already know this problem or if the incoming problem is more recent
		if(!burningBuildings.containsKey(bb) || bb.getUpdateTime() > burningBuildings.get(bb).getUpdateTime() ){
			//updates the model too
			/*Building b = (Building) model.getEntity(bb.getEntityID());
			b.setBrokenness(bb.brokenness);
			b.setFieryness(bb.fieryness);
			b.setTemperature(bb.temperature);
			
			
			//sanity check 
			if (! b.isBuildingAttributesDefined()) {
				Logger.error((String.format("Building %d attributes not defined!", b.getID().getValue())));
			}
			*/
			burningBuildings.put(bb.getEntityID(), bb);
			//problemsToReport.add(bb);
			Logger.info(String.format("Added %s to problems to report", bb));
			
			//Logger.info(("Updated info of bldg " + b.getID()));
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
        		
        		System.out.println("Will check " + b);
        		//Logger.info("" + b.getProperties());
        		//if building is burning, adds it to knowledge base or update its entry. 
        		//else, mark it as solved if it was on knowledge base
                if (b.isOnFire()) {
                    updateBurningBuilding(time, b);
                    Logger.info(String.format("%s burning, updating info.", b));
                }
                else {
                	//mark as solved if exists (since it is not on fire)
                	System.out.println((String.format("%s is not burning anymore.", b)));
                	if(burningBuildings.containsKey(b.getID())){
                		BurningBuilding notBurningAnymore = burningBuildings.get(b.getID());
                		notBurningAnymore.update(b.getBrokenness(), b.getFieryness(), b.getTemperature(), time);
                		notBurningAnymore.markSolved(time);
                		Logger.info(String.format("%s registered as not burning anymore.", notBurningAnymore));
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
	 * @throws Exception 
     */
    protected abstract void doThink(int time, ChangeSet changed, Collection<Command> heard) throws Exception;

    /**
       Construct a random walk starting from this agent's current location to a random building.
       @return A random walk.
    */
    protected List<EntityID> randomWalk() {
    	try{
    		return explore();
    	}
    	catch (Exception e){
    		Logger.error("Cannot use enhanced Random Walk. Going failsafe", e);
    		return failSafeRandomWalk();
    	}
    }
    
    /**
     * The random walk of sample agent
     * @return
     */
    private List<EntityID> failSafeRandomWalk() {
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
        return result;
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

            Collections.sort(possible, new LastVisitSorter(this));	//we want the most recent visit to be the last
            
            boolean found = false;
            for (EntityID next : possible) {
                StandardEntity e = model.getEntity(next);
            	
                //discards entities already in the path and burning buildings
            	if (seen.contains(next))  {
                    continue;
                }
                //discards the burning building if there are other possibilities
            	//if all possibilities are burning buildings, agent gets stuck
                if (possible.size() > 1 && e instanceof Building){
                	Building b = (Building) e;
                	if (b.isOnFire() || b.getFierynessEnum() == StandardEntityConstants.Fieryness.BURNT_OUT ) {
                		Logger.info(String.format("Ignoring %s, onFire=%s, fieryness=%s", b, b.isOnFire(), b.getFierynessEnum()));
                		continue;
                	}
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

