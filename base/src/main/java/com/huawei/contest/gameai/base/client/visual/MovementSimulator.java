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
    private Position target; // 全局目标（未编队时使用）

    // ==================== 编队管理 ====================
    public record SquadInfo(int id, String name, Position target, java.awt.Color color) {}
    private final Map<Integer, SquadInfo> squads = new LinkedHashMap<>();
    private final Map<Integer, Integer> unitToSquad = new HashMap<>(); // unitId → squadId
    private int selectedSquadId = 0; // 0=未编队
    private int nextSquadId = 1;
    private static final java.awt.Color[] SQUAD_COLORS = {
            new java.awt.Color(0xE6, 0x19, 0x4B), // 红
            new java.awt.Color(0x3C, 0xB4, 0x4B), // 绿
            new java.awt.Color(0x42, 0x8B, 0xFF), // 蓝
            new java.awt.Color(0xF5, 0x82, 0x31), // 橙
            new java.awt.Color(0x91, 0x1E, 0xB4), // 紫
            new java.awt.Color(0x00, 0xCD, 0xCD), // 青
    };

    public SquadInfo createSquad(String name) {
        int id = nextSquadId++;
        SquadInfo s = new SquadInfo(id, name, null,
                SQUAD_COLORS[(id - 1) % SQUAD_COLORS.length]);
        squads.put(id, s);
        return s;
    }
    public void removeSquad(int squadId) {
        squads.remove(squadId);
        unitToSquad.values().removeIf(v -> v == squadId);
    }
    public void assignToSquad(int unitId, int squadId) {
        if (squads.containsKey(squadId)) unitToSquad.put(unitId, squadId);
        else unitToSquad.remove(unitId);
    }
    public void setSquadTarget(int squadId, int x, int y) {
        SquadInfo s = squads.get(squadId);
        if (s != null) squads.put(squadId, new SquadInfo(s.id, s.name, Position.of(x, y), s.color));
    }
    public SquadInfo getSquad(int squadId) { return squads.get(squadId); }
    public Collection<SquadInfo> getSquads() { return squads.values(); }
    public int getUnitSquad(int unitId) { return unitToSquad.getOrDefault(unitId, 0); }
    public int getSelectedSquadId() { return selectedSquadId; }
    public void setSelectedSquadId(int id) { this.selectedSquadId = id; }

    // ==================== 单位选择 ====================
    private int selectedUnitId = -1;
    public int getSelectedUnitId() { return selectedUnitId; }
    public void setSelectedUnitId(int id) { this.selectedUnitId = id; }
    public GameUnit getSelectedUnit() { return selectedUnitId >= 0 ? myUnits.get(selectedUnitId) : null; }
    /** 选中指定格子的己方单位，返回是否选中成功 */
    public boolean selectUnitAt(int x, int y) {
        for (GameUnit u : myUnits.values()) {
            if (u.getPos().getX() == x && u.getPos().getY() == y) {
                selectedUnitId = u.getId();
                return true;
            }
        }
        selectedUnitId = -1;
        return false;
    }
    /** 获取编队成员列表 */
    public List<GameUnit> getSquadMembers(int squadId) {
        return myUnits.values().stream()
                .filter(u -> getUnitSquad(u.getId()) == squadId)
                .collect(Collectors.toList());
    }

    // ==================== 移动规划结果 ====================
    private MovementCoordinator coordinator;
    private ReservationTable resTable;
    private Set<Position> vacated;
    private Map<Integer, List<Position>> fullPaths;
    private Map<Integer, Position> firstSteps;
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
        // 解析地图中的守护者作为威胁源
        parseGuardians();
    }

    /** 扫描地图数据中的守护者('6')，加入敌方列表作为威胁源 */
    private void parseGuardians() {
        if (world == null || rawMapData == null) return;
        String[] tokens = rawMapData.split(",");
        int h = world.getHeight();
        int w = world.getWidth();
        for (int mapY = 0; mapY < h; mapY++) {
            int worldY = h - 1 - mapY;
            for (int worldX = 0; worldX < w; worldX++) {
                int idx = mapY * w + worldX;
                if (idx < tokens.length && tokens[idx].trim().equals("6")) {
                    // 创建守护者单位（playerId=-1 表示中立），用于威胁图计算
                    int gid = ID_GEN.incrementAndGet();
                    GameUnit guardian = new GameUnit(gid, -1, UnitType.GUARDIAN,
                            Position.of(worldX, worldY), UnitType.GUARDIAN.getMaxHp());
                    enemyUnits.put(gid, guardian);
                }
            }
        }
        syncUnitsToWorld();
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

    /** 按编队规划所有单位移动。有编队时按编队分目标，否则全部去全局目标。 */
    public void planMovement() {
        if (world == null || myUnits.isEmpty()) return;

        resTable.clear();
        vacated.clear();
        fullPaths = new LinkedHashMap<>();
        firstSteps = new LinkedHashMap<>();
        currentStep = 0;
        maxPathLen = 0;
        stepSnapshots.clear();

        syncUnitsToWorld();
        coordinator = new MovementCoordinator(world);
        List<IUnit> enemies = new ArrayList<>(enemyUnits.values());

        log.info("========== 移动规划开始 ==========");
        log.info("地图: {}x{}, 攻击性: {}, maxSteps: {}",
                world.getWidth(), world.getHeight(),
                String.format("%.2f", aggressiveness), coordinator.getMaxSteps());

        // 收集有目标的编队
        List<SquadInfo> activeSquads = squads.values().stream()
                .filter(s -> s.target != null)
                .collect(Collectors.toList());

        if (!activeSquads.isEmpty()) {
            // ===== 编队模式：每个编队独立规划，共享预留表 =====
            for (SquadInfo squad : activeSquads) {
                List<GameUnit> squadUnits = myUnits.values().stream()
                        .filter(u -> getUnitSquad(u.getId()) == squad.id)
                        .collect(Collectors.toList());
                if (squadUnits.isEmpty()) continue;

                log.info("--- 编队[{}] \"{}\" → ({},{}) {} 个单位 ---",
                        squad.id, squad.name, squad.target.getX(), squad.target.getY(), squadUnits.size());
                planOneSquad(squadUnits, squad.target, enemies);
            }

            // 未编队的单位去全局目标
            List<GameUnit> unassigned = myUnits.values().stream()
                    .filter(u -> getUnitSquad(u.getId()) == 0)
                    .collect(Collectors.toList());
            if (!unassigned.isEmpty() && target != null) {
                log.info("--- 未编队单位 → ({},{}) {} 个单位 ---",
                        target.getX(), target.getY(), unassigned.size());
                planOneSquad(unassigned, target, enemies);
            }
        } else if (target != null) {
            // ===== 无编队模式：全部去全局目标 =====
            log.info("目标位置: ({}, {})", target.getX(), target.getY());
            List<GameUnit> allUnits = new ArrayList<>(myUnits.values());
            log.info("己方单位 ({}个)", allUnits.size());
            log.info("敌方单位 ({}个)", enemies.size());
            planOneSquad(allUnits, target, enemies);
        }

        log.info("最长路径: {} 步", maxPathLen);
        log.info("========== 移动规划结束 ==========");
        recordSnapshot();
    }

    /** 规划一个编队/组到指定目标 */
    private void planOneSquad(List<GameUnit> units, Position targetPos, List<IUnit> enemies) {
        List<IUnit> iunits = new ArrayList<>(units);
        Map<Integer, Position> steps = coordinator.planSquadMovement(
                iunits, targetPos, enemies, aggressiveness, config, resTable, vacated);
        firstSteps.putAll(steps);

        for (GameUnit u : units) {
            List<Position> path = new ArrayList<>();
            Position next = steps.get(u.getId());
            if (next != null) {
                path = reconstructPathFromReservations(u.getId());
                if (path.isEmpty() && !next.equals(u.getPos())) {
                    path.add(next);
                }
                maxPathLen = Math.max(maxPathLen, path.size());
            }
            fullPaths.put(u.getId(), path);
            logUnitPath(u, path);
        }
    }

    private void logUnitPath(GameUnit u, List<Position> path) {
        boolean realMove = !path.isEmpty() && !(path.size() == 1 && path.get(0).equals(u.getPos()));
        if (realMove) {
            int maxShow = 30;
            String pathStr;
            if (path.size() <= maxShow) {
                pathStr = path.stream().map(p -> String.format("(%d,%d)", p.getX(), p.getY()))
                        .collect(Collectors.joining(" → "));
            } else {
                String head = path.subList(0, 15).stream().map(p -> String.format("(%d,%d)", p.getX(), p.getY()))
                        .collect(Collectors.joining(" → "));
                String tail = path.subList(path.size() - 3, path.size()).stream().map(p -> String.format("(%d,%d)", p.getX(), p.getY()))
                        .collect(Collectors.joining(" → "));
                pathStr = head + " → ...(" + (path.size() - 18) + "步)... → " + tail;
            }
            String waitNote = path.get(0).equals(u.getPos()) ? " (第1步原地等待)" : "";
            int sid = getUnitSquad(u.getId());
            String tag = sid > 0 ? "[编队" + sid + "] " : "";
            log.info("{}单位 {} ({}id={}) 路径 ({}步){}: 起点({},{}) → {}",
                    tag, u.type.name(), u.getPlayerId() == myPlayerId ? "" : "敌",
                    u.getId(), path.size(), waitNote, u.getPos().getX(), u.getPos().getY(), pathStr);
        } else {
            log.info("单位 {} (id={}) 完全无法移动，保持在 ({},{})",
                    u.type.name(), u.getId(), u.getPos().getX(), u.getPos().getY());
        }
    }

    /** 从预留表中重建单位路径（跳过 step=0 起始位置 + 连续相同位置） */
    private List<Position> reconstructPathFromReservations(int unitId) {
        List<Position> path = new ArrayList<>();
        Position lastAdded = null;
        for (var entry : resTable.getUnitReservations(unitId, world.getWidth())) {
            if (entry.getKey() == 0) continue; // step=0 是预占的起始位置，不是移动步骤
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
