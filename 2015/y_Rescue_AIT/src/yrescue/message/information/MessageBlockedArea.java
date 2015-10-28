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
	
	public EntityID originID, destinationID;
	public int xOrigin, yOrigin;		//agent coordinates

	public MessageBlockedArea(Tactics<?> blockedAgent, EntityID origin, EntityID destination) {
		super(MessageID.blockedAreaMessage);
		this.originID = origin;
		this.destinationID = destination;
		this.xOrigin = blockedAgent.me().getX();
		this.yOrigin = blockedAgent.me().getY();
	}
	
	public MessageBlockedArea(int rawOriginID, int rawDestinationID, int x, int y) {
		super(MessageID.blockedAreaMessage);
		this.originID = new EntityID(rawOriginID);
		this.destinationID = rawDestinationID == 0 ? null : new EntityID(rawDestinationID);
		this.xOrigin = x;
		this.yOrigin = y;
	 
	}

}
