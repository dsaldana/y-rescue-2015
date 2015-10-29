package yrescue.message.recruitment;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import comlib.manager.MessageManager;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import yrescue.message.information.MessageEnlistment;
import yrescue.message.information.MessageRecruitment;
import yrescue.message.information.Task;

public class RecruitmentManager {
	
	private List<MessageEnlistment> receivedMsgEnlistment;
	private List<MessageRecruitment> receivedMsgRecruitment;
	private List<MessageEnlistment> sendedMsgEnlistment;
	private MessageRecruitment sendedMsgRecruitment;
	private MessageRecruitment choosedMsgRecruitment;
	private MessageManager msgManager;
	private StandardWorldModel world;
	
	private List<Task> capableTasks; 
	private int time = 0;
	private EntityID owner;
	private RecruitmentStates currentState = RecruitmentStates.NOTHING;
	
	public static enum RecruitmentStates {
		RECRUITING(1),
		ENLISTING(2),
		NOTHING(3);
		
		private int value;

		RecruitmentStates(int val) {
	        this.value = val;
	    }

	    public int getValue() {
	        return this.value;
	    }
	}
	
	public RecruitmentManager(){
		this.receivedMsgEnlistment = new LinkedList<MessageEnlistment>();
		this.receivedMsgRecruitment = new LinkedList<MessageRecruitment>();
		this.sendedMsgEnlistment = new LinkedList<MessageEnlistment>();
		this.capableTasks = new LinkedList<Task>();
	}
	
	public void initRecruitmentManager(EntityID owner, List<Task> availableTasks, MessageManager msgManager, StandardWorldModel world){
		this.owner = owner;
		if(availableTasks != null) this.capableTasks = availableTasks;
		this.msgManager = msgManager;
		this.world = world;
	}
	
	public void setMessageManager(MessageManager msgManager){
		this.msgManager = msgManager;
	}
	
	public void clearMessages(){
		clearSendedRecruitmentMessage();
		clearSendedEnlistmentMessages();
		clearReceivedRecruitmentMessages();
		clearReceivedEnlistmentMessages();
		clearChoosedRecruitmentMessage();
	}
	
	public void clearSendedRecruitmentMessage(){
		this.sendedMsgRecruitment = null;
	}
	
	public void clearSendedEnlistmentMessages(){
		this.sendedMsgEnlistment.clear();
	}
	
	public void clearReceivedRecruitmentMessages(){
		this.receivedMsgRecruitment.clear();
	}
	
	public void clearReceivedEnlistmentMessages(){
		this.receivedMsgEnlistment.clear();
	}
	
	public void clearChoosedRecruitmentMessage(){
		this.choosedMsgRecruitment = null;
	}
	
	public void addReceivedRecruitmentMessage(MessageRecruitment msg){
		if(capableTasks.contains(msg.taskType) && msg.agentID != this.owner.getValue()){
			this.receivedMsgRecruitment.add(msg);
		}
	}
	
	public void addReceivedEnlistmentMessage(MessageEnlistment msg){
		boolean found = false;
		if(sendedMsgRecruitment.getUID() == msg.getOriginUID()){
			found = true;
		}
		
		if(!found){
			for(MessageEnlistment mr : sendedMsgEnlistment){
				if(mr.getOriginUID() == msg.getOriginUID()){
					found = true;
					break;
				}
			}
		}
		
		if(found){
			this.receivedMsgEnlistment.add(msg);
		}
	}
	
	private void addSendedRecruitmentMessage(MessageRecruitment msg){
		this.msgManager.addSendMessage(msg);
		this.sendedMsgRecruitment = msg;
	}
	
	private void addSendedEnlistmentMessage(MessageEnlistment msg){
		this.msgManager.addSendMessage(msg);
		this.sendedMsgEnlistment.add(msg);
	}
	
	public boolean createNewRecruitmentMsg(Task taskType, int numAgents, int time){
		if(numAgents < 1 || numAgents > 10) return false;
		if(taskType == null) return false;
		if(!capableTasks.contains(taskType)) return false;
		
		this.time = time;
		
		addSendedRecruitmentMessage(new MessageRecruitment(owner, numAgents, ((Human) world.getEntity(owner)).getPosition(), taskType));
		return true;
	}
	
	public boolean createNewEnlistmentMsg(MessageRecruitment recruitmentMsg, float utility, int time){
		if(recruitmentMsg == null) return false;
		
		this.time = time;
		this.choosedMsgRecruitment = recruitmentMsg;
		
		addSendedEnlistmentMessage(new MessageEnlistment(recruitmentMsg.getUID(), this.owner.getValue(), utility));
		return true;
	}
	
	public boolean isMyEnlistmentSelected(){
		if(this.choosedMsgRecruitment == null) return false;
		boolean found = false;
		
		List<MessageEnlistment> msgEnlistment = new LinkedList<MessageEnlistment>();
		
		for(MessageEnlistment mr : receivedMsgEnlistment){
			if(mr.getOriginUID() == this.choosedMsgRecruitment.getUID()){
				msgEnlistment.add(mr);
			}
		}
		
		msgEnlistment.sort(new Comparator<MessageEnlistment>() {
			@Override
			public int compare(MessageEnlistment o1, MessageEnlistment o2) {
				int comp = Float.compare(o1.getUtility(), o2.getUtility());
				if(comp != 0) return comp;
				else{
					return Integer.compare(o1.getAgentID(), o2.getAgentID());
				}
			}
		});
		
		if(msgEnlistment.size() >= choosedMsgRecruitment.getNumAgents()){
			List<MessageEnlistment> subList = msgEnlistment.subList(0, choosedMsgRecruitment.getNumAgents()-1);
			for(MessageEnlistment mr : subList){
				if(mr.getAgentID() == this.owner.getValue()){
					found = true;
					break;
				}
			}
		}
		else{
			found = true;
		}
		
		return found;
	}
	
	public boolean isRecruitmentAvailable(){
		return !this.receivedMsgEnlistment.isEmpty();
	}
	
	public MessageRecruitment getNearestRecruitment(){
		if(this.receivedMsgRecruitment == null || this.receivedMsgRecruitment.isEmpty()) return null;
		
		this.receivedMsgRecruitment.sort(new Comparator<MessageRecruitment>() {
			@Override
			public int compare(MessageRecruitment o1, MessageRecruitment o2) {
				int dist1 = world.getDistance(((Human) world.getEntity(owner)).getPosition(), new EntityID(o1.getAgentID()));
				int dist2 = world.getDistance(((Human) world.getEntity(owner)).getPosition(), new EntityID(o2.getAgentID()));
				
				return Integer.compare(dist1, dist2);
			}
		});
		
		return this.receivedMsgRecruitment.get(0);
	}
	
	public boolean isAnyoneEnlisted(){
		if(sendedMsgRecruitment == null) return false;
		boolean found = false;
		
		for(MessageEnlistment mr : sendedMsgEnlistment){
			if(mr.getOriginUID() == sendedMsgRecruitment.getUID()){
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	public boolean isEnlistmentAvailable(){
		if(this.choosedMsgRecruitment == null) return false;
		boolean found = false;
		
		for(MessageEnlistment mr : receivedMsgEnlistment){
			if(mr.getOriginUID() == this.choosedMsgRecruitment.getUID()){
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	public int getAgentsEnlisted(){
		if(sendedMsgRecruitment == null) return 0;
		int agentsFound = 0;
		
		for(MessageEnlistment mr : sendedMsgEnlistment){
			if(mr.getOriginUID() == sendedMsgRecruitment.getUID()){
				agentsFound++;
			}
		}
		
		return agentsFound;
	}
	
	public void setRecruitmentState(RecruitmentStates state){
		if(state != null) this.currentState = state;
	}
	
	public RecruitmentStates getRecruitmentState(){
		return this.currentState;
	}
	
}
