package comlib.message;


public interface MessageID
{
	int dummyMessage = 15;

	int civilianMessage = 11;
	int fireBrigadeMessage = 12;
	int policeForceMessage = 2;
	int ambulanceTeamMessage = 3;

	int buildingMessage = 4;
	int roadMessage = 1;
	int blockedAreaMessage = 5;		//message for reporting a blocked agent

	int reportMessage = 6;
	int policeCommand = 7;
	int ambulanceCommand = 8;
	int fireCommand = 9;
	int scoutCommand = 10;
	
	int hydrantMessage = 16;		//message for reporting busy hydrant
	int recruitmentMessage = 17;	//message for requesting for help
	int enlistmentMessage = 18;		//message for offering help
}
