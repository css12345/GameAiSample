package com.huawei.contest.gameai.base.client.movement;

import com.huawei.contest.gameai.base.client.AIConfig;
import com.huawei.contest.gameai.base.client.entity.GameWorldState;
import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.Position;

import java.util.*;

public class MovementCoordinator {
    private GameWorldState world;
    private GridGraph graph;
    private ThreatMap threatMap;
    private int maxSteps = 10;

    public MovementCoordinator(GameWorldState world) {
        this.world = world;
        this.graph = new GridGraph(world);
        this.threatMap = new ThreatMap(world);
    }

    /**
     * 规划单个小队的移动。
     * @param squadUnits 本小队待移动单位
     * @param targetCenter 目标位置
     * @param enemies 所有敌方单位
     * @param aggressiveness 激进程度 0-1
     * @param config AI配置
     * @param globalResTable 全局预留表，跨小队共享
     * @param globalVacateds 全局腾空哈希集合，跨小队共享
     * @return 每个单位的下一步位置
     */
    public Map<Integer, Position> planSquadMovement(List<IUnit> squadUnits, Position targetCenter,
                                                    List<IUnit> enemies, double aggressiveness, AIConfig config,
                                                    ReservationTable globalResTable, Set<Position> globalVacateds) {
        // 设置 graph 使用全局腾空集合
        graph.setVacatedPositions(globalVacateds);

        // 预占用：所有己方单位当前格 step=0 预留
        for (IUnit u : squadUnits) {
            globalResTable.reserve(0, u.getPos().getX(), u.getPos().getY(), world.getWidth(), u.getId());
        }

        threatMap.update(enemies);
        SpaceTimeAStar astar = new SpaceTimeAStar(graph, globalResTable, maxSteps, world, threatMap);
        double dangerWeight = config.getPathDangerWeight() - (config.getPathDangerWeight() - config.getAggressiveDangerMod()) * aggressiveness;
        double threshold = config.getPathMaxDangerThreshold() + (1 - aggressiveness) * 2.0;
        astar.setDangerWeight(dangerWeight);
        astar.setMaxDangerThreshold(threshold);

        // ========= 排序：狭窄通道依赖排序 =========
        List<IUnit> sorted = new ArrayList<>(squadUnits);
        Position squadCenter = getCenter(sorted);
        int dirX = Integer.signum(targetCenter.getX() - squadCenter.getX());
        int dirY = Integer.signum(targetCenter.getY() - squadCenter.getY());

        if (dirX == 0 && dirY == 0) {
            // 已达目标附近，使用原有距离排序
            sorted.sort(Comparator.comparingInt(u ->
                    (u.getDamage() > 10 ? 0 : 10) + u.getPos().chebyshev(targetCenter)));
        } else {
            // 构建位置->单位映射
            Map<Position, IUnit> posToUnit = new HashMap<>();
            for (IUnit u : sorted) posToUnit.put(u.getPos(), u);

            // 阻挡者计数：在前进方向上且离目标更近的友军数量
            Map<IUnit, Integer> blockerCount = new HashMap<>();
            for (IUnit u : sorted) {
                int blockers = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0)  {
                            continue;
                        }
                        Position adj = u.getPos().add(dx, dy);
                        IUnit other = posToUnit.get(adj);
                        if (other != null && adj.chebyshev(targetCenter) < u.getPos().chebyshev(targetCenter)) {
                            blockers++;
                        }
                    }
                }
                blockerCount.put(u, blockers);
            }

            // 按阻挡者数量升序，相同则按伤害优先级+距离
            sorted.sort(Comparator.<IUnit>comparingInt(u -> blockerCount.getOrDefault(u, 0))
                    .thenComparingInt(u -> (u.getDamage() > 10 ? 0 : 10) + u.getPos().chebyshev(targetCenter)));
        }

        // ========= 依次规划 =========
        Map<Integer, List<Position>> paths = new LinkedHashMap<>();
        for (IUnit unit : sorted) {
            List<Position> path = astar.search(unit, targetCenter);
            if (path != null && !path.isEmpty()) {
                paths.put(unit.getId(), path);
                // 成功移动，原位置加入全局腾空集合
                globalVacateds.add(unit.getPos());
                // 标记路径到全局预留表
                markPath(unit.getId(), path, globalResTable);
            } else {
                paths.put(unit.getId(), null);
            }
        }

        // 提取第一步
        Map<Integer, Position> firstSteps = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Position>> e : paths.entrySet()) {
            List<Position> path = e.getValue();
            if (path != null && !path.isEmpty()) firstSteps.put(e.getKey(), path.get(0));
            else firstSteps.put(e.getKey(), world.getUnits().get(e.getKey()).getPos());
        }

        // 注意：不要清空 globalVacated，由外部管理
        return firstSteps;
    }

    private void markPath(int unitId, List<Position> path, ReservationTable resTable) {
        for (int i = 0; i < path.size(); i++) {
            Position p = path.get(i);
            resTable.reserve(i + 1, p.getX(), p.getY(), world.getWidth(), unitId);
        }
    }

    private Position getCenter(Collection<IUnit> units) {
        if (units.isEmpty()) return new Position(0, 0);
        int ax = 0, ay = 0;
        for (IUnit u : units) {
            ax += u.getPos().getX();
            ay += u.getPos().getY();
        }
        return new Position(ax / units.size(), ay / units.size());
    }

    public void cleanup() {
        graph.setVacatedPositions(Collections.emptySet());
    }
}