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
    /** A* 搜索访问过的位置（可视化用） */
    private final Set<Long> visitedNodes = new HashSet<>();
    /** 节点展开上限：超过则回退到最接近目标的已探索节点 */
    @Setter
    private int maxExpansions = 4_000;

    public SpaceTimeAStar(GridGraph graph, ReservationTable resTable, int maxSteps,
                   GameWorldState world, ThreatMap threatMap) {
        this.graph = graph;
        this.resTable = resTable;
        this.maxSteps = maxSteps;
        this.threatMap = threatMap;
    }

    /** 紧凑 long 键：step(20bit) | y(10bit) | x(10bit)，替代 String 拼接 */
    private static long stateKey(int x, int y, int step) {
        return ((long) step << 20) | (y << 10) | x;
    }

    public List<Position> search(IUnit unit, Position target) {
        visitedNodes.clear();
        int startX = unit.getPos().getX(), startY = unit.getPos().getY();
        int targetX = target.getX(), targetY = target.getY();
        int width = graph.getWidth();

        PriorityQueue<SpaceTimeNode> open = new PriorityQueue<>(4096);
        Map<Long, SpaceTimeNode> visited = new HashMap<>(4096);

        int startH = chebyshev(startX, startY, targetX, targetY);
        SpaceTimeNode start = new SpaceTimeNode(startX, startY, 0,
                startH * 10 + getDangerCost(startX, startY), null, 0);
        open.add(start);
        long startKey = stateKey(startX, startY, 0);
        visited.put(startKey, start);
        visitedNodes.add(startKey);

        SpaceTimeNode bestNode = null;
        int bestDist = Integer.MAX_VALUE;
        int startDist = startH;

        int expansions = 0;
        while (!open.isEmpty()) {
            SpaceTimeNode current = open.poll();
            int cx = current.x, cy = current.y;
            int curDist = chebyshev(cx, cy, targetX, targetY);
            if (curDist < bestDist && current.step > 0) {
                bestDist = curDist;
                bestNode = current;
            }
            if (current.step > 0 && curDist <= 1) {
                return reconstructPath(current);
            }
            if (current.step >= maxSteps) continue;
            if (++expansions > maxExpansions) {
                if (bestNode != null && bestDist < startDist) {
                    return reconstructPath(bestNode);
                }
                return null;
            }

            // 展开邻居（内联避免对象创建）
            // 1. 原地等待
            expandNeighbor(open, visited, current, cx, cy, width, targetX, targetY);
            // 2. 八方向
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = cx + dx, ny = cy + dy;
                    if (!graph.isWalkableAt(nx, ny)) continue;
                    // 对角线防穿墙角
                    if (dx != 0 && dy != 0) {
                        if (!graph.isWalkableAt(cx + dx, cy) || !graph.isWalkableAt(cx, cy + dy)) {
                            continue;
                        }
                    }
                    expandNeighbor(open, visited, current, nx, ny, width, targetX, targetY);
                }
            }
        }

        if (bestNode != null && bestDist < startDist) {
            return reconstructPath(bestNode);
        }
        return null;
    }

    private void expandNeighbor(PriorityQueue<SpaceTimeNode> open, Map<Long, SpaceTimeNode> visited,
                                SpaceTimeNode current, int nx, int ny, int width,
                                int targetX, int targetY) {
        int nextStep = current.step + 1;
        if (resTable.getReservation(nextStep, nx, ny, width) != -1) {
            return;
        }
        long key = stateKey(nx, ny, nextStep);
        if (visited.containsKey(key)) {
            return;
        }
        int stepCost = 10 + getDangerCost(nx, ny);
        int newG = current.gCost + stepCost;
        int newH = chebyshev(nx, ny, targetX, targetY) * 10;
        SpaceTimeNode neighborNode = new SpaceTimeNode(nx, ny, nextStep, newG + newH, current, newG);
        open.add(neighborNode);
        visited.put(key, neighborNode);
        visitedNodes.add(key);
    }

    private int getDangerCost(int x, int y) {
        if (threatMap == null) return 0;
        return (int)(threatMap.getDanger(x, y) * dangerWeight * 10);
    }

    private static int chebyshev(int ax, int ay, int bx, int by) {
        return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }

    private List<Position> reconstructPath(SpaceTimeNode node) {
        LinkedList<Position> path = new LinkedList<>();
        while (node != null) {
            path.addFirst(Position.of(node.x, node.y));
            node = node.parent;
        }
        if (!path.isEmpty()) path.removeFirst();
        return path;
    }

    /** 返回 A* 搜索过程中访问过的所有 (x, y) 位置，用于可视化调试 */
    public Set<Position> getVisitedPositions() {
        Set<Position> result = new HashSet<>(visitedNodes.size());
        for (Long key : visitedNodes) {
            int x = (int)(key & 0x3FF);
            int y = (int)((key >>> 10) & 0x3FF);
            result.add(Position.of(x, y));
        }
        return result;
    }

    static class SpaceTimeNode implements Comparable<SpaceTimeNode> {
        final int x, y, step, fCost, gCost;
        final SpaceTimeNode parent;
        SpaceTimeNode(int x, int y, int step, int f, SpaceTimeNode p, int g) {
            this.x = x; this.y = y; this.step = step; this.fCost = f; this.parent = p; this.gCost = g;
        }
        @Override public int compareTo(SpaceTimeNode o) { return Integer.compare(fCost, o.fCost); }
    }
}
