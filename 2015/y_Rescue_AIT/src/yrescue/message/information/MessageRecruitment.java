package yrescue.message.information;

import adk.team.tactics.Tactics;
import comlib.message.MessageID;
import comlib.message.MessageMap;
import rescuecore2.worldmodel.EntityID;
import yrescue.message.information.Task;

public class MessageRecruitment extends MessageMap {
	
	public int UID_Message;
	public EntityID entityID;
	public EntityID agentID;
	public int numberAgents;
	public int x,y;
	public Task task;

	// Exemplo: 
	// MessageID = MessageID.fireBrigadeMessage
	public MessageRecruitment(int messageAgentID, Tactics<?> agent, int help, EntityID entityID, int posX, int posY, Task action){
		super(messageAgentID);
		this.agentID = agent.me().getID();
		this.entityID = entityID;
		this.numberAgents = help;
		this.x = posX;
		this.y = posY;
		this.task = action;
		this.UID_Message = Integer.parseInt(Integer.toString(agentID.getValue()) + Integer.toString(this.entityID.getValue()));
	}
	
	public MessageRecruitment(int messageAgentID, Tactics<?> agent, int help, int rawEntityID, int posX, int posY, Task action) {
		super(messageAgentID);
		this.entityID = new EntityID(rawEntityID);
		this.numberAgents = help;
		this.agentID = agent.me().getID();
		this.x = posX;
		this.y = posY;
		this.task = action;
		this.UID_Message = Integer.parseInt(Integer.toString(agentID.getValue()) + Integer.toString(this.entityID.getValue()));
	}
}
