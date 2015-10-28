package yrescue.message.information;

import java.util.Random;

import adk.team.tactics.Tactics;
import comlib.message.MessageID;
import comlib.message.MessageMap;
import rescuecore2.worldmodel.EntityID;
import yrescue.message.information.Task;

public class MessageRecruitment extends MessageMap {
	
	public int UID;
	public int positionID;
	public int agentID;
	public int numAgents;
	public Task taskType;

	// Exemplo: 
	// MessageID = MessageID.fireBrigadeMessage
	
	public MessageRecruitment(EntityID ownerAgentID, int numAgents, EntityID position, Task taskType){
		super(MessageID.recruitmentMessage);
		this.agentID = ownerAgentID.getValue();
		this.positionID = position.getValue();
		this.numAgents = numAgents;
		this.taskType = taskType;
		this.UID = createNewUID();
	}
	
	public MessageRecruitment(Tactics<?> tacticsAgent, int numAgents, int position, Task taskType) {
		super(MessageID.recruitmentMessage);
		this.agentID = tacticsAgent.getID().getValue();
		this.numAgents = numAgents;
		this.positionID = position;
		this.taskType = taskType;
		this.UID = createNewUID();
	}
	
	public MessageRecruitment(int uid, Tactics<?> tacticsAgent, int numAgents, EntityID position, Task taskType){
		super(MessageID.recruitmentMessage);
		this.agentID = tacticsAgent.getID().getValue();
		this.positionID = position.getValue();
		this.numAgents = numAgents;
		this.taskType = taskType;
		this.UID = uid;
	}
	
	public MessageRecruitment(int uid, int agentID, int numAgents, int position, int taskType){
		super(MessageID.recruitmentMessage);
		this.agentID = agentID;
		this.positionID = position;
		this.numAgents = numAgents;
		this.taskType = Task.values()[taskType -1];
		this.UID = uid;
	}
	
	private Integer createNewUID(){
		Random random = new Random();
		return random.nextInt();
		//return (int) (Integer.parseInt(Integer.toString(this.agentID) + Integer.toString(this.agentID)) + (System.currentTimeMillis() % 1000));
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Recruitment msg ");
		sb.append(this.UID);
		sb.append(" ");
		sb.append(this.agentID);
		sb.append(" ");
		sb.append(this.numAgents);
		sb.append(" ");
		sb.append(this.positionID);
		
		return sb.toString();
	}
}
