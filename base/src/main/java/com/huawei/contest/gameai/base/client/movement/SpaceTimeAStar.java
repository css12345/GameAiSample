package com.huawei.contest.gameai.base.client.movement;

import com.badlogic.gdx.ai.pfa.Connection;
import com.huawei.contest.gameai.base.client.entity.GameWorldState;
import com.huawei.contest.gameai.base.client.entity.GridNode;
import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.Position;
import lombok.Setter;

import java.util.*;

public class SpaceTimeAStar {
    private final GridGraph graph;
    private final ReservationTable resTable;
    private final int maxSteps;
    private ThreatMap threatMap;
    @Setter
    private double dangerWeight = 3.0;
    @Setter
    private double maxDangerThreshold = 1.5;

    public SpaceTimeAStar(GridGraph graph, ReservationTable resTable, int maxSteps,
                   GameWorldState world, ThreatMap threatMap) {
        this.graph = graph;
        this.resTable = resTable;
        this.maxSteps = maxSteps;
        this.threatMap = threatMap;
    }

    public List<Position> search(IUnit unit, Position target) {
        GridNode startNode = new GridNode(unit.getPos().getX(), unit.getPos().getY());
        GridNode targetNode = new GridNode(target.getX(), target.getY());

        PriorityQueue<SpaceTimeNode> open = new PriorityQueue<>();
        Map<String, SpaceTimeNode> visited = new HashMap<>();

        SpaceTimeNode start = new SpaceTimeNode(startNode, 0,
                heuristic(startNode, targetNode) + getDangerCost(startNode), null, 0);
        open.add(start);
        visited.put(stateKey(startNode.getX(), startNode.getY(), 0), start);

        while (!open.isEmpty()) {
            SpaceTimeNode current = open.poll();
            // 终止条件：至少移动一步，且距离目标切比雪夫距离<=1
            if (current.step > 0 && chebyshevDist(current.node, targetNode) <= 1) {
                return reconstructPath(current);
            }
            if (current.step >= maxSteps) continue;

            // 扩展：原地等待 + 八个邻居（由Graph提供）
            List<GridNode> neighbors = new ArrayList<>();
            neighbors.add(current.node); // 等待
            for (Connection<GridNode> conn : graph.getConnections(current.node)) {
                neighbors.add(conn.getToNode());
            }

            for (GridNode next : neighbors) {
                int nextStep = current.step + 1;
                // 1. 预留检查
                if (resTable.getReservation(nextStep, next.getX(), next.getY(), graph.getWidth()) != -1) {
                    continue;
                }
                // 2. 威胁阻断
                if (threatMap != null && maxDangerThreshold > 0 &&
                        threatMap.getDanger(next.getX(), next.getY()) > maxDangerThreshold) {
                    continue;
                }
                String key = stateKey(next.getX(), next.getY(), nextStep);
                if (visited.containsKey(key)) {
                    continue;
                }

                int stepCost = 10 + getDangerCost(next);
                int newG = current.gCost + stepCost;
                int newH = heuristic(next, targetNode);
                SpaceTimeNode neighborNode = new SpaceTimeNode(next, nextStep, newG + newH, current, newG);
                open.add(neighborNode);
                visited.put(key, neighborNode);
            }
        }
        return null;
    }

    private int getDangerCost(GridNode node) {
        if (threatMap == null) {
            return 0;
        }
        return (int)(threatMap.getDanger(node.getX(), node.getY()) * dangerWeight * 10);
    }

    private int chebyshevDist(GridNode a, GridNode b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    private int heuristic(GridNode a, GridNode b) { return chebyshevDist(a, b) * 10; }

    private List<Position> reconstructPath(SpaceTimeNode node) {
        LinkedList<Position> path = new LinkedList<>();
        while (node != null) {
            path.addFirst(new Position(node.node.getX(), node.node.getY()));
            node = node.parent;
        }
        if (!path.isEmpty()) path.removeFirst(); // 移除起点
        return path;
    }

    private String stateKey(int x, int y, int step) { return x + "," + y + "," + step; }

    static class SpaceTimeNode implements Comparable<SpaceTimeNode> {
        GridNode node; int step; int fCost; int gCost; SpaceTimeNode parent;
        SpaceTimeNode(GridNode n, int step, int f, SpaceTimeNode p, int g) { node=n; this.step=step; fCost=f; parent=p; gCost=g; }
        @Override public int compareTo(SpaceTimeNode o) { return Integer.compare(fCost, o.fCost); }
    }
}
