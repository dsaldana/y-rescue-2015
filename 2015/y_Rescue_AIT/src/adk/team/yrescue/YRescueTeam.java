package adk.team.yrescue;

import adk.team.Team;
import adk.team.precompute.*;
import adk.team.tactics.TacticsAmbulance;
import adk.team.tactics.TacticsFire;
import adk.team.tactics.TacticsPolice;
import yrescue.precompute.YRescuePrecomputeAmbulance;
import yrescue.precompute.YRescuePrecomputeFire;
import yrescue.precompute.YRescuePrecomputePolice;
import yrescue.tactics.YRescueTacticsAmbulance;
import yrescue.tactics.YRescueTacticsFire;
import yrescue.tactics.YRescueTacticsPolice;

public class YRescueTeam extends Team {

    @Override
    public String getTeamName() {
        return "Y-Rescue";
    }

    @Override
    public TacticsAmbulance getAmbulanceTeamTactics() {
        return new YRescueTacticsAmbulance();
    }

    @Override
    public TacticsFire getFireBrigadeTactics() {
        return new YRescueTacticsFire();
    }

    @Override
    public TacticsPolice getPoliceForceTactics() {
        return new YRescueTacticsPolice();
    }

    @Override
    public PrecomputeAmbulance getAmbulancePrecompute() {
        return new YRescuePrecomputeAmbulance();
    }

    @Override
    public PrecomputeFire getFirePrecompute() {
        return new YRescuePrecomputeFire();
    }

    @Override
    public PrecomputePolice getPolicePrecompute() {
        return new YRescuePrecomputePolice();
    }

}
