package yrescue.heatmap;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class HeatMap {
	protected Map<EntityID, HeatNode> heatNodeMap = null;
	public boolean DEBUG = false;
	private EntityID ownerID = null;
	private StandardWorldModel worldModel = null;
	private String defaultFileName = null;
	
	private EntityID lastFrom = null;
	private int lastTime = 0;
	
	private EntityID explorationTarget = null;
	
	public HeatMap(EntityID ownerID, StandardWorldModel worldModel){
		this.heatNodeMap = new HashMap<EntityID, HeatNode>();
		this.ownerID = ownerID;
		this.worldModel = worldModel;
		
		this.defaultFileName = "/tmp/heatmap_" + this.ownerID.getValue() + ".txt";
	}
	
	/**
	 * Update the heat of a node by time
	 * @param from
	 * @param time
	 * @return
	 */
	public boolean updateNode(EntityID from, Integer time){
		this.lastFrom = from;
		this.lastTime = time;
		
		if(from.equals(explorationTarget)){
			Logger.info("HeatMap exploration target " + explorationTarget + " reached!");
			explorationTarget = null;
		}
		
		HeatNode hn = heatNodeMap.get(from);
		if(hn != null){
			hn.updateHeat(from, time);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Get the next entityID to visit. It will update the heat map and select the one with less heat and closer
	 * @return
	 */
	public EntityID getNodeToVisit(){
		updateHeatMap(this.lastFrom, this.lastTime);
		if(explorationTarget == null) {
			List<HeatNode> heatNodes = new ArrayList<HeatNode>(this.heatNodeMap.values());
			heatNodes = orderHeatNodeList(heatNodes);
			explorationTarget = heatNodes.get(0).getEntity();
			Logger.debug("New HeatMap exploration target chosen: " + explorationTarget);
		}
		return explorationTarget;
	}
	
	/**
	 * Add a new entity to the heat map
	 * @param entity
	 * @param priority
	 * @param timeStep
	 */
	public void addEntityID(EntityID entity, HeatNode.PriorityLevel priority, int timeStep){
		this.heatNodeMap.put(entity, new HeatNode(entity, priority, timeStep));
	}
	
	/**
	 * Update the heat of all the nodes
	 * @param from
	 * @param time
	 */
	private void updateHeatMap(EntityID from, Integer time){
		for (Iterator<EntityID> it = heatNodeMap.keySet().iterator(); it.hasNext();) {
			HeatNode hn = heatNodeMap.get(it.next());
			hn.updateHeat(from, time);
		}
	}
	
	/**
	 * Write the current heat map the a file
	 * The format of the space separated file is:
	 * 'entity_id centroid_x centroid_y edge_number edge1x edge1y edge2x edge2y ... edgeNx edgeNy current_heat'
	 */
	public void writeMapToFile(){
		writeMapToFile(this.defaultFileName);
	}
	
	/**
	 * Write the current heat map to an specified file
	 * @param filePath
	 */
	public void writeMapToFile(String filePath){
		PrintWriter writer;
		try {
			writer = new PrintWriter(filePath, "UTF-8");
			
			for (Iterator<EntityID> it = heatNodeMap.keySet().iterator(); it.hasNext();) {
				HeatNode hn = heatNodeMap.get(it.next());
				
				Area area0 = (Area) this.worldModel.getEntity(hn.getEntity());
				
				List<Edge> edgeList = area0.getEdges();
				StringBuilder strBuilder = new StringBuilder();
				strBuilder.append(hn.getEntity().getValue());
				strBuilder.append(" ");
				strBuilder.append(area0.getX());
				strBuilder.append(" ");
				strBuilder.append(area0.getY());
				strBuilder.append(" ");
				strBuilder.append(edgeList.size());
				strBuilder.append(" ");
				for(Edge e : edgeList){
					strBuilder.append(e.getStartX());
					strBuilder.append(" ");
					strBuilder.append(e.getStartY());
					strBuilder.append(" ");
				}
				strBuilder.append(hn.getHeat());
				writer.println(strBuilder.toString());
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Order the node list of the map to find the next node to visit.
	 * It selects first by heat level (less heat first) and then order by distance (closest ones first)
	 * @param nodes
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<HeatNode> orderHeatNodeList(List<HeatNode> nodes) {

		Collections.sort(nodes, new Comparator() {

			public int compare(Object o1, Object o2) {

				Float x1 = ((HeatNode) o1).getHeat();
				Float x2 = ((HeatNode) o2).getHeat();
				int sComp = x1.compareTo(x2);

				if (sComp != 0) {
					return sComp;
				} else {
					int compPriority = Integer.compare(((HeatNode) o1).getPriorityLevel().getValue(), ((HeatNode) o2).getPriorityLevel().getValue());
					if(compPriority != 0){
						return compPriority;
					}
					else{
						Integer d1 = worldModel.getDistance(((HeatNode) o1).getEntity(), lastFrom);
						Integer d2 = worldModel.getDistance(((HeatNode) o2).getEntity(), lastFrom);
						return d1.compareTo(d2);
					}
				}
			}
		});

		return nodes;
	}
}
