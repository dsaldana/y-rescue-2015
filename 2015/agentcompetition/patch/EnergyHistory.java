/**
 * Replace: <rescue_dir>/oldsims/firesimulator/simulator/EnergyHistory.java
 */

package firesimulator.simulator;

import java.util.Map;
import java.util.HashMap;

import firesimulator.world.World;
import firesimulator.world.Building;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class EnergyHistory {
    private static final Log LOG = LogFactory.getLog(EnergyHistory.class);
    private static final Log REGRESSION_LOG = LogFactory.getLog("regression");
    
    private int time;
    private Map<Building, Double> initialEnergy;
    private Map<Building, Integer> initialFieryness;
    private Map<Building, Double> initialTemperature;
    private Map<Building, Double> burnEnergy;
    private Map<Building, Double> coolEnergy;
    private Map<Building, Double> exchangedWithAir;
    private Map<Building, Double> lostToRadiation;
    private Map<Building, Double> gainedByRadiation;
    private Map<Building, Double> finalEnergy;
    private Map<Building, Double> finalTemperature;
    private Map<Building, Integer> finalFieryness;

    public EnergyHistory(World world, int time) {
        initialEnergy = new HashMap<Building, Double>();
        initialTemperature = new HashMap<Building, Double>();
        initialFieryness = new HashMap<Building,Integer>();
        burnEnergy = new HashMap<Building, Double>();
        coolEnergy = new HashMap<Building, Double>();
        exchangedWithAir = new HashMap<Building, Double>();
        lostToRadiation = new HashMap<Building, Double>();
        gainedByRadiation = new HashMap<Building, Double>();
        finalEnergy = new HashMap<Building, Double>();
        finalTemperature = new HashMap<Building, Double>();
        finalFieryness = new HashMap<Building, Integer>();
        this.time = time;
        for (Building next : world.getBuildings()) {
            initialEnergy.put(next, next.getEnergy());
            initialTemperature.put(next, next.getTemperature());
            initialFieryness.put(next, next.getFieryness());
        }
    }

    public void registerBurn(Building b, double energy) {
        burnEnergy.put(b, energy);
    }

    public void registerCool(Building b, double energy) {
        coolEnergy.put(b, energy);
    }

    public void registerAir(Building b, double energy) {
        exchangedWithAir.put(b, energy);
    }

    public void registerRadiationLoss(Building b, double energy) {
        lostToRadiation.put(b, energy);
    }

    public void registerRadiationGain(Building b, double energy) {
        double old = gainedByRadiation.containsKey(b) ? gainedByRadiation.get(b) : 0;
        gainedByRadiation.put(b, old + energy);
    }

    public void registerFinalEnergy(World world) {
        for (Building next : world.getBuildings()) {
            finalEnergy.put(next, next.getEnergy());
            finalTemperature.put(next, next.getTemperature());
            finalFieryness.put(next, next.getFieryness());
        }
    }

    public void logSummary() {
        LOG.debug("Energy summary at time " + time);
        for (Building next : initialEnergy.keySet()) {
            boolean changed = burnEnergy.containsKey(next) || coolEnergy.containsKey(next) || exchangedWithAir.containsKey(next) || lostToRadiation.containsKey(next) || gainedByRadiation.containsKey(next);
            if (changed && !initialEnergy.get(next).equals(finalEnergy.get(next))) {
                LOG.debug("Building " + next.getID());
                LOG.debug("  Initial energy / temperature: " + initialEnergy.get(next) + " / " + initialTemperature.get(next));
                LOG.debug("  Burn energy                 : " + burnEnergy.get(next));
                LOG.debug("  Cool energy                 : " + coolEnergy.get(next));
                LOG.debug("  Exchanged with air          : " + exchangedWithAir.get(next));
                LOG.debug("  Lost to radiation           : " + lostToRadiation.get(next));
                LOG.debug("  Gained by radiation         : " + gainedByRadiation.get(next));
                LOG.debug("  Final energy / temperature  : " + finalEnergy.get(next) + " / " + finalTemperature.get(next));
                LOG.debug("  Initial fieryness           : " + initialFieryness.get(next));
                LOG.debug("  Final fieryness             : " + finalFieryness.get(next));
                LOG.debug("  Initial temperature         : " + Math.round(initialTemperature.get(next)));
                LOG.debug("  Final temperature           : " + Math.round(finalTemperature.get(next)));
                LOG.debug("  Code (material)             : " + next.getCode());
                LOG.debug("  Ground area                 : " + next.getBuildingAreaGround());
                LOG.debug("  Floors                      : " + next.getFloors());
                LOG.debug("  Total area                  : " + next.getBuildingAreaGround()*next.getFloors());
            }
            /**
             * format: time,buildingID,groundArea,floors,totalArea,initialTemperature,finalTemperature,initialFieryness,finalFieryness
             */
            String regString = String.format(
        		"%d,%d,%d,%d,%d,%d,%d,%d,%d", 
        		time, next.getID(), Math.round(next.getBuildingAreaGround()), next.getFloors(),
        		Math.round(next.getBuildingAreaGround()*next.getFloors()),
        		Math.round(initialTemperature.get(next)), Math.round(finalTemperature.get(next)),
        		initialFieryness.get(next), finalFieryness.get(next)
        	);
            		
            REGRESSION_LOG.debug(regString);
        }
    }
}