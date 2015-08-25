package adk.team;

import adk.team.yrescue.control.YrescueControlAmbulance;
import adk.team.yrescue.control.YrescueControlFire;
import adk.team.yrescue.control.YrescueControlPolice;
import adk.team.control.ControlAmbulance;
import adk.team.control.ControlFire;
import adk.team.control.ControlPolice;
import adk.team.precompute.*;
import adk.team.tactics.TacticsAmbulance;
import adk.team.tactics.TacticsFire;
import adk.team.tactics.TacticsPolice;

public abstract class Team {
	public abstract String getTeamName();

	public abstract TacticsAmbulance getAmbulanceTeamTactics();

	public abstract TacticsFire getFireBrigadeTactics();

	public abstract TacticsPolice getPoliceForceTactics();

	//control

	public ControlAmbulance getAmbulanceCentreControl() {
		return new YrescueControlAmbulance();
	}

	public ControlFire getFireStationControl() {
		return new YrescueControlFire();
	}

	public ControlPolice getPoliceOfficeControl() {
		return new YrescueControlPolice();
	}

	public abstract PrecomputeAmbulance getAmbulancePrecompute();

	public abstract PrecomputeFire getFirePrecompute();

	public abstract PrecomputePolice getPolicePrecompute();
}
