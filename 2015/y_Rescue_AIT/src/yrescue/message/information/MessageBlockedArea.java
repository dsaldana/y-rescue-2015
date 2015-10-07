package yrescue.message.information;

import adk.team.tactics.Tactics;
import adk.team.util.provider.WorldProvider;
import comlib.message.MessageID;
import comlib.message.MessageInformation;
import comlib.message.MessageMap;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

public class MessageBlockedArea extends MessageMap {
	
	public EntityID roadID;
	public int x, y;		//agent coordinates

	public MessageBlockedArea(Tactics<?> blockedAgent, EntityID locationID) {
		super(MessageID.blockedAreaMessage);
		this.roadID = locationID;
		this.x = blockedAgent.me().getX();
		this.y = blockedAgent.me().getY();
	}
	
	public MessageBlockedArea(int rawRoadID, int x, int y) {
		super(MessageID.blockedAreaMessage);
		this.roadID = new EntityID(rawRoadID);
		this.x = x;
		this.y = y;
	 
	}

}
