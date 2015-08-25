package adk.team.yrescue;

import adk.team.Team;
import adk.team.precompute.*;
import adk.team.tactics.TacticsAmbulance;
import adk.team.tactics.TacticsFire;
import adk.team.tactics.TacticsPolice;
import adk.team.yrescue.tactics.YRescueTacticsAmbulance;
import adk.team.yrescue.tactics.YRescueTacticsFire;
import adk.team.yrescue.tactics.YRescueTacticsPolice;

public class YRescueTeam extends Team {

    @Override
    public String getTeamName() {
        return "Yrescue";
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
        return null;
    }

    @Override
    public PrecomputeFire getFirePrecompute() {
        return null;
    }

    @Override
    public PrecomputePolice getPolicePrecompute() {
        return null;
    }

}
