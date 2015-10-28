package yrescue.message.information;

import java.util.Random;

import adk.team.tactics.Tactics;
import comlib.message.MessageID;
import comlib.message.MessageMap;
import rescuecore2.worldmodel.EntityID;
import yrescue.message.information.Task;

public class MessageEnlistment extends MessageMap {
	
	public int UID;
	public int originUID;
	public int agentID;
	public float utility;

	public MessageEnlistment(int uidFrom, Tactics<?> agent, float utility){
		super(MessageID.enlistmentMessage);
		
		this.UID = createNewUID();
		this.originUID = uidFrom;
		this.agentID = agent.getID().getValue();
		this.utility = utility;
	}
	
	public MessageEnlistment(int uid, int uidFrom, int agentID, float utility){
		super(MessageID.enlistmentMessage);
		
		this.UID = uid;
		this.originUID = uidFrom;
		this.agentID = agentID;
		this.utility = utility;
	}
	
	public MessageEnlistment(int uid, int uidFrom, Tactics<?> agent, float utility){
		super(MessageID.enlistmentMessage);
		
		this.UID = uid;
		this.originUID = uidFrom;
		this.agentID = agent.getID().getValue();
		this.utility = utility;
	}
	
	private Integer createNewUID(){
		Random random = new Random();
		return random.nextInt();
		//return (int) (Integer.parseInt(Integer.toString(this.agentID) + Integer.toString(this.agentID)) + (System.currentTimeMillis() % 1000));
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Enlistment msg ");
		sb.append(this.UID);
		sb.append(" ");
		sb.append(this.originUID);
		sb.append(" ");
		sb.append(this.agentID);
		sb.append(" ");
		sb.append(this.utility);
		
		return sb.toString();
	}
}
