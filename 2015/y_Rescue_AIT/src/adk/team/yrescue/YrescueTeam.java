package adk.team.yrescue;

import adk.team.Team;
import adk.team.precompute.*;
import adk.team.tactics.TacticsAmbulance;
import adk.team.tactics.TacticsFire;
import adk.team.tactics.TacticsPolice;
import adk.team.yrescue.tactics.YrescueTacticsAmbulance;
import adk.team.yrescue.tactics.YrescueTacticsFire;
import adk.team.yrescue.tactics.YrescueTacticsPolice;

public class YrescueTeam extends Team {

    @Override
    public String getTeamName() {
        return "Yrescue";
    }

    @Override
    public TacticsAmbulance getAmbulanceTeamTactics() {
        return new YrescueTacticsAmbulance();
    }

    @Override
    public TacticsFire getFireBrigadeTactics() {
        return new YrescueTacticsFire();
    }

    @Override
    public TacticsPolice getPoliceForceTactics() {
        return new YrescueTacticsPolice();
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
