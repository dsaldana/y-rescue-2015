package yworld;

import java.util.Map;

import rescuecore2.log.Logger;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;

/**
 * Estimates fire progression on YBuildings
 */
public class YFireSimulator {
	
	//some coefficients
	public static float GAMMA = 0.2f;//0.5f; (Kobe2013)
    public static float AIR_TO_AIR_COEFFICIENT=0.5f;
    public static float AIR_TO_BUILDING_COEFFICIENT=45f;
    public static float WATER_COEFFICIENT = 20f; //0.5f; (Kobe2013)
    public static float ENERGY_LOSS=0.9f;
    public static float WIND_DIRECTION=0.9f;
    public static float WIND_RANDOM=0f;
    public static int   WIND_SPEED=0;
    public static float RADIATION_COEFFICENT=1.0f;
    public static float TIME_STEP_LENGTH=1f;
    public static float WEIGHT_GRID = 0.2f;
    public static float AIR_CELL_HEAT_CAPACITY = 1f;

    StandardWorldModel world;
    Map<EntityID, YBuilding> yBuildings;
    
    public YFireSimulator(StandardWorldModel world, Map<EntityID, YBuilding> yBuildings){
    	this.world = world;
    	this.yBuildings = yBuildings;
    }
    
    public void step(){
    	//refill();
        //executeExtinguishRequests();
        burn();
        cool();
        burnNeighborhood();
        //exchangeBuilding();
        //FIXED
        cool();
    }
    
    public void stepOneBuilding(YBuilding build){
    	//refill();
        //executeExtinguishRequests();
        burn(build);
        cool(build);
        burnNeighborhood(build);
        //exchangeBuilding();
        //FIXED
        cool(build);
    }

	private void burnNeighborhood() {
		for (YBuilding yb : yBuildings.values()){
			//TODO callibrate influence range
			yb.heatFromNeighborhood(world, 20000, yBuildings); //20000 corresponds to 20 meters
		}
		
	}
	
	private void burnNeighborhood(YBuilding yb) {
		yb.heatFromNeighborhood(world, 20000, yBuildings); //20000 corresponds to 20 meters		
	}
	
	/**
	 * Returns the YBuilding, given its ID
	 * @param id
	 * @return
	 */
	public YBuilding getYBuilding(EntityID id){
		return yBuildings.get(id);
	}

	private void cool() {
		for (YBuilding yb : yBuildings.values()){
			yb.cool(WATER_COEFFICIENT, GAMMA);
		}
		
	}
	
	private void cool(YBuilding yb) {
		yb.cool(WATER_COEFFICIENT, GAMMA);
	}

	private void burn() {
		for (YBuilding yb : yBuildings.values()){
			yb.burnStep();
		}
		
	}
	
	private void burn(YBuilding yb) {
		yb.burnStep();		
	}
}
