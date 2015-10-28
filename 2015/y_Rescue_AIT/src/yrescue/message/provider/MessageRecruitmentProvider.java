package yrescue.message.provider;

import comlib.event.MessageEvent;
import comlib.manager.RadioConfig;
import comlib.manager.VoiceConfig;
import comlib.message.information.MessageRoad;
import comlib.provider.MapMessageProvider;
import comlib.util.BitOutputStream;
import comlib.util.BitStreamReader;
import yrescue.message.event.MessageRecruitmentEvent;
import yrescue.message.information.MessageRecruitment;

public class MessageRecruitmentProvider extends MapMessageProvider<MessageRecruitment, MessageRecruitmentEvent>{
	
	public MessageRecruitmentProvider(int id){
		super(id);
	}

	protected void writeMessage(RadioConfig config, BitOutputStream bos, MessageRecruitment msg) {
		super.writeMessage(config, bos, msg);
		
		bos.writeBits(msg.UID, 32);
		bos.writeBits(msg.agentID, 32);
		bos.writeBits(msg.numAgents, 32);
		bos.writeBits(msg.positionID, 32);
		bos.writeBits(msg.taskType.getValue(), 32);
	}

	protected void writeMessage(VoiceConfig config, StringBuilder sb, MessageRoad msg)
	{
		//config.appendData(sb, String.valueOf(msg.getValue()));
	}

	protected MessageRecruitment createMessage(RadioConfig config, int time, BitStreamReader bsr) {
		int uid = bsr.getBits(32);
		int ownerId = bsr.getBits(32);
		int numAgents = bsr.getBits(32);
		int position = bsr.getBits(32);
		int taskType = bsr.getBits(32);
		
		return new MessageRecruitment(
			uid,
			ownerId,
			numAgents,
			position,
			taskType
		);
	}

	protected MessageRecruitment createMessage(VoiceConfig config, int time, int ttl, String[] data, int next)
	{
		return null;
	}

	@Override
	public Class<? extends MessageEvent> getEventClass() {
		return MessageRecruitmentEvent.class;
	}


}
