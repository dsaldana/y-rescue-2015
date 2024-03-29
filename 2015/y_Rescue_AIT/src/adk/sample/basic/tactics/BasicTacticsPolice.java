package adk.sample.basic.tactics;

import adk.team.action.*;
import adk.team.tactics.TacticsPolice;
import adk.team.util.ImpassableSelector;
import adk.team.util.RouteSearcher;
import adk.team.util.graph.PositionUtil;
import adk.team.util.provider.ImpassableSelectorProvider;
import adk.team.util.provider.RouteSearcherProvider;

import com.google.common.collect.Lists;

import comlib.manager.MessageManager;
import comlib.message.information.MessagePoliceForce;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import java.awt.geom.Line2D;
import java.util.*;

public abstract class BasicTacticsPolice extends TacticsPolice implements RouteSearcherProvider, ImpassableSelectorProvider {

    public ImpassableSelector impassableSelector;

    public RouteSearcher routeSearcher;

    public Point2D[] agentPoint;
    public boolean beforeMove;
    public Map<EntityID, List<List<Edge>>> neighbourEdgesMap;
    public Map<EntityID, List<Point2D>> passablePointMap;
    public Map<EntityID, List<Point2D>> clearListMap;
    public Point2D mainTargetPoint;
    public int count;

    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.routeSearcher = this.initRouteSearcher();
        this.impassableSelector = this.initImpassableSelector();
        this.agentPoint = new Point2D[2];
        this.beforeMove = false;
        this.neighbourEdgesMap = new HashMap<>();
        this.passablePointMap = new HashMap<>();
        this.clearListMap = new HashMap<>();
        this.beforeMove = false;
        this.count = -1;
    }

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
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        this.agentPoint[1] = this.agentPoint[0];
        this.agentPoint[0] = new Point2D(this.me.getX(), this.me.getY());
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //状態の確認
        //埋まっている場合，周辺の道路の瓦礫の除去
        if(this.me.getBuriedness() > 0) {
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
            Vector2D vector = (new Point2D(area.getX(), area.getY())).minus(this.agentPoint[0]).normalised().scale(1000000);
            return new ActionClear(this, (int) (this.me.getX() + vector.getX()), (int) (this.me.getY() + vector.getY()));
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //動ける
        //mainTargetPointと現在の地点が誤差含め同じか
        //同じならばoldTargetのデータから地点の削除
        if(this.mainTargetPoint != null) {
            if(PositionUtil.equalsPoint(this.agentPoint[0], this.mainTargetPoint, 2000.0D)) {
                this.removeTargetPoint((Road)this.world.getEntity(this.target), this.mainTargetPoint);
                this.mainTargetPoint = null;
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //対象の選択または，対象の変更
        //対象が存在しない場合，noTargetMove
        int oldTarget = 0;
        if(this.target != null) {
            oldTarget = this.target.getValue();
            this.target = this.impassableSelector.updateTarget(currentTime, this.target);
        }
        else {
            this.target = this.impassableSelector.getNewTarget(currentTime);
        }
        if(this.target == null) {
            this.beforeMove = true;
            return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if(this.beforeMove && (PositionUtil.equalsPoint(this.agentPoint[0], this.agentPoint[1], 2.0D))) {
            this.beforeMove = false;
            if(this.location instanceof Building) {
                List<EntityID> neighbours = ((Area)this.location).getNeighbours();
                if(neighbours.isEmpty()) {
                    return new ActionRest(this);
                }
                if(this.count <= 0 || this.count > neighbours.size()) {
                    this.count = neighbours.size();
                }
                this.count--;
                Area area = (Area)this.world.getEntity(neighbours.get(this.count));
                Vector2D vector = (new Point2D(area.getX(), area.getY())).minus(this.agentPoint[0]).normalised().scale(1000000);
                return new ActionClear(this, (int) (this.me.getX() + vector.getX()), (int) (this.me.getY() + vector.getY()));
            }
            List<EntityID> path = this.routeSearcher.getPath(currentTime, this.me, this.target);
            if(path == null || path.size() < 2) {
                path = this.routeSearcher.noTargetMove(currentTime, this.me);
            }
            if (path != null && path.size() >= 2) {
                Road road = (Road) this.location;
                Vector2D vector = this.getVector(this.agentPoint[0], PositionUtil.getEdgePoint(road.getEdgeTo(path.get(1))), road);//Edgeが取得できていない
                return new ActionClear(this, (int) (this.me.getX() + vector.getX()), (int) (this.me.getY() + vector.getY()));
            }
            else {
                Road road = (Road) this.location;
                List<EntityID> neighbours = road.getNeighbours();
                if (neighbours.isEmpty()) {
                    return new ActionRest(this);
                }
                if (this.count <= 0 || this.count > neighbours.size()) {
                    this.count = neighbours.size();
                }
                this.count--;
                Vector2D vector = this.getVector(this.agentPoint[0], PositionUtil.getEdgePoint(road.getEdgeTo(neighbours.get(1))), road);
                return new ActionClear(this, (int) (this.me.getX() + vector.getX()), (int) (this.me.getY() + vector.getY()));
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if(this.location.getID().getValue() != this.target.getValue()) {
            /*if(this.location instanceof Building) {
                this.beforeMove = true;
                List<EntityID> path = this.routeSearcher.getPath(currentTime, this.me, this.target);
                return new ActionMove(this, path != null ? path : this.routeSearcher.noTargetMove(currentTime));
            }
            else if(this.passable((Road)this.location)) {
                this.beforeMove = true;
                List<EntityID> path = this.routeSearcher.getPath(currentTime, this.me, this.target);
                return new ActionMove(this, path != null ? path : this.routeSearcher.noTargetMove(currentTime));
            }*/
            //通れない道なら，Targetに設定
            //this.target = this.location.getID();
            this.beforeMove = true;
            List<EntityID> path = this.routeSearcher.getPath(currentTime, this.me, this.target);
            return new ActionMove(this, path != null ? path : this.routeSearcher.noTargetMove(currentTime, this.me));
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //対象の確定
        Road road = (Road)this.world.getEntity(this.target);
        List<Point2D> clearList = this.getClearList(road);
        while(clearList == null || clearList.isEmpty()) {
            this.impassableSelector.remove(road);
            this.target = this.impassableSelector.getNewTarget(currentTime);
            if(this.target == null) {
                this.beforeMove = true;
                return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
            }
            road = (Road)this.world.getEntity(this.target);
            clearList = this.getClearList(road);
        }
        
        //道の点の選択
        if(oldTarget != this.target.getValue()) {
            this.mainTargetPoint = clearList.get(0);
        }
        else if(this.mainTargetPoint == null) {
            this.mainTargetPoint = clearList.get(0);
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //移動した直後か，Clear後の移動を行った場合
        if(this.beforeMove) {
            if(PositionUtil.equalsPoint(this.agentPoint[0], this.mainTargetPoint, 2.0D)) {
                this.removeTargetPoint(road, this.mainTargetPoint);
                this.mainTargetPoint = null;
                List<Point2D> clearPoint = this.clearListMap.get(this.target);
                this.beforeMove = true;
                if(clearPoint != null && clearPoint.size() > 0) {
                    this.mainTargetPoint = clearPoint.get(0);
                    return new ActionMove(this, Lists.newArrayList(this.target), (int) this.mainTargetPoint.getX(), (int) this.mainTargetPoint.getY());
                }
                else {
                    this.target = this.impassableSelector.getNewTarget(currentTime);
                    List<EntityID> path = null;
                    if(this.target != null) {
                        path = this.routeSearcher.getPath(currentTime, this.getID(), this.target);
                    }
                    return new ActionMove(this, path != null ? path : this.routeSearcher.noTargetMove(currentTime ,this.me));
                }
            }
            else {
                Vector2D vector = this.getVector(this.agentPoint[0], this.mainTargetPoint, road);
                this.beforeMove = false;
                return new ActionClear(this, (int) (this.me.getX() + vector.getX()), (int) (this.me.getY() + vector.getY()));
            }
        }
        else {
            this.beforeMove = true;
            return new ActionMove(this, Lists.newArrayList(this.target), (int) this.mainTargetPoint.getX(), (int) this.mainTargetPoint.getY());
        }
    }

    public Vector2D getVector(Point2D agentPos, Point2D targetPos, Road road) {
        EntityID roadID = road.getID();
        if (!this.clearListMap.containsKey(roadID)) {
            this.analysisRoad(road);
        }
        List<Edge> edges = road.getEdges();
        if (this.canStraightForward(agentPos, targetPos, roadID, edges)) {
            return targetPos.minus(agentPos).normalised().scale(1000000);
        } else {
            Point2D edgePoint;
            Point2D min = null;
            for (Edge edge : edges) {
                edgePoint = PositionUtil.getEdgePoint(edge);
                if (this.canStraightForward(agentPos, edgePoint, roadID, edges)) {
                    min = min != null ? PositionUtil.compareDistance(agentPos, min, edgePoint).translate(0.0D, 0.0D) : edgePoint.translate(0.0D, 0.0D);
                }
            }
            return min == null ? targetPos.minus(agentPos).normalised().scale(1000000) : min.minus(agentPos).normalised().scale(1000000);
        }
    }
    
    public Action moveRefuge(int currentTime) {
        Refuge result = PositionUtil.getNearTarget(this.world, this.me, this.getRefuges());
        List<EntityID> path = getRouteSearcher().getPath(currentTime, this.me(), result);
        
        Logger.trace(String.format("moveRefuge called. dest=%s, path=%s, me=%s", result, path, this.me()));
        
        return new ActionMove(this, path != null ? path : getRouteSearcher().noTargetMove(currentTime, this.me()));
    }

    public void analysisRoad(Road road) {
        EntityID roadID = road.getID();
        if (this.clearListMap.containsKey(roadID)) {
            return;
        }
        List<List<Edge>> neighbourEdges = new ArrayList<>();
        List<Point2D> passablePoint = new ArrayList<>();
        road.getEdges().stream().filter(Edge::isPassable).forEach(edge -> {
            List<Edge> edges = new ArrayList<>(((Area) this.getWorld().getEntity(edge.getNeighbour())).getEdges());
            edges.remove(edge);
            neighbourEdges.add(edges);
            passablePoint.add(PositionUtil.getEdgePoint(edge));
        });
        List<Point2D> clearList;
        if (road.getBlockades().isEmpty()) {
            clearList = new ArrayList<>();
            this.impassableSelector.remove(road);
        } else {
            clearList = new ArrayList<>(passablePoint);
            this.impassableSelector.add(road);
        }
        this.neighbourEdgesMap.put(roadID, neighbourEdges);
        this.passablePointMap.put(roadID, passablePoint);
        this.clearListMap.put(roadID, clearList);
    }

    public List<Point2D> getClearList(Road road) {
        EntityID roadID = road.getID();
        if (!this.clearListMap.containsKey(roadID)) {
            this.analysisRoad(road);
        }
        return this.clearListMap.get(roadID);
    }

    public void removeTargetPoint(Road road, Point2D point) {
        EntityID roadID = road.getID();
        List<Point2D> clearList = this.clearListMap.get(roadID);
        clearList.remove(point);
        this.clearListMap.put(roadID, clearList);
        if (clearList.isEmpty()) {
            this.impassableSelector.remove(road);
        }
    }

    public boolean canStraightForward(Point2D point, Point2D targetPoint, EntityID roadID, Collection<Edge> edges) {
        for (Edge edge : edges) {
            if (this.linesIntersect(point, targetPoint, edge)) {
                return false;
            }
        }
        for (List<Edge> list : this.neighbourEdgesMap.get(roadID)) {
            if (!this.canStraightForward(point, targetPoint, list)) {
                return false;
            }
        }
        return true;
    }

    public boolean canStraightForward(Point2D point, Point2D targetPoint, Collection<Edge> edges) {
        for (Edge edge : edges) {
            if (this.linesIntersect(point, targetPoint, edge)) {
                return false;
            }
        }
        return true;
    }

    public boolean linesIntersect(Point2D point, Point2D targetPoint, Edge edge) {
        Point2D start = edge.getStart();
        double startX = start.getX();
        double startY = start.getY();
        Point2D end = edge.getEnd();
        double endX = end.getX();
        double endY = end.getY();
        return Line2D.linesIntersect(point.getX(), point.getY(), targetPoint.getX(), targetPoint.getY(), startX, startY, endX, endY) && !PositionUtil.equalsPoint(targetPoint, ((startX + endX) / 2.0D), (startY + endX) / 2.0D, 10.0D);
    }
}
