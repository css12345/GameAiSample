package com.huawei.contest.gameai.base.client.movement;

import com.huawei.contest.gameai.base.client.AIConfig;
import com.huawei.contest.gameai.base.client.entity.GameUnit;
import com.huawei.contest.gameai.base.client.entity.GameWorldState;
import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.Position;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MovementCoordinator {
    private GameWorldState world;
    private GridGraph graph;
    private ThreatMap threatMap;
    /**
     * 安全上限：防止无解时无限搜索。
     * 默认 = 2×(宽+高)，覆盖绕障碍物场景。40×40 地图 = 160 步。
     * 设为 0 或负数表示不限制（危险，仅调试用）。
     */
    private int maxSteps;
    private SpaceTimeAStar lastAStar;

    public MovementCoordinator(GameWorldState world) {
        this.world = world;
        this.graph = new GridGraph(world);
        this.threatMap = new ThreatMap(world);
        // 对角线 Manhattan 距离 × 2，覆盖绕障碍物场景
        this.maxSteps = (world.getWidth() + world.getHeight()) * 2;
    }

    /**
     * 规划单个小队的移动。
     * @param squadUnits 本小队待移动单位
     * @param targetCenter 目标位置
     * @param enemies 所有敌方单位
     * @param aggressiveness 激进程度 0-1
     * @param config AI配置
     * @param globalResTable 全局预留表，跨小队共享
     * @param globalVacates 全局腾空哈希集合，跨小队共享
     * @return 每个单位的下一步位置
     */
    public Map<Integer, Position> planSquadMovement(List<IUnit> squadUnits, Position targetCenter,
                                                    List<IUnit> enemies, double aggressiveness, AIConfig config,
                                                    ReservationTable globalResTable, Set<Position> globalVacates) {
        // 设置 graph 使用全局腾空集合
        graph.setVacatedPositions(globalVacates);

        // 预占用：所有己方单位当前格 step=0 预留
        for (IUnit u : squadUnits) {
            globalResTable.reserve(0, u.getPos().getX(), u.getPos().getY(), world.getWidth(), u.getId());
        }

        threatMap.update(enemies);
        SpaceTimeAStar astar = new SpaceTimeAStar(graph, globalResTable, maxSteps, world, threatMap);
        this.lastAStar = astar;
        double dangerWeight = config.getPathDangerWeight() - (config.getPathDangerWeight() - config.getAggressiveDangerMod()) * aggressiveness;
        double threshold = config.getPathMaxDangerThreshold() + (1 - aggressiveness) * 2.0;
        astar.setDangerWeight(dangerWeight);
        astar.setMaxDangerThreshold(threshold);
        log.debug("A*参数: dangerWeight={}, maxDangerThreshold={}", dangerWeight, threshold);

        // ========= 排序：狭窄通道依赖排序 =========
        List<IUnit> sorted = new ArrayList<>(squadUnits);
        Position squadCenter = getCenter(sorted);
        int dirX = Integer.signum(targetCenter.getX() - squadCenter.getX());
        int dirY = Integer.signum(targetCenter.getY() - squadCenter.getY());

        log.debug("小队中心=({},{}), 方向=({},{}), 目标=({},{})",
                squadCenter.getX(), squadCenter.getY(), dirX, dirY,
                targetCenter.getX(), targetCenter.getY());

        if (dirX == 0 && dirY == 0) {
            sorted.sort(Comparator.comparingInt(u ->
                    (u.getDamage() > 10 ? 0 : 10) + u.getPos().chebyshev(targetCenter)));
        } else {
            Map<Position, IUnit> posToUnit = new HashMap<>();
            for (IUnit u : sorted) posToUnit.put(u.getPos(), u);

            Map<IUnit, Integer> blockerCount = new HashMap<>();
            for (IUnit u : sorted) {
                int blockers = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        Position adj = u.getPos().add(dx, dy);
                        IUnit other = posToUnit.get(adj);
                        if (other != null && adj.chebyshev(targetCenter) < u.getPos().chebyshev(targetCenter)) {
                            blockers++;
                        }
                    }
                }
                blockerCount.put(u, blockers);
            }

            sorted.sort(Comparator.<IUnit>comparingInt(u -> blockerCount.getOrDefault(u, 0))
                    .thenComparingInt(u -> (u.getDamage() > 10 ? 0 : 10) + u.getPos().chebyshev(targetCenter)));
        }

        log.debug("规划顺序: {}", sorted.stream()
                .map(u -> String.format("%d@(%d,%d)", u.getId(),
                        u.getPos().getX(), u.getPos().getY()))
                .collect(Collectors.joining(" → ")));

        // ========= 依次规划 =========
        Map<Integer, List<Position>> paths = new LinkedHashMap<>();
        for (IUnit unit : sorted) {
            List<Position> path = astar.search(unit, targetCenter);
            if (path != null && !path.isEmpty()) {
                paths.put(unit.getId(), path);
                globalVacates.add(unit.getPos());
                markPath(unit.getId(), path, globalResTable);
                log.debug("单位 id={} 规划成功, 路径={}步, 第一步→({},{})",
                        unit.getId(), path.size(),
                        path.get(0).getX(), path.get(0).getY());
            } else {
                paths.put(unit.getId(), null);
                log.warn("单位 id={} (pos=({},{})) 无法找到到目标({},{})的路径",
                        unit.getId(), unit.getPos().getX(), unit.getPos().getY(),
                        targetCenter.getX(), targetCenter.getY());
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
        // === 关键：锁定终点位置，防止后来者停在同一个格子上 ===
        // A* 终止条件是 chebyshevDist<=1，不同单位可能在不同 step 到达同一终点格。
        // 把终点从到达步数+1 到 maxSteps 全部预留，确保后来者必须选不同的相邻格。
        Position finalPos = path.get(path.size() - 1);
        for (int s = path.size() + 1; s <= maxSteps; s++) {
            resTable.reserve(s, finalPos.getX(), finalPos.getY(), world.getWidth(), unitId);
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

    public void setMaxSteps(int maxSteps) { this.maxSteps = maxSteps; }
    public int getMaxSteps() { return maxSteps; }

    public void cleanup() {
        graph.setVacatedPositions(Collections.emptySet());
    }

    /** 获取最后使用的 SpaceTimeAStar 实例，用于可视化 A* 搜索过程 */
    public SpaceTimeAStar getLastAStar() { return lastAStar; }

    /** 获取威胁地图，用于可视化热力图层 */
    public ThreatMap getThreatMap() { return threatMap; }
}