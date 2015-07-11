package message;

import java.util.LinkedList;
import java.util.List;

import org.junit.runner.Request;

import problem.Recruitment;
import rescuecore2.worldmodel.EntityID;

public class RecruitmentMsgUtil {

	private List<TaskType> validTasks;
	
	private List<Recruitment> requestList;
	private List<Recruitment> commitList;
	private List<Recruitment> releaseList;
	private List<Recruitment> engageList;
	private List<Recruitment> timeoutList;
	
	private List<Recruitment> sendMessages;
	
	/**
	 * Constructor
	 * @param validTasks is a list of possible TaskType 's that this agent can perform
	 */
	public RecruitmentMsgUtil(List<TaskType> validTasks) {
		this.validTasks = validTasks;
		
		this.requestList = new LinkedList<Recruitment>();
		this.commitList = new LinkedList<Recruitment>();
		this.releaseList = new LinkedList<Recruitment>();
		this.engageList = new LinkedList<Recruitment>();
		this.timeoutList = new LinkedList<Recruitment>();
		
		this.sendMessages = new LinkedList<Recruitment>();
	}
	
	/**
	 * Update the internal messages given a list of Recruitment objects
	 * @param recruitmentMessages
	 */
	public void updateMessages(List<Recruitment> recruitmentMessages){
		// Clear the old data
		this.requestList.clear(); 
		this.commitList.clear();
		this.releaseList.clear();
		this.engageList.clear();
		this.timeoutList.clear();
		
		// Update the new data
		for(Recruitment r : recruitmentMessages){
			if(r == null) continue;
			if(!this.validTasks.contains(r.getTaskType())) continue;
			if(r.getMessageType() == MessageType.RECRUITMENT_COMMIT){
				this.commitList.add(r);
			}
			else if(r.getMessageType() == MessageType.RECRUITMENT_ENGAGE){
				this.engageList.add(r);
			}
			else if(r.getMessageType() == MessageType.RECRUITMENT_RELEASE){
				this.releaseList.add(r);
			}
			else if(r.getMessageType() == MessageType.RECRUITMENT_REQUEST){
				this.requestList.add(r);
			}
			else if(r.getMessageType() == MessageType.RECRUITMENT_TIMEOUT){
				this.timeoutList.add(r);
			}
		}
	}
	
	public boolean hasRequest(){
		return this.requestList.size() > 0 ? true : false;
	}
	
	public List<Recruitment> getRequestMessages(){
		return this.requestList;
	}
	
	public boolean hasCommit(){
		return commitList.size() > 0 ? true : false;
	}
	
	public List<Recruitment> getCommitMessages(){
		return this.commitList;
	}
	
	public boolean hasRelease(){
		return releaseList.size() > 0 ? true : false;
	}
	
	public List<Recruitment> getReleaseMessages(){
		return this.releaseList;
	}
	
	public boolean hasEngage(){
		return engageList.size() > 0 ? true : false;
	}
	
	public List<Recruitment> getEngageMessages(){
		return this.engageList;
	}
	
	public boolean hasTimeout(){
		return timeoutList.size() > 0 ? true : false;
	}
	
	public List<Recruitment> getTimeoutMessages(){
		return this.timeoutList;
	}
	
	/**
	 * Store a response for a request message
	 * 
	 * @param originalMessage
	 * @param responseEntityID
	 * @param response
	 * @param responsePosition
	 * @param time
	 * @return
	 */
	public boolean responseRequest(Recruitment originalMessage, EntityID responseEntityID, MessageType response, EntityID responsePosition, int time){
		Recruitment r = new Recruitment(originalMessage.getEntityID(), responseEntityID, responsePosition, originalMessage.getTaskType(), response, time);
		return this.sendMessages.add(r);
	}
	
	/**
	 * Store a response for a commit message
	 * 
	 * @param originalMessage
	 * @param responseEntityID
	 * @param response
	 * @param responsePosition
	 * @param time
	 * @return
	 */
	public boolean responseCommit(Recruitment originalMessage, EntityID responseEntityID, MessageType response, EntityID responsePosition, int time){
		Recruitment r = new Recruitment(originalMessage.getEntityID(), responseEntityID, responsePosition, originalMessage.getTaskType(), response, time);
		return this.sendMessages.add(r);
	}
	
	/**
	 * Clear the messages to send list
	 */
	public void clearMessagesToSend(){
		this.sendMessages.clear();
	}
	
	/**
	 * Get the list of messages to send
	 * @return
	 */
	public List<Recruitment> getMessagesToSend(){
		return this.sendMessages;
	}
	
}
