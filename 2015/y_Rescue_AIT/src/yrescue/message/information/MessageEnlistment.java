package yrescue.message.information;

import adk.team.tactics.Tactics;
import comlib.message.MessageID;
import comlib.message.MessageMap;
import rescuecore2.worldmodel.EntityID;
import yrescue.message.information.Task;

public class MessageEnlistment extends MessageMap {
	
	public int UID_Message;
	public int UID_Message_Origin;
	public EntityID agentID;
	public float utility;

	// Exemplo: 
	// MessageID = MessageID.fireBrigadeMessage
	public MessageEnlistment(int messageAgentID, int UID_Message, Tactics<?> agent, float utility){
		// Tem que gerar o UID para a mensagem
		super(messageAgentID);
		this.UID_Message_Origin = UID_Message;
		this.agentID = agent.me().getID();
		this.utility = utility;
		this.UID_Message = Integer.parseInt(Integer.toString(this.agentID.getValue()) + Integer.toString(this.UID_Message_Origin));
	}
}
