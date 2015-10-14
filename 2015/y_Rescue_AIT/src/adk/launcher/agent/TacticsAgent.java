package adk.launcher.agent;

import adk.team.action.Action;
import adk.team.action.ActionMove;
import adk.team.tactics.Tactics;
import comlib.agent.CommunicationAgent;
import comlib.manager.MessageManager;
import rescuecore2.messages.Message;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.standard.messages.AKRest;
import rescuecore2.misc.Pair;
import rescuecore2.log.Logger;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class TacticsAgent<E extends StandardEntity> extends CommunicationAgent<E> {
	
    public static final String LOS_MAX_DISTANCE_KEY = "perception.los.max-distance";
    
    protected Tactics tactics;
    protected Action action;						//current action of this agent
    protected Map<Integer, Action> commandHistory;	//history of commands
    public int ignoreTime;
    protected Pair<Integer, Integer> lastPosition;
    

    public TacticsAgent(Tactics t, boolean pre) {
        super();
        this.tactics = t;
        this.tactics.pre = pre;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void postConnect() {
        super.postConnect();
        this.ignoreTime = this.config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY);
        this.tactics.sightDistance = this.config.getIntValue(this.LOS_MAX_DISTANCE_KEY);
        this.tactics.world = this.getWorld();
        this.tactics.model = this.getWorld();
        this.tactics.agentID = this.getID();
        this.tactics.refugeList = this.getRefuges();
        this.tactics.config = this.config;
        //this.tactics.setWorldInfo(this);
        this.setAgentUniqueValue();
        this.setAgentEntity();
        this.tactics.preparation(this.config, manager);
        this.tactics.registerTacticsAgent(this);
        
        lastPosition = me().getLocation(model);
        
        this.commandHistory = new HashMap<Integer, Action>();
    }

    protected abstract void setAgentUniqueValue();

    protected abstract void setAgentEntity();

    @Override
    public void registerProvider(MessageManager manager) {
        this.tactics.registerProvider(manager);
    }
    
    @Override
    public void registerEvent(MessageManager manager) {
        this.tactics.registerEvent(manager);
    }
    
    @Override
    public void think(int time, ChangeSet changed) {
        if(time <= this.ignoreTime) {
            this.tactics.agentID = this.getID();
            this.tactics.ignoreTimeThink(time, changed, this.manager);
            return;
        }
        this.action = this.tactics.think(time, changed, this.manager);
        lastPosition = me().getLocation(model); //updates lastPosition
        Message message = this.action == null ? new AKRest(this.getID(), time) : this.action.getCommand(this.getID(), time);
        //System.out.println(message.getClass());
        System.out.println(this.action.getClass());
        this.send(message);
    }

    @Override
    public void receiveBeforeEvent(int time, ChangeSet changed) {
        this.tactics.time = time;
        this.tactics.changed = changed;
        this.tactics.config = this.config;
        //this.tactics.world = this.getWorld();
        this.tactics.startProcessTime = this.startProcessTime;
        this.setAgentEntity();
    }

    @Override
    public void sendAfterEvent(int time, ChangeSet changed) {
    	//added by Anderson: registers the current action in history
    	this.commandHistory.put(time, this.action);
    }

    @Override
    public List<Refuge> getRefuges() {
        return this.model.getEntitiesOfType(StandardEntityURN.REFUGE).stream().map(entity -> (Refuge) entity).collect(Collectors.toList());
    }

    public StandardWorldModel getWorld() {
        return this.model;
    }
    
    /**
	 * Tests whether I tried to move last time and it was not possible
	 * @return
	 */
	public boolean stuck(int time){
		int tolerance = 500;	//if agent moved less than this, will be considered as stuck
		double distance;
		
		//agents cannot issue move commands in beginning
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			return false;
		}
	
		if (commandHistory.size() == 0) {
			return false;
		}
	
		Pair<Integer, Integer> currentPos = me().getLocation(model);
		
		distance = Math.hypot(currentPos.first() - lastPosition.first(), currentPos.second() - lastPosition.second());
		
		Logger.debug("Stuckness test - distance from last position:" + distance);
	
		if (commandHistory.containsKey(time -1)) {
			Action cmd = commandHistory.get(time -1); 
		
			Logger.debug(String.format(
				"Stuckness test: last command: %s, Last position (%d, %d), Curr position (%d, %d)", 
				cmd, lastPosition.first(), lastPosition.second(), currentPos.first(), currentPos.second()
			));
		
			//if move command was issued and I traversed small distance, I'm stuck
			if ( (cmd instanceof ActionMove) && (distance  < tolerance)) {
					Logger.info("Dammit, I'm stuck!");
					return true;
			}
		}
		Logger.info("Not stuck!");
		return false;
	}

    
}


	