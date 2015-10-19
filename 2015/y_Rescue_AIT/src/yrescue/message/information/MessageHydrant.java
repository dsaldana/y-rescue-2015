package yrescue.message.information;

import adk.team.tactics.Tactics;
import comlib.message.MessageID;
import comlib.message.MessageMap;
import rescuecore2.worldmodel.EntityID;

public class MessageHydrant extends MessageMap {
	
	public EntityID hydrantID;
	EntityID agentID;
	int timesteps;

	public MessageHydrant(Tactics<?> fireBrigadeAgent, int currentTime, int waterOnTank, int rate, int maximumWater, EntityID locationID) {
		super(MessageID.fireBrigadeMessage); 
		this.agentID = fireBrigadeAgent.me().getID();
		this.hydrantID = locationID;
		this.timesteps = (maximumWater-waterOnTank)/rate;
	}
	
	public MessageHydrant(Tactics<?> fireBrigadeAgent, int currentTime,  int waterOnTank, int rate, int maximumWater, int rawHydrantID) {
		super(MessageID.fireBrigadeMessage);
		this.agentID = fireBrigadeAgent.me().getID();
		this.hydrantID = new EntityID(rawHydrantID);
		this.timesteps = (maximumWater-waterOnTank)/rate;
	}
}
