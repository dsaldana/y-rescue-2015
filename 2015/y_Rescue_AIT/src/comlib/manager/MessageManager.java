package comlib.manager;

import comlib.provider.MessageDummyProvider;
import comlib.provider.MessageProvider;
import comlib.event.MessageEvent;
import comlib.message.CommunicationMessage;
import comlib.message.MessageID;
import comlib.provider.information.*;
import comlib.provider.topdown.*;
import comlib.util.BitOutputStream;
import comlib.util.BitStreamReader;
import rescuecore2.Constants;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.messages.Message;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;
import yrescue.message.provider.MessageBlockedAreaProvider;
import yrescue.message.provider.MessageEnlistmentProvider;
import yrescue.message.provider.MessageHydrantProvider;
import yrescue.message.provider.MessageRecruitmentProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class MessageManager {
	private final int PRIORITY_DEPTH = 16;
	private final int NORMAL_PRIORITY = 8;

	private boolean developerMode;

	private RadioConfig radioConfig;
	private VoiceConfig voiceConfig;

	private boolean useRadio;
	private int numRadio;
	private int numVoice;

	private int kernelTime;

	private MessageProvider[] providerList;
	private List<MessageEvent> eventList;
	//	private MessageEvent[] eventList;

	private List<CommunicationMessage> receivedMessages; // FOR-COMPATIBLE
	private List<CommunicationMessage> sendMessages;
	private BitOutputStream[] bitOutputStreamList;
	private int[] maxBandWidthList;

	private EntityID agentID;

	private boolean heardAgentHelp = false;

	private int getBitOutputStreamNumber(int priority, int kind) {
		//Logger.trace(String.format("getBitOutputStreamNumber(priority=%d,kind=%d)", priority, kind));
		//Logger.trace("getBitOutputStreamNumber=" + ((PRIORITY_DEPTH * kind) + priority));
		return (PRIORITY_DEPTH * kind) + priority;
	}

	public MessageManager(Config config, EntityID agentID) {
		this.init(config);
		this.agentID = agentID;
	}

	private void init(Config config) {
		this.developerMode = false;
			//config.getBooleanValue("comlib.develop.developerMode", false);
		this.radioConfig = new RadioConfig(config);
		this.voiceConfig = new VoiceConfig(config);
		this.kernelTime = -1;

		this.numRadio = config.getIntValue("comms.channels.max.platoon");
		this.numVoice =
			((config.getValue("comms.channels.0.type").equals("voice")) ? 1 : 0);
		this.useRadio = ( this.numRadio >= 1 );

		this.providerList =
			new MessageProvider[32];//[config.getIntValue("comlib.default.messageID", 32)];
		
		
		//BUGFIX? priority and kind were inverted...
		this.bitOutputStreamList = new BitOutputStream[this.getBitOutputStreamNumber(
			PRIORITY_DEPTH,
			31//config.getIntValue("comlib.default.messageID", 32) -1
		)];
		
		Logger.trace("bitOutputStreamList.length=" + bitOutputStreamList.length);
		
		this.maxBandWidthList = new int[numRadio];
		this.eventList = new ArrayList<>();
		this.receivedMessages = new ArrayList<>();
		this.sendMessages = new ArrayList<>();

		for(int bosl = 0; bosl < this.bitOutputStreamList.length; bosl++) { 
			this.bitOutputStreamList[bosl] = new BitOutputStream(); 
		}

		for (int ch = 1; ch <= numRadio; ch++) {
			maxBandWidthList[ch -1] =
				config.getIntValue("comms.channels." + ch + ".bandwidth");
		}

		this.initLoadProvider();
	}

	public boolean canUseRadio() {
		return this.useRadio;
	}

	public RadioConfig getRadioConfig() {
		return this.radioConfig;
	}

	public VoiceConfig getVoiceConfig() {
		return this.voiceConfig;
	}

	public int getTime() {
		return this.kernelTime;
	}

	public int getMaxBandWidth(int ch) {
		return this.maxBandWidthList[ch - 1];
	}

	public boolean isHeardAgentHelp() {
		return this.heardAgentHelp;
	}

	public void receiveMessage(int time, Collection<Command> heard)	{
		this.kernelTime = time;
		this.receivedMessages.clear();
		this.heardAgentHelp = false;

		Logger.trace("\n--- BEGIN: receiving messages ---- ");
		Logger.trace("heard: " + heard);
		for (BitOutputStream bos : bitOutputStreamList) {
			bos.reset();
		}

		for (Command command : heard) {
			if (command instanceof AKSpeak) {
				if (agentID == command.getAgentID()) { 
					Logger.trace("Ignoring message 'cuz I'm the sender");
					continue; 
				}

				byte[] data = ((AKSpeak)command).getContent();
				if (data.length <= 0) {
					Logger.trace("Ignoring message 'cuz it has no data");
					continue; 
				}

				if (((AKSpeak) command).getChannel() == 0) {
					String voice = new String(data);
					if ("Help".equalsIgnoreCase(voice) || "Ouch".equalsIgnoreCase(voice)) {
						//System.out.println(voice + " : " + command.getAgentID() + " : " );
						this.heardAgentHelp = true;
						Logger.trace("I heard Help or Ouch.");
						continue;
					}
					String[] voiceData =
						voice.split(this.voiceConfig.getMessageSeparator());
					this.receiveVoiceMessage(
						(AKSpeak)command,
						Arrays.copyOfRange(voiceData, 1, voiceData.length - 1),
						this.receivedMessages
					);
					// TODO: refactoring
				}
				else {
					Logger.trace("Will process radio messages!");
					this.receiveRadioMessage((AKSpeak)command, this.receivedMessages);
				}
			}
		}
		Logger.trace("--- END: receiving messages ---- \n");
	}

	private void receiveRadioMessage(AKSpeak akSpeak, List<CommunicationMessage> list)	{
		if (akSpeak.getContent() == null || list == null) {
			Logger.trace("Ignoring radio message 'cuz it has no content or my list is null");
			return;
		}
		BitStreamReader bsr = new BitStreamReader(akSpeak.getContent());
		Logger.trace("SizeOfMsgID: " + this.radioConfig.getSizeOfMessageID());
		int msgID = bsr.getBits(this.radioConfig.getSizeOfMessageID());
		//MessageProvider provider = this.providerList[bsr.getBits(this.radioConfig.getSizeOfMessageID())];
		Logger.trace("Received MessageID: " + msgID);
		MessageProvider provider = this.providerList[msgID];
		//Logger.trace("MessageProvider:" + provider);
		
		//		System.out.println("MSGID: " + msgID);
		int lastRemainBufferSize = bsr.getRemainBuffer();
		while(bsr.getRemainBuffer() > 0) {
			try {
				CommunicationMessage msg = provider.create(this, bsr, akSpeak.getAgentID());
				list.add(msg);
				//Logger.trace("Added message " + msg + " to list ");
			} catch(Exception e) {
				Logger.error("Error receiving message!");
				//System.err.println("Received message is corrupt or format is different.");
				//e.printStackTrace();
				return;
			}

			// TODO: Check!!
			if (bsr.getRemainBuffer() == lastRemainBufferSize) {
				Logger.debug(String.format(
					"bsr.getRemainBuffer(): %d // lastRemainBufferSize: %d, breaking",
					bsr.getRemainBuffer(), lastRemainBufferSize
				));
				break;
			} else {
				lastRemainBufferSize = bsr.getRemainBuffer();
			}
			//Logger.trace("Processed MSG : " + msgID + ", RemainBuf : " + bsr.getRemainBuffer());
			//System.out.println("MSG : " + msgID + ", RemainBuf : " + bsr.getRemainBuffer());
		}
		Logger.debug("MessageList: " + list);
	}

	// TODO: refactoring
	private void receiveVoiceMessage(AKSpeak akSpeak, String[] data, List<CommunicationMessage> list) {
		if (data == null || (data.length & 0x01) == 1 || list == null) {
			return;
		}
		for (int count = 0; count < data.length; count += 2) {
			int id = Integer.parseInt(data[count]);
			String[] messageData = data[count + 1].split(this.voiceConfig.getDataSeparator());
			list.add(this.providerList[id].create(this, messageData, akSpeak.getAgentID()));
		}
	}

	public List<Message> createSendMessage(EntityID agentID) {
		List<Message> messages = new ArrayList<Message>();

		int bosNum = 0;
		boolean isFirstLoop = true;
		Logger.trace(String.format("Will send messages. providerList.length=%d, bosList.length=%d", providerList.length, bitOutputStreamList.length));
		for (int ch = 1; ch <= numRadio; ch++) {
			int sentMessageSize = 0;

			// for (; bosNum < bitOutputStreamList.length; bosNum++)
			// {
			// 	BitOutputStream bos = bitOutputStreamList[bosNum];
			// 	if (bos.size() <= 0)
			// 	{ continue; }
			// 	if ((sentMessageSize + bos.size()) > getMaxBandWidth(ch))
			// 	{ continue; }
			// 	sentMessageSize += bos.size();
			// 	messages.add(
			// 			new AKSpeak(agentID, this.getTime(), ch, bos.toByteArray()));
			// }
			for (int priority = 0; priority < PRIORITY_DEPTH; priority++) {
				for (int kind = 0; kind < providerList.length; kind++) {
					BitOutputStream bos = bitOutputStreamList[this.getBitOutputStreamNumber(priority, kind)];
					if (bos.size() <= 0) {
						continue;
					}
					if ((sentMessageSize + bos.size()) > getMaxBandWidth(ch)) {
						Logger.warn("Message does not fit in bandwidth");
						continue;
					}
					Logger.trace("msg content: " + bos.toByteArray());
					sentMessageSize += bos.size();
					messages.add(new AKSpeak(agentID, this.getTime(), ch, bos.toByteArray()));
					Logger.trace("Added message to queue. Msg size: " + bos.size());
				}
			}

			if (ch == numRadio && isFirstLoop) {
				isFirstLoop = false;
				ch = 1;
				bosNum = 0;
			}
			//			System.out.println("@MSG:" + sentMessageSize);
		}

		//		messages.add(new AKSpeak(agentID, this.getTime(), 1, ("TEST").getBytes()));
		// StringBuilder sb = new StringBuilder();
		// for (CommunicationMessage msg : this.sendMessages)
		// { this.providerList[msg.getMessageID()].write(this, sb, msg); }
		return messages;
	}

	public List<CommunicationMessage> getReceivedMessage() // FOR-COMPATIBLE
	{
		return this.receivedMessages;
	}

	public <M extends CommunicationMessage> void addSendMessage(M msg, int priority)
	{
		if (priority < 0 || PRIORITY_DEPTH <= priority)
		{ throw new IllegalArgumentException(); }
		
		this.sendMessages.add(msg);
		int msgID = msg.getMessageID();
				
		Logger.trace("Adding message to 'queue' with ID: " + msgID + " with priority " + priority);
		// TODO: need cutting data
		this.providerList[msgID].write(this, bitOutputStreamList[msgID], msg);
	}

	public <M extends CommunicationMessage> void addSendMessage(M msg)
	{
		this.addSendMessage(msg, NORMAL_PRIORITY);
	}

	// public void old_addSendMessage(CommunicationMessage msg)
	// {
	// 	this.sendMessages.add(msg);
	// }

	public void addVoiceSendMessage(CommunicationMessage msg)
	{
		// TODO: NFCのリストを用意して．．．いろいろ
		this.sendMessages.add(msg);
	}

	private void initLoadProvider()
	{
		// TODO: Load provider
		this.registerStandardProvider(
				new MessageDummyProvider(MessageID.dummyMessage));
		this.registerStandardProvider(
				new MessageCivilianProvider(MessageID.civilianMessage));
		this.registerStandardProvider(
				new MessageFireBrigadeProvider(MessageID.fireBrigadeMessage));
		this.registerStandardProvider(
				new MessagePoliceForceProvider(MessageID.policeForceMessage));
		this.registerStandardProvider(
				new MessageAmbulanceTeamProvider(MessageID.ambulanceTeamMessage));
		this.registerStandardProvider(
				new MessageBuildingProvider(MessageID.buildingMessage));
		this.registerStandardProvider(
				new MessageRoadProvider(MessageID.roadMessage));
		this.registerStandardProvider(
				new MessageReportProvider(MessageID.reportMessage));
		this.registerStandardProvider(
				new CommandPoliceProvider(MessageID.policeCommand));
		this.registerStandardProvider(
				new CommandAmbulanceProvider(MessageID.ambulanceCommand));
		this.registerStandardProvider(
				new CommandFireProvider(MessageID.fireCommand));
		this.registerStandardProvider(
				new CommandScoutProvider(MessageID.scoutCommand));
		
		//adding MessageBlockedArea
		this.registerStandardProvider(
			new MessageBlockedAreaProvider(MessageID.blockedAreaMessage)
		);
		
		this.registerStandardProvider(
			new MessageHydrantProvider(MessageID.hydrantMessage)
		);
		
		this.registerStandardProvider(
			new MessageRecruitmentProvider(MessageID.recruitmentMessage)
		);
		this.registerStandardProvider(
			new MessageEnlistmentProvider(MessageID.enlistmentMessage)
		);
		//this.register(CommunicationMessage.buildingMessageID, new MessageBuildingProvider(this.event));
		//this.register(CommunicationMessage.blockadeMessageID, new BlockadeMessageProvider(this.event));
		//this.register(CommunicationMessage.victimMessageID,   new VictimMessageProvider());
		//this.register(CommunicationMessage.positionMessageID, new PositionMessageProvider(this.event));
	}

	private void registerStandardProvider(MessageProvider provider)	{
		this.providerList[provider.getMessageID()] = provider;
	}

	public boolean registerProvider(MessageProvider provider) {
		int messageID = provider.getMessageID();
		if (!this.developerMode || this.kernelTime != -1 || provider == null || messageID < 0) {
			return false;
		}

		if (messageID >= this.providerList.length) {
			this.providerList = Arrays.copyOf(this.providerList, messageID +1);
			// this.bitOutputStreamList =
			// 	Arrays.copyOf(this.bitOutputStreamList,
			// 			this.getBitOutputStreamNumber(messageID -1, PRIORITY_DEPTH)
			// 			);

			//TODO: refactoring
			for (int bosl = 0; bosl < this.bitOutputStreamList.length; bosl++) {
				this.bitOutputStreamList[bosl] = new BitOutputStream();
			}
		}
		else if (this.providerList[messageID] != null) {
			return false;
		}

		this.registerStandardProvider(provider);
		this.radioConfig.updateMessageIDSize(messageID);
		this.searchEvent(this.providerList[messageID]);
		return true;
	}

	public boolean registerEvent(MessageEvent event)
	{
		if (event == null)
		{ return false; }
		this.eventList.add(event);
		this.searchProvider(event);
		return true;
	}

	private void searchProvider(MessageEvent event)
	{
		for (MessageProvider provider : this.providerList) {
			if (provider != null) {
				provider.trySetEvent(event);
			}
		}
	}

	private void searchEvent(MessageProvider provider)
	{
		// if (this.eventList.size() < 1)
		// {	return; }

		for (MessageEvent event : this.eventList) {
			provider.trySetEvent(event);
		}
	}
}
