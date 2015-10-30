package comlib.manager;

import comlib.util.IntegerDataHelper;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;

public class RadioConfig {
    private int channel;

    private int sizeOfMessageID;

    private int sizeOfTime;

	public RadioConfig(Config config) {
		this.channel = config.getIntValue("comlib.message.channel", 1);
		this.sizeOfTime = IntegerDataHelper.getBitSize(31); //config.getIntValue("comlib.size.time", 9);
		this.updateMessageIDSize(31);//(config.getIntValue("comlib.message.messageID", 32) - 1);
		
		Logger.debug("-------- BEGIN: COMM PARAMETERS ---------");
		Logger.debug(String.format(
			"channel: %d, sizeOfTime: %d, sizeOfMessageID: %d",
			channel, sizeOfTime, sizeOfMessageID
		));
		Logger.debug("-------- END: COMM PARAMETERS ---------");
	}

    public void updateMessageIDSize(int id) {
        int size = IntegerDataHelper.getBitSize(id);
        if (size > this.sizeOfMessageID){
            this.sizeOfMessageID = size;
        }
    }

    public int getChannel() { return this.channel; }

    public int getSizeOfMessageID() { return this.sizeOfMessageID; }

    public int getSizeOfTime() { return this.sizeOfTime; }

    public int getSizeOfDummyValue() { return 32; }

    public int getSizeOfHumanHP() { return 32; }
    public int getSizeOfHumanBuriedness() { return 32; }
    public int getSizeOfHumanDamage() { return 32; }
    public int getSizeOfHumanPosition() { return 32; }

    public int getSizeOfCivilianID() { return 32; }

    public int getSizeOfAmbulanceTeamID() { return 32; }

    public int getSizeOfFireBrigadeID() { return 32; }
    public int getSizeOfFireBrigadeWater() { return 32; }

    public int getSizeOfPoliceForceID() { return 32; }

    public int getSizeOfBuildingID() { return 32; }
    public int getSizeOfBuildingBrokenness() { return 32; }
    public int getSizeOfBuildingFieryness() { return 32; }
    public int getSizeOfBuildingTemperature() { return 32; }

    /*
		public int getSizeOfEntityID(Class<? extends Building> c) {

        return c != null ? 32 : 0;
    }

    public int getSizeOfEntityID(Class<? extends Civilian> c) {
        return c != null ? 32 : 0;
    }

    public int getSizeOfEntityID(Class<? extends FireBrigade> c)
    {
        return 32;
    }

    public int getSizeOfEntityID(Class<? extends GasStation> c)
    {
        return 32;
    }

    public int getSizeOfEntityID(Class<? extends PoliceForce> c)
    {
        return 32;
    }

    public int getSizeOfEntityID(Class<? extends Refuge> c)
    {
        return 32;
    }

    public int getSizeOfEntityID(Class<? extends Road> c)
    {
        return 32;
    }

    public int getSizeOfHumanHP() {
        return 32;
    }

    public int getSizeOfHumanBuriedness() {
        return 32;
    }

    public int getSizeOfHumanDamage() {
        return 32;
    }

    public int getSizeOfPosition() {
        return 32;
    }*/

}
