package adk.team;

import adk.team.control.ControlAmbulance;
import adk.team.control.ControlFire;
import adk.team.control.ControlPolice;
import adk.team.precompute.*;
import adk.team.tactics.TacticsAmbulance;
import adk.team.tactics.TacticsFire;
import adk.team.tactics.TacticsPolice;
import yrescue.control.YRescueControlAmbulance;
import yrescue.control.YRescueControlFire;
import yrescue.control.YRescueControlPolice;

public abstract class Team {
	public abstract String getTeamName();

	public abstract TacticsAmbulance getAmbulanceTeamTactics();

	public abstract TacticsFire getFireBrigadeTactics();

	public abstract TacticsPolice getPoliceForceTactics();

	//control

	public ControlAmbulance getAmbulanceCentreControl() {
		return new YRescueControlAmbulance();
	}

	public ControlFire getFireStationControl() {
		return new YRescueControlFire();
	}

	public ControlPolice getPoliceOfficeControl() {
		return new YRescueControlPolice();
	}

	public abstract PrecomputeAmbulance getAmbulancePrecompute();

	public abstract PrecomputeFire getFirePrecompute();

	public abstract PrecomputePolice getPolicePrecompute();
}
