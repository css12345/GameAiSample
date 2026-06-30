package com.huawei.contest.gameai.base.client.visual;

import com.huawei.contest.gameai.base.client.AIConfig;
import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.base.client.movement.MovementCoordinator;
import com.huawei.contest.gameai.base.client.movement.ReservationTable;
import com.huawei.contest.gameai.base.client.model.Start;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 移动模拟引擎 — 封装 MovementCoordinator，管理模拟状态，暴露可视化数据。
 */
@Slf4j
public class MovementSimulator {

    private static final AtomicInteger ID_GEN = new AtomicInteger(1000);

    private final int myPlayerId = 1111;
    private final int enemyPlayerId = 2222;

    private GameWorldState world;
    private AIConfig config = AIConfig.aggressiveRush();
    private double aggressiveness = 0.5;
    private String rawMapData; // 原始地图字符串，用于地形渲染

    private final Map<Integer, GameUnit> myUnits = new LinkedHashMap<>();
    private final Map<Integer, GameUnit> enemyUnits = new LinkedHashMap<>();
    private Position target;

    // 移动规划结果
    private MovementCoordinator coordinator;
    private ReservationTable resTable;
    private Set<Position> vacated;
    private Map<Integer, List<Position>> fullPaths;   // unitId -> 完整路径（不含起点）
    private Map<Integer, Position> firstSteps;         // unitId -> 下一步位置
    private int currentStep = 0;
    private int maxPathLen = 0;

    // 记录每一步所有单位的位置快照（用于回放）
    private final List<Map<Integer, Position>> stepSnapshots = new ArrayList<>();

    public void loadMap(String mapName) {
        MapDataLoader.MapInfo info = MapDataLoader.getMap(mapName);
        if (info == null) throw new IllegalArgumentException("Unknown map: " + mapName);
        this.rawMapData = info.data();
        Start start = MapDataLoader.buildStart(mapName, myPlayerId, enemyPlayerId);
        this.world = GameWorldState.fromMapString(start, myPlayerId);
        reset();
    }

    public void loadCustomMap(int width, int height, String mapData) {
        this.rawMapData = mapData;
        Start start = MapDataLoader.buildStart(width, height, mapData, myPlayerId, enemyPlayerId);
        this.world = GameWorldState.fromMapString(start, myPlayerId);
        reset();
    }

    private void reset() {
        myUnits.clear();
        enemyUnits.clear();
        target = null;
        fullPaths = null;
        firstSteps = null;
        resTable = new ReservationTable();
        vacated = new HashSet<>();
        currentStep = 0;
        maxPathLen = 0;
        stepSnapshots.clear();
    }

    // ==================== 单位管理 ====================

    public GameUnit addMyUnit(UnitType type, int x, int y) {
        int id = ID_GEN.incrementAndGet();
        GameUnit u = new GameUnit(id, myPlayerId, type, Position.of(x, y), type.getMaxHp());
        myUnits.put(id, u);
        syncUnitsToWorld();
        return u;
    }

    public GameUnit addEnemyUnit(UnitType type, int x, int y) {
        int id = ID_GEN.incrementAndGet();
        GameUnit u = new GameUnit(id, enemyPlayerId, type, Position.of(x, y), type.getMaxHp());
        enemyUnits.put(id, u);
        syncUnitsToWorld();
        return u;
    }

    public void removeMyUnit(int unitId) {
        myUnits.remove(unitId);
        syncUnitsToWorld();
    }

    public void removeEnemyUnit(int unitId) {
        enemyUnits.remove(unitId);
        syncUnitsToWorld();
    }

    private void syncUnitsToWorld() {
        if (world == null) return;
        world.getUnits().clear();
        myUnits.values().forEach(u -> world.getUnits().put(u.getId(), u));
        enemyUnits.values().forEach(u -> world.getUnits().put(u.getId(), u));
        world.refreshOccupied();
    }

    // ==================== 目标 ====================

    public void setTarget(int x, int y) {
        this.target = Position.of(x, y);
    }

    // ==================== 规划移动 ====================

    public void planMovement() {
        if (world == null || myUnits.isEmpty() || target == null) return;

        resTable.clear();
        vacated.clear();
        fullPaths = null;
        firstSteps = null;
        currentStep = 0;
        maxPathLen = 0;
        stepSnapshots.clear();

        syncUnitsToWorld();
        coordinator = new MovementCoordinator(world);

        List<IUnit> squadUnits = new ArrayList<>(myUnits.values());
        List<IUnit> enemies = new ArrayList<>(enemyUnits.values());

        // ===== 日志：规划开始 =====
        log.info("========== 移动规划开始 ==========");
        log.info("地图: {}x{}", world.getWidth(), world.getHeight());
        log.info("目标位置: ({}, {})", target.getX(), target.getY());
        log.info("攻击性: {}, 危险权重: {}, 最大危险阈值: {}, maxSteps: {}",
                String.format("%.2f", aggressiveness), config.getPathDangerWeight(),
                config.getPathMaxDangerThreshold(), coordinator.getMaxSteps());

        String unitList = squadUnits.stream()
                .map(u -> String.format("%s(id=%d, pos=(%d,%d))",
                        ((GameUnit)u).type.name(), u.getId(), u.getPos().getX(), u.getPos().getY()))
                .collect(Collectors.joining(", "));
        log.info("己方单位 ({}个): [{}]", squadUnits.size(), unitList);

        String enemyList = enemies.stream()
                .map(u -> String.format("%s(id=%d, pos=(%d,%d))",
                        ((GameUnit)u).type.name(), u.getId(), u.getPos().getX(), u.getPos().getY()))
                .collect(Collectors.joining(", "));
        log.info("敌方单位 ({}个): [{}]", enemies.size(), enemyList);

        firstSteps = coordinator.planSquadMovement(
                squadUnits, target, enemies, aggressiveness, config, resTable, vacated);

        // 从预留表重建完整路径
        fullPaths = new LinkedHashMap<>();
        for (GameUnit u : myUnits.values()) {
            List<Position> path = new ArrayList<>();
            Position next = firstSteps.get(u.getId());
            if (next != null && !next.equals(u.getPos())) {
                path = reconstructPathFromReservations(u.getId());
                if (path.isEmpty()) {
                    path.add(next);
                }
                maxPathLen = Math.max(maxPathLen, path.size());
            }
            fullPaths.put(u.getId(), path);

            // ===== 日志：每个单位的路径 =====
            if (!path.isEmpty()) {
                int maxShow = 30; // 最多显示前30步
                String pathStr;
                if (path.size() <= maxShow) {
                    pathStr = path.stream()
                            .map(p -> String.format("(%d,%d)", p.getX(), p.getY()))
                            .collect(Collectors.joining(" → "));
                } else {
                    String head = path.subList(0, 15).stream()
                            .map(p -> String.format("(%d,%d)", p.getX(), p.getY()))
                            .collect(Collectors.joining(" → "));
                    String tail = path.subList(path.size() - 3, path.size()).stream()
                            .map(p -> String.format("(%d,%d)", p.getX(), p.getY()))
                            .collect(Collectors.joining(" → "));
                    pathStr = head + " → ...(" + (path.size() - 18) + "步)... → " + tail;
                }
                log.info("单位 {} ({}id={}) 路径 ({}步): 起点({},{}) → {}",
                        u.type.name(), u.getPlayerId() == myPlayerId ? "" : "敌",
                        u.getId(), path.size(),
                        u.getPos().getX(), u.getPos().getY(), pathStr);
            } else {
                log.info("单位 {} (id={}) 无法到达目标，保持在 ({},{})",
                        u.type.name(), u.getId(), u.getPos().getX(), u.getPos().getY());
            }
        }

        log.info("最长路径: {} 步", maxPathLen);
        log.info("========== 移动规划结束 ==========");

        // 记录初始快照 (step 0)
        recordSnapshot();
    }

    /** 从预留表中重建单位路径（跳过连续相同位置，仅记录实际移动） */
    private List<Position> reconstructPathFromReservations(int unitId) {
        List<Position> path = new ArrayList<>();
        Position lastAdded = null;
        for (var entry : resTable.getUnitReservations(unitId, world.getWidth())) {
            Position cur = entry.getValue();
            if (!cur.equals(lastAdded)) {
                path.add(cur);
                lastAdded = cur;
            }
        }
        return path;
    }

    // ==================== 步进控制 ====================

    public void stepForward() {
        if (currentStep < maxPathLen) {
            currentStep++;
            recordSnapshot();
        }
    }

    public void stepBackward() {
        if (currentStep > 0) {
            currentStep--;
        }
    }

    public void stepTo(int step) {
        currentStep = Math.max(0, Math.min(step, maxPathLen));
    }

    private void recordSnapshot() {
        Map<Integer, Position> snapshot = new LinkedHashMap<>();
        for (GameUnit u : myUnits.values()) {
            Position pos = getUnitPositionAtStep(u.getId(), currentStep);
            snapshot.put(u.getId(), pos);
        }
        if (stepSnapshots.size() > currentStep) {
            stepSnapshots.set(currentStep, snapshot);
        } else if (stepSnapshots.size() == currentStep) {
            stepSnapshots.add(snapshot);
        }
    }

    // ==================== 可视化数据查询 ====================

    public GameWorldState getWorld() { return world; }

    public Collection<GameUnit> getMyUnits() { return myUnits.values(); }
    public Collection<GameUnit> getEnemyUnits() { return enemyUnits.values(); }
    public Position getTarget() { return target; }

    public int getCurrentStep() { return currentStep; }
    public int getMaxPathLen() { return maxPathLen; }

    public double getAggressiveness() { return aggressiveness; }
    public void setAggressiveness(double v) { this.aggressiveness = Math.max(0, Math.min(1, v)); }

    public AIConfig getConfig() { return config; }

    public boolean hasPlan() { return fullPaths != null; }

    /** 获取某单位在当前步数的位置 */
    public Position getUnitPositionAtStep(int unitId, int step) {
        if (!myUnits.containsKey(unitId)) return null;
        if (step == 0) return myUnits.get(unitId).getPos();
        if (fullPaths == null || !fullPaths.containsKey(unitId)) return myUnits.get(unitId).getPos();
        List<Position> path = fullPaths.get(unitId);
        if (path.isEmpty()) return myUnits.get(unitId).getPos();
        int idx = Math.min(step - 1, path.size() - 1);
        return path.get(idx);
    }

    /** 获取某单位完整路径 */
    public List<Position> getUnitPath(int unitId) {
        if (fullPaths == null) return Collections.emptyList();
        return fullPaths.getOrDefault(unitId, Collections.emptyList());
    }

    /** 获取当前步数快照 */
    public Map<Integer, Position> getCurrentSnapshot() {
        if (stepSnapshots.isEmpty() || currentStep >= stepSnapshots.size()) {
            Map<Integer, Position> snap = new LinkedHashMap<>();
            for (GameUnit u : myUnits.values()) snap.put(u.getId(), u.getPos());
            return snap;
        }
        return stepSnapshots.get(currentStep);
    }

    /** 获取威胁图热度值 */
    public double getDanger(int x, int y) {
        if (coordinator == null || coordinator.getThreatMap() == null) return 0;
        return coordinator.getThreatMap().getDanger(x, y);
    }

    /** 获取预留表中某格子在某步数的预留单位 ID（-1 表示无预留） */
    public int getReservation(int step, int x, int y) {
        if (resTable == null || world == null) return -1;
        return resTable.getReservation(step, x, y, world.getWidth());
    }

    /** 获取 A* 搜索访问过的位置 */
    public Set<Position> getVisitedPositions() {
        if (coordinator == null || coordinator.getLastAStar() == null) return Collections.emptySet();
        return coordinator.getLastAStar().getVisitedPositions();
    }

    /** 获取地形静态障碍物 */
    public boolean isObstacle(int x, int y) {
        if (world == null) return false;
        return !world.isWalkable(x, y);
    }

    /** 获取指定位置的地形字符码（用于渲染） */
    public char getTerrainChar(int x, int y) {
        if (world == null || rawMapData == null) return '0';
        String[] tokens = rawMapData.split(",");
        // 游戏坐标 y=0 在地图底部，但 rawMapData 从顶部开始
        int mapY = world.getHeight() - 1 - y;
        int idx = mapY * world.getWidth() + x;
        if (idx < 0 || idx >= tokens.length) return '0';
        return tokens[idx].trim().isEmpty() ? '0' : tokens[idx].trim().charAt(0);
    }

    /** 获取指定位置的资源信息（如果有） */
    public String getResourceAt(int x, int y) {
        if (world == null) return null;
        return world.isWalkable(x, y) ? null : "obstacle";
    }

    /** 最大威胁值（用于颜色归一化） */
    public double getMaxDanger() {
        if (world == null) return 0;
        double max = 0;
        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                max = Math.max(max, getDanger(x, y));
            }
        }
        return max;
    }
}
