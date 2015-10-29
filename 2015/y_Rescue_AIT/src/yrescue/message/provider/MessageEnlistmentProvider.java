package yrescue.message.provider;

import adk.team.tactics.Tactics;
import comlib.event.MessageEvent;
import comlib.manager.RadioConfig;
import comlib.manager.VoiceConfig;
import comlib.message.information.MessageRoad;
import comlib.provider.MapMessageProvider;
import comlib.util.BitOutputStream;
import comlib.util.BitStreamReader;
import yrescue.message.event.MessageEnlistmentEvent;
import yrescue.message.event.MessageRecruitmentEvent;
import yrescue.message.information.MessageEnlistment;
import yrescue.message.information.MessageRecruitment;

public class MessageEnlistmentProvider extends MapMessageProvider<MessageEnlistment, MessageEnlistmentEvent>{
	
	public MessageEnlistmentProvider(int id){
		super(id);
	}

	protected void writeMessage(RadioConfig config, BitOutputStream bos, MessageEnlistment msg) {
		super.writeMessage(config, bos, msg);
		
		//int uid, int uidFrom, Tactics<?> agent, float utility
		
		bos.writeBits(msg.UID, 32);
		bos.writeBits(msg.originUID, 32);
		bos.writeBits(msg.agentID, 32);
		bos.writeBits(msg.utility, 32);
	}

	protected void writeMessage(VoiceConfig config, StringBuilder sb, MessageRoad msg)
	{
		//config.appendData(sb, String.valueOf(msg.getValue()));
	}

	protected MessageEnlistment createMessage(RadioConfig config, int time, BitStreamReader bsr) {
		int uid = bsr.getBits(32);
		int originUID = bsr.getBits(32);
		int agentID = bsr.getBits(32);
		float utility = Float.intBitsToFloat(bsr.getBits(32));
		
		return new MessageEnlistment(
			uid,
			originUID,
			agentID,
			utility
		);
	}

	protected MessageEnlistment createMessage(VoiceConfig config, int time, int ttl, String[] data, int next)
	{
		return null;
	}

	@Override
	public Class<? extends MessageEvent> getEventClass() {
		return MessageEnlistmentEvent.class;
	}


}
