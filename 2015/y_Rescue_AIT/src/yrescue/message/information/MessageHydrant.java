package yrescue.message.information;

import adk.team.tactics.Tactics;
import comlib.message.MessageID;
import comlib.message.MessageMap;
import rescuecore2.worldmodel.EntityID;

public class MessageHydrant extends MessageMap {
	
	public EntityID hydrantID;
	public EntityID agentID;
	public int timestep_free;

	/**
	 * 
	 * @param fireBrigadeAgent
	 * @param currentTime
	 * @param waterOnTank
	 * @param rate
	 * @param maximumWater
	 * @param locationID
	 */
	public MessageHydrant(Tactics<?> fireBrigadeAgent, int currentTime, int waterOnTank, int rate, int maximumWater, EntityID locationID) {
		super(MessageID.hydrantMessage); 
		this.agentID = fireBrigadeAgent.me().getID();
		this.hydrantID = locationID;
		this.timestep_free = currentTime + (maximumWater-waterOnTank)/rate; 	//time in which the hydrant will be free again
	}
	
	public MessageHydrant(int rawAgentID, int rawHydrantID,  int timesteps) {
		super(MessageID.hydrantMessage);
		this.agentID = new EntityID(rawAgentID);
		this.hydrantID = new EntityID(rawHydrantID);
		this.timestep_free = timesteps;
	}
	
	@Override
	public String toString(){
		return String.format("MessageHydrant (hydrantID=%s, agentID=%s, timestep_free=%s)", hydrantID, agentID, timestep_free);
	}
	
}
