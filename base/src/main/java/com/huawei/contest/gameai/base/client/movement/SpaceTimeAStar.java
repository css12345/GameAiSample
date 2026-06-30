package com.huawei.contest.gameai.base.client.movement;

import com.badlogic.gdx.ai.pfa.Connection;
import com.huawei.contest.gameai.base.client.entity.GameWorldState;
import com.huawei.contest.gameai.base.client.entity.GridNode;
import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.Position;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

public class SpaceTimeAStar {
    private final GridGraph graph;
    private final ReservationTable resTable;
    private final int maxSteps;
    private ThreatMap threatMap;
    @Setter
    private double dangerWeight = 3.0;
    @Setter
    private double maxDangerThreshold = 1.5;
    private final Set<String> visitedNodes = new HashSet<>();
    /** 节点展开上限：超过则回退到最接近目标的已探索节点。25K 平衡速度与路径质量 */
    @Setter
    private int maxExpansions = 25_000;

    public SpaceTimeAStar(GridGraph graph, ReservationTable resTable, int maxSteps,
                   GameWorldState world, ThreatMap threatMap) {
        this.graph = graph;
        this.resTable = resTable;
        this.maxSteps = maxSteps;
        this.threatMap = threatMap;
    }

    public List<Position> search(IUnit unit, Position target) {
        visitedNodes.clear();
        GridNode startNode = new GridNode(unit.getPos().getX(), unit.getPos().getY());
        GridNode targetNode = new GridNode(target.getX(), target.getY());

        PriorityQueue<SpaceTimeNode> open = new PriorityQueue<>();
        Map<String, SpaceTimeNode> visited = new HashMap<>();

        SpaceTimeNode start = new SpaceTimeNode(startNode, 0,
                heuristic(startNode, targetNode) + getDangerCost(startNode), null, 0);
        open.add(start);
        String startKey = stateKey(startNode.getX(), startNode.getY(), 0);
        visited.put(startKey, start);
        visitedNodes.add(startKey);

        // 追踪最接近目标的节点（用于失败时的 best-effort 回退）
        SpaceTimeNode bestNode = null;
        int bestDist = Integer.MAX_VALUE;

        int expansions = 0;
        while (!open.isEmpty()) {
            SpaceTimeNode current = open.poll();
            int curDist = chebyshevDist(current.node, targetNode);
            if (curDist < bestDist && current.step > 0) {
                bestDist = curDist;
                bestNode = current;
            }
            // 终止条件：至少移动一步，且距离目标切比雪夫距离<=1
            if (current.step > 0 && curDist <= 1) {
                return reconstructPath(current);
            }
            if (current.step >= maxSteps) continue;
            // 节点展开预算耗尽 → 回退到最佳节点，至少靠近目标
            if (++expansions > maxExpansions) {
                if (bestNode != null && bestDist < chebyshevDist(startNode, targetNode)) {
                    return reconstructPath(bestNode);
                }
                return null;
            }

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
                visitedNodes.add(key);
            }
        }

        // 搜索空间耗尽：回退到最佳节点
        if (bestNode != null && bestDist < chebyshevDist(startNode, targetNode)) {
            return reconstructPath(bestNode);
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

    /** 返回 A* 搜索过程中访问过的所有 (x, y, step) 位置，用于可视化调试 */
    public Set<Position> getVisitedPositions() {
        return visitedNodes.stream()
                .map(key -> {
                    String[] parts = key.split(",");
                    return Position.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                })
                .collect(Collectors.toSet());
    }

    private String stateKey(int x, int y, int step) { return x + "," + y + "," + step; }

    static class SpaceTimeNode implements Comparable<SpaceTimeNode> {
        GridNode node; int step; int fCost; int gCost; SpaceTimeNode parent;
        SpaceTimeNode(GridNode n, int step, int f, SpaceTimeNode p, int g) { node=n; this.step=step; fCost=f; parent=p; gCost=g; }
        @Override public int compareTo(SpaceTimeNode o) { return Integer.compare(fCost, o.fCost); }
    }
}
