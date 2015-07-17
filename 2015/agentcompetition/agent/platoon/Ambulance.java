package agent.platoon;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import message.MessageType;
import message.RecruitmentMsgUtil;
import message.TaskType;
import problem.Recruitment;
import problem.WoundedHuman;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import statemachine.ActionStates;
import statemachine.StateMachine;
import util.DistanceSorter;
import util.WoundedHumanHPSorter;

/**
 * Ambulance agent. The main objective is to rescue, unbury and transport
 * Civilians.
 */
public class Ambulance extends AbstractPlatoon<AmbulanceTeam> {

	private final int BURRIEDNESS_TRESHOLD_RECRUITMENT = 10;
	private final int TIMEOUT_REACHING_TARGET = 10;
	private final int TIMEOUT_WAIT_RESPONSE = 5;
	private final int RECRUITMENT_LIMIT = 1;

	private Integer totalHP = 0;
	private Collection<EntityID> unexploredBuildings;
	private Integer assignedBuildingNumber = 0;
	private EntityID currentTarget = null;
	private TaskType currentTask = null;
	private RecruitmentMsgUtil recruitmentUtil = null;
	private int reachingTargetTimestep = 0;
	private int agentsRecruited = 0;
	private int timeWaitingRecruitmentResponse = 0;

	private boolean isMinimumID = false;

	protected StateMachine recruitmentStateMachine = new StateMachine(
			ActionStates.RECRUITMENT_NOTHING);

	Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
		@Override
		public Set<EntityID> createValue() {
			return new HashSet<EntityID>();
		}
	};
	
	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.CIVILIAN,
				StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE,
				StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.REFUGE,
				StandardEntityURN.HYDRANT, StandardEntityURN.GAS_STATION,
				StandardEntityURN.BUILDING);
		unexploredBuildings = new HashSet<EntityID>(buildingIDs);

		model.getBounds();

		for (Entity next : model) {
			if (next instanceof Area) {
				Collection<EntityID> areaNeighbours = ((Area) next)
						.getNeighbours();
				neighbours.get(next.getID()).addAll(areaNeighbours);
			}
		}

		StringBuilder sb = new StringBuilder();

		for (EntityID entId : neighbours.keySet()) {
			sb.append(String.valueOf(entId.getValue()));
			sb.append(" ");
		}

		System.out.println("Area ids : " + sb.toString());

		totalHP = me().getHP();
		assignedBuildingNumber = unexploredBuildings.size();

		// Define the recruitment util class with the capabilities of this agent
		List<TaskType> taskCapabilities = new LinkedList<TaskType>();
		taskCapabilities.add(TaskType.AMBULANCE_UNBURY);
		recruitmentUtil = new RecruitmentMsgUtil(taskCapabilities);

		// Hack to test the RECRUITMENT process
		/*
		 * if(ambulances.size() > 1){ int minValue = me().getID().getValue();
		 * for(StandardEntity e : ambulances){ if(e.getID().getValue() <
		 * minValue){ minValue = e.getID().getValue(); } } if(minValue ==
		 * me().getID().getValue()){ isMinimumID = true; } }
		 */
	}

	@Override
	protected void doThink(int time, ChangeSet changed,
			Collection<Command> heard) throws Exception {
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Subscribe to channel 1
			sendSubscribe(time, 1);
		}
		for (Command next : heard) {
			Logger.debug("Heard " + next);
		}

		for (Recruitment r : recruitmentMsgReceived) {
			Logger.debug("Recruitment Heard " + r.toString());
		}

		updateUnexploredBuildings(changed);

		String statusString = "HP:" + me().getHP() + " Total HP:" + totalHP
				+ " pos:" + me().getPosition() + " Damage:" + me().getDamage()
				+ " Stamina:" + me().getStamina() + " unexploredBuildings:"
				+ unexploredBuildings.size();
		System.out.println(statusString);

		// Am I transporting a civilian to a refuge?
		if (someoneOnBoard()) {
			if (location() instanceof Refuge) {
				currentTarget = null;
				Logger.info("Unloading");
				stateMachine.setState(ActionStates.Ambulance.UNLOADING);
				sendUnload(time);
				return;
			} else {
				// Move to a refuge
				List<EntityID> path = searchStrategy.shortestPath(
						me().getPosition(), refugeIDs).getPath();
				if (path != null) {
					stateMachine
							.setState(ActionStates.Ambulance.CARRYING_WOUNDED);
					Logger.info("Moving to refuge");
					sendMove(time, path);
					return;
				}
				// What do I do now? Might as well carry on and see if we can
				// dig someone else out.
				Logger.debug("Failed to plan path to refuge");
			}
		} else {
			// Sanity check
			if (stateMachine.currentState() == ActionStates.Ambulance.CARRYING_WOUNDED
					|| stateMachine.currentState() == ActionStates.Ambulance.UNLOADING) {
				currentTarget = null;
				stateMachine.setState(ActionStates.RANDOM_WALK);
			}

			// If we are not carrying anyone, and there is a civilian with no
			// burriedness at our position, load it
			if (!(location() instanceof Refuge)) {
				for (StandardEntity next : model.getObjectsInRange(
						me().getID(), sightRange)) {
					if ((next instanceof Civilian)
							&& ((Civilian) next).getPosition().equals(
									location().getID())) {

						if (((Human) next).getBuriedness() == 0
								&& !(location() instanceof Refuge)
								&& !refugeIDs.contains(((Civilian) next)
										.getPosition().getValue())) {

							stateMachine
									.setState(ActionStates.Ambulance.LOADING);
							currentTarget = next.getID();
							Logger.info("Loading now!"
									+ next
									+ "("
									+ next.getClass().getName()
									+ ")"
									+ " At pos: "
									+ ((Civilian) next).getPosition()
											.getValue());
							sendLoad(time, next.getID());
							return;
						}
					}
				}
			}
		}

		// If the agent is on a fire zone and get damaged, go to a refuge
		if (me().getDamage() >= 10) {
			List<EntityID> path = searchStrategy.shortestPath(
					me().getPosition(), refugeIDs).getPath();
			if (path != null) {
				Logger.info("Moving to refuge");
				sendMove(time, path);
				return;
			}
		}

		// Check recruitment
		processRecruitmentMessages();
		processRecruitmentTask();

		if (stateMachine.currentState() == ActionStates.Ambulance.UNBURYING) {
			Human humanTarget = ((Human) model.getEntity(currentTarget));
			if (humanTarget.getBuriedness() > 0) {
				String humanStatusString = "HP:" + humanTarget.getHP()
						+ " burriedness:" + humanTarget.getBuriedness()
						+ " Damage:" + humanTarget.getDamage();
				Logger.info("Rescueing (STATE) " + currentTarget + " "
						+ humanStatusString);
				stateMachine.setState(ActionStates.Ambulance.UNBURYING);
				sendRescue(time, humanTarget.getID());

				if (humanTarget.getBuriedness() >= BURRIEDNESS_TRESHOLD_RECRUITMENT) {
					recruitmentMsgToSend.add(new Recruitment(me().getID(), me()
							.getID(), humanTarget.getPosition(),
							TaskType.AMBULANCE_UNBURY,
							MessageType.RECRUITMENT_REQUEST, time));
				}
				return;
			} else {
				stateMachine.setState(ActionStates.Ambulance.LOADING);
				Logger.info("Loading (STATE) " + currentTarget
						+ " the civilian is at: " + humanTarget.getPosition());
				sendLoad(time, currentTarget);
				stateMachine.setState(ActionStates.Ambulance.CARRYING_WOUNDED);
				return;
			}
		}

		if (unexploredBuildings.size() > assignedBuildingNumber * 0.1) {
			Logger.info("Not enough exploration yet...");
			List<EntityID> path = searchStrategy.shortestPath(
					me().getPosition(), unexploredBuildings).getPath();
			if (path != null) {
				Logger.info("Searching buildings -10%");
				sendMove(time, path);
				return;
			}
		}

		// Go through targets (sorted by distance) and check for things we can
		// do
		List<WoundedHuman> humanList = getTargets();
		System.out.println("HumanList size: " + humanList.size());
		if (processTargets(humanList))
			return; // If processTargets is going to process something, stop
					// further processing

		// Nothing to do, check the unexplored buildings
		Logger.info("Checking unexplored buildings");
		List<EntityID> entityIDList = new ArrayList<EntityID>();
		for (EntityID subset : unexploredBuildings) {
			entityIDList.add(subset);
		}
		Collections.shuffle(entityIDList);

		if (entityIDList.size() > 0) {
			List<EntityID> path = null;
			try {
				path = searchStrategy.shortestPath(me().getPosition(),
						entityIDList).getPath();
			} catch (Exception e) {
				Logger.error("Path search exception:", e);
			}

			if (path != null) {
				stateMachine
						.setState(ActionStates.Ambulance.SEARCHING_BUILDINGS);
				Logger.info("Searching buildings");
				sendMove(time, path);
				return;
			}
		}

		// If there is no more unexplored buildings, go random
		if (stateMachine.currentState() != ActionStates.RECRUITMENT_WAITING_RESPONSE
				&& stateMachine.currentState() != ActionStates.RECRUITMENT_GOING_TO_TARGET) {
			stateMachine.setState(ActionStates.RANDOM_WALK);
		}

		Logger.info("Moving randomly");
		List<EntityID> listNodes = randomWalk();

		// Check if we are going in the same places in a loop
		if (lastVisitQueue.get(0).getValue() == listNodes.get(0).getValue()
				|| lastVisitQueue.get(1).getValue() == listNodes.get(0)
						.getValue()) {
			Logger.info("Local minima in paths where all buildings are burning, using normal random walk");
			listNodes = failSafeRandomWalk();
		}

		sendMove(time, listNodes);
	}

	@Override
	protected void failsafe() {
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			sendSubscribe(time, 1);
		}
		for (Command next : heard) {
			Logger.debug("Heard " + next);
		}
		failSafeUpdateUnexploredBuildings(changed);
		if (failSafeSomeoneOnBoard()) {
			if (location() instanceof Refuge) {
				Logger.info("Unloading");
				sendUnload(time);
				return;
			} else {
				List<EntityID> path = failSafeSearch.breadthFirstSearch(me()
						.getPosition(), refugeIDs);
				if (path != null) {
					Logger.info("Moving to refuge");
					sendMove(time, path);
					return;
				}
				Logger.debug("Failed to plan path to refuge");
			}
		}
		for (Human next : failSafeGetTargets()) {
			if (next.getPosition().equals(location().getID())) {
				if ((next instanceof Civilian) && next.getBuriedness() == 0
						&& !(location() instanceof Refuge)) {
					Logger.info("Loading " + next);
					sendLoad(time, next.getID());
					return;
				}
				if (next.getBuriedness() > 0) {
					Logger.info("Rescueing " + next);
					sendRescue(time, next.getID());
					return;
				}
			} else {
				List<EntityID> path = failSafeSearch.breadthFirstSearch(me()
						.getPosition(), next.getPosition());
				if (path != null) {
					Logger.info("Moving to target");
					sendMove(time, path);
					return;
				}
			}
		}
		List<EntityID> path = failSafeSearch.breadthFirstSearch(me()
				.getPosition(), unexploredBuildings);
		if (path != null) {
			Logger.info("Searching buildings");
			sendMove(time, path);
			return;
		}
		Logger.info("Moving randomly");
		sendMove(time, randomWalk());
	}

	/**
	 * Copied from SampleAgent. Do not change
	 * 
	 * @return
	 */
	private List<Human> failSafeGetTargets() {
		List<Human> targets = new ArrayList<Human>();
		for (StandardEntity next : model.getEntitiesOfType(
				StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE,
				StandardEntityURN.POLICE_FORCE,
				StandardEntityURN.AMBULANCE_TEAM)) {
			Human h = (Human) next;
			if (h == me()) {
				continue;
			}
			if (h.isHPDefined() && h.isBuriednessDefined()
					&& h.isDamageDefined() && h.isPositionDefined()
					&& h.getHP() > 0
					&& (h.getBuriedness() > 0 || h.getDamage() > 0)) {
				targets.add(h);
			}
		}
		Collections.sort(targets, new DistanceSorter(location(), model));
		return targets;
	}

	/**
	 * Copied from sample agent. Do not change
	 * 
	 * @return
	 */
	private boolean failSafeSomeoneOnBoard() {
		for (StandardEntity next : model
				.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			if (((Human) next).getPosition().equals(getID())) {
				Logger.debug(next + " is on board");
				return true;
			}
		}
		return false;
	}

	/**
	 * Copied from SampleAgent. Do not change
	 * 
	 * @return
	 */
	private void failSafeUpdateUnexploredBuildings(ChangeSet changed) {
		for (EntityID next : changed.getChangedEntities()) {
			unexploredBuildings.remove(next);
		}
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
	}

	/**
	 * Get a list of human targets
	 * 
	 * @return List<WoundedHuman
	 */
	private List<WoundedHuman> getTargets() {
		List<WoundedHuman> targets = new ArrayList<WoundedHuman>();
		for (StandardEntity next : model.getEntitiesOfType(
				StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE,
				StandardEntityURN.POLICE_FORCE,
				StandardEntityURN.AMBULANCE_TEAM)) {
			// System.out.println("\nHuman: " + next.getID());

			Human h = (Human) next;
			if (h == me()) {
				continue;
			}
			if (h.isHPDefined() && h.isBuriednessDefined()
					&& h.isDamageDefined() && h.isPositionDefined()
					&& h.getHP() > 0
					&& (h.getBuriedness() > 0 || h.getDamage() > 0)) {

				targets.add(new WoundedHuman(h.getID(), h.getPosition(), h
						.getBuriedness(), h.getHP(), h.getDamage(), 0));
			}
		}

		targets.addAll(woundedHumans.values());

		// Collections.sort(targets, new DistanceSorter(location(), model));
		Collections.sort(targets, new WoundedHumanHPSorter()); // TODO: replace
																// by a
																// LifeExpectancySorter
																// or something
																// alike

		return targets;
	}

	/**
	 * Tests whether the wounded human is a civilian (i.e.: is not on the lists
	 * of platoon units) TODO test because contains accept any object :(
	 * 
	 * @param next
	 * @return
	 */
	private boolean isCivilian(WoundedHuman next) {
		EntityID id = next.civilianID;
		return !(firefighters.contains(id) || policemen.contains(id) || ambulances
				.contains(id));
	}

	/**
	 * Check the recruitment messages
	 */
	public void processRecruitmentMessages() {
		recruitmentUtil.clearMessagesToSend();
		recruitmentUtil.updateMessages(super.recruitmentMsgReceived);

		// Test if we are in a state that will listen to recruitment messages
		if (stateMachine.currentState() == ActionStates.GOING_TO_TARGET
				|| stateMachine.currentState() == ActionStates.Ambulance.SEARCHING_BUILDINGS
				|| stateMachine.currentState() == ActionStates.RANDOM_WALK
				|| stateMachine.currentState() == ActionStates.RECRUITMENT_WAITING_RESPONSE) {

			if ((recruitmentUtil.hasEngage() || recruitmentUtil.hasRelease())
					&& stateMachine.currentState() == ActionStates.RECRUITMENT_WAITING_RESPONSE) {
				timeWaitingRecruitmentResponse++;

				for (Recruitment r : recruitmentUtil.getEngageMessages()) {
					if (r.getToEntityID().getValue() == me().getID().getValue()
							&& r.getTaskType() == currentTask) {
						stateMachine
								.setState(ActionStates.RECRUITMENT_GOING_TO_TARGET);
						timeWaitingRecruitmentResponse = 0;
						break;
					}
				}

				for (Recruitment r : recruitmentUtil.getReleaseMessages()) {
					if (r.getToEntityID().getValue() == me().getID().getValue()
							&& r.getTaskType() == currentTask) {
						stateMachine.setState(ActionStates.RANDOM_WALK);
						timeWaitingRecruitmentResponse = 0;
						break;
					}
				}
			}

			// Sanity check to now stay waiting forever
			if (timeWaitingRecruitmentResponse >= TIMEOUT_WAIT_RESPONSE) {
				stateMachine.setState(ActionStates.RANDOM_WALK);
				timeWaitingRecruitmentResponse = 0;
			}

			if (recruitmentUtil.hasCommit()) {
				for (Recruitment r : recruitmentUtil.getCommitMessages()) {
					if (r.getEntityID().getValue() == me().getID().getValue()
							&& r.getTaskType() == currentTask) {
						if (agentsRecruited >= RECRUITMENT_LIMIT) {
							recruitmentUtil.responseCommit(r, me().getID(),
									MessageType.RECRUITMENT_RELEASE, me()
											.getPosition(), time);
						} else {
							recruitmentUtil.responseCommit(r, me().getID(),
									MessageType.RECRUITMENT_ENGAGE, me()
											.getPosition(), time);
							agentsRecruited++;
						}
					}
				}

				agentsRecruited = 0;
			}

			if (recruitmentUtil.hasRequest()) {
				for (Recruitment r : recruitmentUtil.getRequestMessages()) {
					if (r.getToEntityID().getValue() == me().getID().getValue())
						continue;
					if (r.taskType == TaskType.AMBULANCE_UNBURY
							&& (currentTarget == null || currentTarget
									.getValue() != r.getEntityID().getValue())) {
						int d1 = 0;
						int d2 = 0;

						if (currentTarget != null) {
							d1 = model.getDistance(me().getPosition(),
									currentTarget);
							d2 = model.getDistance(me().getPosition(),
									r.getPosition());
							Logger.info("Request received d1:" + d1 + " d2:"
									+ d2);
						}

						if (d2 < d1 || currentTarget == null) {
							Logger.info("Timeout reaching recruitment target, going to normal operations");
							currentTarget = r.getPosition();
							currentTask = TaskType.AMBULANCE_UNBURY;
							stateMachine
									.setState(ActionStates.RECRUITMENT_WAITING_RESPONSE);
							recruitmentUtil.responseRequest(r, me().getID(),
									MessageType.RECRUITMENT_COMMIT, me()
											.getPosition(), time);
							break;
						}
					} else if (r.taskType == TaskType.AMBULANCE_UNBURY
							&& currentTarget.getValue() == r.getEntityID()
									.getValue()) {
						currentTask = TaskType.AMBULANCE_UNBURY;
						recruitmentUtil.responseRequest(r, me().getID(),
								MessageType.RECRUITMENT_COMMIT, me()
										.getPosition(), time);
						stateMachine
								.setState(ActionStates.RECRUITMENT_WAITING_RESPONSE);
						break;
					}
				}
			}
		}

		super.recruitmentMsgToSend = recruitmentUtil.getMessagesToSend();
	}

	/**
	 * Process the recruitment task
	 */
	private void processRecruitmentTask() {

		if (stateMachine.currentState() == ActionStates.RECRUITMENT_GOING_TO_TARGET) {
			if (me().getPosition().getValue() == currentTarget.getValue()) {
				Logger.info("Recruitment reach target, doing task!");
				stateMachine.setState(ActionStates.RECRUITMENT_DOING_TASK);
			} else if (reachingTargetTimestep >= TIMEOUT_REACHING_TARGET) {
				Logger.info("Timeout reaching recruitment target, going to normal operations");
				currentTarget = null;
				stateMachine.setState(ActionStates.RANDOM_WALK);
				reachingTargetTimestep = 0;
			} else {
				List<EntityID> path = searchStrategy.shortestPath(
						me().getPosition(), currentTarget).getPath();
				if (path != null) {
					Logger.info("Moving to recruitment target ["
							+ currentTarget.getValue() + "]");
					sendMove(time, path);
					return;
				}
				reachingTargetTimestep++;
			}
		} else if (stateMachine.currentState() == ActionStates.RECRUITMENT_DOING_TASK) {
			if (!(location() instanceof Refuge)) {
				Collection<StandardEntity> se = model.getObjectsInRange(me()
						.getID(), sightRange);
				Logger.info("All objects in sight:" + se.toArray());
				for (StandardEntity next : se) {
					if (next instanceof Human
							&& ((Human) next).getPosition().equals(
									location().getID())) {
						if ((next instanceof Civilian)
								&& ((Human) next).getBuriedness() == 0
								&& !(location() instanceof Refuge)
								&& !refugeIDs.contains(((Civilian) next)
										.getPosition().getValue())) {
							// Load
							currentTarget = next.getID();
							Logger.info("Rescuing now (Recruitment)!"
									+ next
									+ "("
									+ next.getClass().getName()
									+ ")"
									+ " At pos: "
									+ ((Civilian) next).getPosition()
											.getValue());
							stateMachine
									.setState(ActionStates.Ambulance.UNBURYING);
							sendRescue(time, next.getID());
							return;
						}
					}
				}
				List<WoundedHuman> humanList = getTargets();
				System.out.println("HumanList size: " + humanList.size());

				for (WoundedHuman next : humanList) {
					if (next.position.getValue() == me().getPosition()
							.getValue()) {
						currentTarget = next.civilianID;
						Logger.info("Rescuing now (Recruitment wounded human)!"
								+ next + "(" + next.getClass().getName() + ")"
								+ " At pos: " + next.position.getValue());
						stateMachine.setState(ActionStates.Ambulance.UNBURYING);
						sendRescue(time, next.civilianID);
						return;
					}
				}
			} else {
				stateMachine.setState(ActionStates.RANDOM_WALK);
				currentTarget = null;
			}
		}
	}

	private boolean processTargets(List<WoundedHuman> humanList) {
		for (WoundedHuman next : humanList) {
			
			Logger.info("Pos " + next.position + " My pos:"
					+ me().getPosition());

			if (burningBuildings.containsKey(next.position)) {
				Logger.info("The building is burning! Next");
				continue;
			}

			if (refugeIDs.contains(next.position)) {
				Logger.info("The Civilian is on refugee! Next");
				continue;
			}

			if (next.position.equals(location().getID())) {
				// Targets in the same place might need rescueing or loading
				Logger.info("" + next + " isCivilian? " + isCivilian(next));
				if (isCivilian(next) && next.buriedness == 0
						&& !(location() instanceof Refuge)
						&& !refugeIDs.contains(next.position)) {
					boolean civilianFound = false;
					Collection<StandardEntity> objectsInRange = model
							.getObjectsInRange(me().getID(), sightRange);
					for (StandardEntity sightEntities : objectsInRange) {
						if (sightEntities.getID().getValue() == next
								.getEntityID().getValue()) {
							civilianFound = true;
							break;
						}
					}

					if (civilianFound) {
						// Load
						currentTarget = next.getEntityID();
						stateMachine.setState(ActionStates.Ambulance.LOADING);
						Logger.info("Loading " + next + " the civilian is at: "
								+ next.position);
						sendLoad(time, next.civilianID);
						return true;
					} else {
						Logger.info("Loading STOPPED no civilian at:"
								+ next.position);
					}
				}

				if (next.buriedness > 0) {
					// Rescue
					currentTarget = next.getEntityID();
					String humanStatusString = "HP:" + next.health
							+ " burriedness:" + next.buriedness + " Damage:"
							+ next.damage;
					Logger.info("Rescueing " + next + " " + humanStatusString);
					stateMachine.setState(ActionStates.Ambulance.UNBURYING);
					sendRescue(time, next.civilianID);

					if (next.buriedness >= BURRIEDNESS_TRESHOLD_RECRUITMENT) {
						recruitmentMsgToSend.add(new Recruitment(me().getID(),
								me().getID(), next.position,
								TaskType.AMBULANCE_UNBURY,
								MessageType.RECRUITMENT_REQUEST, time));
					}
					return true;
				}
			} else {
				// Try to move to the target
				// Check if position is not a human ID TODO
				List<EntityID> path = searchStrategy.shortestPath(
						me().getPosition(), next.position).getPath();
				if (path != null) {
					currentTarget = next.getEntityID();
					stateMachine.setState(ActionStates.GOING_TO_TARGET);
					Logger.info("Moving to target");
					sendMove(time, path);
					return true;
				}
			}
		}

		return false;
	}

	private boolean someoneOnBoard() {
		for (StandardEntity next : model
				.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			if (((Human) next).getPosition().equals(getID())) {
				Logger.debug(next + " is on board");
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("Ambulance(%s)", me().getID());
	}

	private void updateUnexploredBuildings(ChangeSet changed) {
		for (EntityID next : changed.getChangedEntities()) {
			unexploredBuildings.remove(next);
		}
	}

}
