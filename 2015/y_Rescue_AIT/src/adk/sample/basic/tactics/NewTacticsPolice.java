package adk.sample.basic.tactics;

import adk.team.action.Action;
import adk.team.action.ActionClear;
import adk.team.action.ActionMove;
import adk.team.action.ActionRest;
import adk.team.tactics.TacticsPolice;
import adk.team.util.ImpassableSelector;
import adk.team.util.RouteSearcher;
import adk.team.util.graph.PositionUtil;
import adk.team.util.provider.ImpassableSelectorProvider;
import adk.team.util.provider.RouteSearcherProvider;
import comlib.manager.MessageManager;
import comlib.message.information.MessagePoliceForce;
import rescuecore2.config.Config;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;


/**
 * It seems that BasicTacticsPolice performs better than this one.
 * Marked as Deprecated by Anderson.
 */
@Deprecated
public abstract class NewTacticsPolice extends TacticsPolice implements RouteSearcherProvider, ImpassableSelectorProvider {

    public ImpassableSelector impassableSelector;
    public RouteSearcher routeSearcher;

    //0 -> current
    //1 -> current - 1
    public Point2D[] agentPoint;
    public boolean posInit;
    public boolean beforeMove;

    public ClearPlanner clear;
    private PointSelector points;
    private List<EntityID> clearPath;
    private List<Point2D> clearPoints;
    private int count;

    public abstract ImpassableSelector initImpassableSelector();

    public abstract RouteSearcher initRouteSearcher();

    public abstract void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager);

    @Override
    public RouteSearcher getRouteSearcher() {
        return this.routeSearcher;
    }

    @Override
    public ImpassableSelector getImpassableSelector() {
        return this.impassableSelector;
    }

    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.routeSearcher = this.initRouteSearcher();
        this.impassableSelector = this.initImpassableSelector();
        this.beforeMove = false;
        this.agentPoint = new Point2D[2];
        this.posInit = true;
        this.points = new PointSelector(this.world);
        this.clear = new ClearPlanner(this.world, this.points);
    }

    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        if (this.me.getBuriedness() > 0) {
            return this.buriednessAction(manager);
        }
        if(posInit) {
            this.posInit = false;
            this.agentPoint[0] = new Point2D(this.me.getX(), this.me.getY());
            this.agentPoint[1] = new Point2D(this.me.getX(), this.me.getY());
        }
        else {
            this.agentPoint[1] = this.agentPoint[0];
            this.agentPoint[0] = new Point2D(this.me.getX(), this.me.getY());
        }

        this.checkClearPath(currentTime);

        if(this.target == null || this.clearPath == null || this.clearPath.isEmpty()) {
            this.target = this.impassableSelector.getNewTarget(currentTime);
            if(this.target != null && this.target.getValue() == this.me.getPosition().getValue()) {
                this.impassableSelector.remove(this.target);
                this.target = this.impassableSelector.getNewTarget(currentTime);
            }
            if(this.target == null) {
                this.beforeMove = true;
                return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
            }
            this.clearPath = this.routeSearcher.getPath(currentTime, this.me(), this.target);
            if(this.clearPath == null || this.clearPath.isEmpty()) {
                this.beforeMove = true;
                return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //before agent position == this agent position && this.beforeMove
        if(this.target.getValue() != this.me().getPosition().getValue() && this.clearPoints == null) {
            if (this.beforeMove) {
                //clear
                if (PositionUtil.equalsPoint(this.agentPoint[1], this.agentPoint[0], 1000.0D)) {
                    this.beforeMove = false;
                    EntityID id = this.clearPath.get(this.clearPath.indexOf(this.me.getPosition()) + 1);
                    return this.clear.getAction(this, id);
                }
            }
            this.beforeMove = true;
            return new ActionMove(this, this.clearPath);
        }
        else {
            Point2D targetPoint = this.clearPoints.get(this.count);
            if (this.beforeMove) {
                //clear
                if (PositionUtil.equalsPoint(this.agentPoint[1], this.agentPoint[0], 1000.0D)) {
                    this.beforeMove = false;
                    return this.clear.getAction(this, targetPoint);
                }
            }
            List<EntityID> path = new ArrayList<>();
            if(this.target.getValue() != this.me().getPosition().getValue()) {
                path.add(this.me().getPosition());
            }
            path.add(this.target);
            return new ActionMove(this, path, (int)targetPoint.getX(), (int)targetPoint.getY());
        }
    }

    private void checkClearPath(int time) {
        if(this.target != null && clearPath != null && !clearPath.isEmpty()) {
            if(this.target.getValue() == this.me().getPosition().getValue()) {
                if(this.world.getEntity(this.target) instanceof Building) {
                    this.target = null;
                    this.clearPath = null;
                    this.count = 0;
                }
                else {
                    if(this.clearPoints == null || this.clearPoints.isEmpty()) {
                        this.clearPoints = this.points.getClearPoints(this.target);
                    }
                    if(PositionUtil.equalsPoint(this.agentPoint[0], this.clearPoints.get(this.count), 2000.0D)){
                        this.count++;
                    }
                    if(this.count == this.clearPoints.size()) {
                        this.target = null;
                        this.clearPath = null;
                        this.clearPoints = null;
                        this.count = 0;
                    }
                }
            }
            else {
                int oldTarget = this.target.getValue();
                this.target = this.impassableSelector.updateTarget(time, this.target);
                if(this.target != null) {
                    if (oldTarget == this.target.getValue()) {
                        int index = this.clearPath.indexOf(this.me().getPosition());
                        if (index != -1) {
                            for (int i = 0; i < index; i++) {
                                EntityID removeID = this.clearPath.remove(i);
                                this.impassableSelector.remove(removeID);
                            }
                        }
                    } else {
                        //新しい対象
                        this.clearPath = this.routeSearcher.getPath(time, this.me(), this.target);
                    }
                }
            }
        }
    }

    private Action buriednessAction(MessageManager manager) {
        this.beforeMove = false;
        manager.addSendMessage(new MessagePoliceForce(this.me, MessagePoliceForce.ACTION_REST, this.agentID));
        List<EntityID> neighbours = ((Area)this.location).getNeighbours();
        if(neighbours.isEmpty()) {
            return new ActionRest(this);
        }
        if(this.count <= 0) {
            this.count = neighbours.size();
        }
        this.count--;
        Area area = (Area)this.world.getEntity(neighbours.get(this.count));
        Vector2D vector = (new Point2D(area.getX(), area.getY())).minus(new Point2D(this.me.getX(), this.me.getY())).normalised().scale(1000000);
        return new ActionClear(this, (int) (this.me.getX() + vector.getX()), (int) (this.me.getY() + vector.getY()));
    }

    public Action oldThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);

        if (this.me.getBuriedness() > 0) {
            return this.buriednessAction(manager);
        }
        if(posInit) {
            this.posInit = false;
            this.agentPoint[0] = new Point2D(this.me.getX(), this.me.getY());
            this.agentPoint[1] = new Point2D(this.me.getX(), this.me.getY());
        }
        else {
            this.agentPoint[1] = this.agentPoint[0];
            this.agentPoint[0] = new Point2D(this.me.getX(), this.me.getY());
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //最終目的地で全エッジに向かう処理を追加すること
        //ClearMove
        //ClearArea
        //Move
        //Rest
        this.checkClearPath(currentTime);
        if(this.target == null || this.clearPath == null || this.clearPath.isEmpty()) {
            this.target = this.impassableSelector.getNewTarget(currentTime);
            if(this.target != null && this.target.getValue() == this.me.getPosition().getValue()) {
                this.impassableSelector.remove(this.target);
                this.target = this.impassableSelector.getNewTarget(currentTime);
            }
            if(this.target == null) {
                this.beforeMove = true;
                return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
            }
            this.clearPath = this.routeSearcher.getPath(currentTime, this.me(), this.target);
            if(this.clearPath == null || this.clearPath.isEmpty()) {
                this.beforeMove = true;
                return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //before agent position == this agent position && this.beforeMove
        if(this.beforeMove) {
            //clear
            if(PositionUtil.equalsPoint(this.agentPoint[1 ], this.agentPoint[0], 1000.0D)) {
                this.beforeMove = false;
                EntityID id = this.clearPath.get(this.clearPath.indexOf(this.me.getPosition()) + 1);
                return this.clear.getAction(this, id);
            }
        }
        this.beforeMove = true;
        return new ActionMove(this, this.clearPath);
        //else
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }


}
