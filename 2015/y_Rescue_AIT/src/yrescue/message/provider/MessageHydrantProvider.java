package yrescue.message.provider;

import comlib.event.MessageEvent;
import comlib.manager.RadioConfig;
import comlib.manager.VoiceConfig;
import comlib.message.information.MessageRoad;
import comlib.provider.MapMessageProvider;
import comlib.util.BitOutputStream;
import comlib.util.BitStreamReader;
import yrescue.message.event.MessageBlockedAreaEvent;
import yrescue.message.event.MessageHydrantEvent;
import yrescue.message.information.MessageHydrant;

public class MessageHydrantProvider extends MapMessageProvider<MessageHydrant, MessageHydrantEvent>{
	

	public MessageHydrantProvider(int id){
		super(id);
	}

	protected void writeMessage(RadioConfig config, BitOutputStream bos, MessageHydrant msg)
	{
		super.writeMessage(config, bos, msg);
		
		bos.writeBits(msg.hydrantID.getValue(), 32);
		bos.writeBits(msg.agentID.getValue(), 32);
		bos.writeBits(msg.timestep_free, 32);
	}

	protected void writeMessage(VoiceConfig config, StringBuilder sb, MessageRoad msg)
	{
		//config.appendData(sb, String.valueOf(msg.getValue()));
	}

	protected MessageHydrant createMessage(RadioConfig config, int time, BitStreamReader bsr)
	{
		return new MessageHydrant(
			bsr.getBits(32),
			bsr.getBits(32),
			bsr.getBits(32)
		);
	}

	protected MessageHydrant createMessage(VoiceConfig config, int time, int ttl, String[] data, int next)
	{
		return null;
		// return new MessageCivilian(time, ttl, Integer.parseInt(data[next]));
	}

//	@Override
//	public void trySetEvent(MessageRoadEvent ev) {
//		if (ev instanceof MessageRoadEvent) { this.event = ev; }
//
//	}

	@Override
	public Class<? extends MessageEvent> getEventClass() {
		return MessageBlockedAreaEvent.class;
	}


}
