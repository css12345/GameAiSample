package com.huawei.contest.gameai.base.client.movement;

import com.huawei.contest.gameai.base.client.AIConfig;
import com.huawei.contest.gameai.base.client.entity.GameUnit;
import com.huawei.contest.gameai.base.client.entity.GameWorldState;
import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.base.client.entity.UnitType;
import com.huawei.contest.gameai.base.client.model.ObjectIdRange;
import com.huawei.contest.gameai.base.client.model.Start;
import com.huawei.contest.gameai.base.client.model.StartMap;
import com.huawei.contest.gameai.base.client.model.StartPlayer;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class MovementCoordinatorStressTest {
    @Test
    void singleUnitShouldMoveTowardsTargetAlongOpenPath() {
        Start start = buildStart("0,0,1,0,9,a", 3, 2);


        AIConfig config = AIConfig.aggressiveRush();
        ReservationTable globalResTable = new ReservationTable();
        Set<Position> globalVacated = new HashSet<>();

        GameWorldState gameWorldState = GameWorldState.fromMapString(start, 1111);
        MovementCoordinator movementCoordinator = new MovementCoordinator(gameWorldState);

        GameUnit u = new GameUnit(101, 1111, UnitType.FIGHTER, Position.of(0, 0), 100);
        gameWorldState.getUnits().put(u.getId(), u);
        Map<Integer, Position> steps = movementCoordinator.planSquadMovement(List.of(u), Position.of(2, 1), Collections.emptyList(),  // 无敌人
                0.5, config, globalResTable, globalVacated);

        assertThat(steps).containsKey(101);
        Position next = steps.get(101);
        assertThat(next.getX()).isEqualTo(0);
        assertThat(next.getY()).isEqualTo(1);
        // 第一步不可能是原地（除非无路，但此处有路）
        assertThat(next).isNotEqualTo(u.getPos());
    }

    @Test
    void twoUnitShouldMoveTowardsTargetAlongOpenPath() {
        Start start = buildStart("0,0,1,0,9,a", 3, 2);


        AIConfig config = AIConfig.aggressiveRush();
        ReservationTable globalResTable = new ReservationTable();
        Set<Position> globalVacated = new HashSet<>();

        GameWorldState gameWorldState = GameWorldState.fromMapString(start, 1111);
        MovementCoordinator movementCoordinator = new MovementCoordinator(gameWorldState);

        GameUnit u1 = new GameUnit(101, 1111, UnitType.FIGHTER, Position.of(0, 0), 100);
        GameUnit u2 = new GameUnit(102, 1111, UnitType.FIGHTER, Position.of(0, 1), 100);
        gameWorldState.getUnits().put(u1.getId(), u1);
        Map<Integer, Position> steps = movementCoordinator.planSquadMovement(List.of(u1, u2), Position.of(2, 1), Collections.emptyList(),  // 无敌人
                0.5, config, globalResTable, globalVacated);

        assertThat(steps).containsKey(101);
        Position next1 = steps.get(101);
        assertThat(next1.getX()).isEqualTo(0);
        assertThat(next1.getY()).isEqualTo(1);
        // 第一步不可能是原地（除非无路，但此处有路）
        assertThat(next1).isNotEqualTo(u1.getPos());

        assertThat(steps).containsKey(102);
        Position next2 = steps.get(102);
        assertThat(next2.getX()).isEqualTo(1);
        assertThat(next2.getY()).isEqualTo(1);
        // 第一步不可能是原地（除非无路，但此处有路）
        assertThat(next2).isNotEqualTo(u2.getPos());
    }

    /**
     * 5 矿工从右下区域向 (25,28) 移动，验证密集编队移动。
     *
     * <h3>场景</h3>
     * 地图: default (40×40)，5 个矿工，无敌人，目标 (25,28)
     *
     * <h3>结论</h3>
     * maxSteps 默认为 2×(宽+高)=160，A* 在到达目标、超过 maxSteps 或节点预算耗尽时终止。
     * 之前 maxSteps=10 人为限制了搜索深度导致绕行失败，现已修复。
     * 若人工设回 10，仅最近单位能成功 → 验证了旧行为的 bug 性质。
     */
    @Test
    void fiveMinersMovingToTarget_shouldAllFindPaths() {
        String mapData = loadMapFromResource("maps/default.txt");
        assertThat(mapData).as("地图文件应可加载").isNotNull();

        Start start = buildStart(mapData, 40, 40);
        GameWorldState world = GameWorldState.fromMapString(start, 1111);

        AIConfig config = AIConfig.aggressiveRush();

        List<GameUnit> miners = List.of(
                new GameUnit(1004, 1111, UnitType.MINER, Position.of(34, 34), 70),
                new GameUnit(1005, 1111, UnitType.MINER, Position.of(35, 34), 70),
                new GameUnit(1006, 1111, UnitType.MINER, Position.of(36, 35), 70),
                new GameUnit(1007, 1111, UnitType.MINER, Position.of(35, 36), 70),
                new GameUnit(1008, 1111, UnitType.MINER, Position.of(31, 28), 70)
        );
        miners.forEach(u -> world.getUnits().put(u.getId(), u));
        world.refreshOccupied();

        Position target = Position.of(25, 28);

        // ===== 默认 maxSteps=2×(40+40)=160：全部应该成功 =====
        MovementCoordinator mc = new MovementCoordinator(world);
        Map<Integer, Position> steps = mc.planSquadMovement(
                new ArrayList<>(miners), target, Collections.emptyList(), 0.5, config,
                new ReservationTable(), new HashSet<>());

        long moved = steps.entrySet().stream()
                .filter(e -> {
                    GameUnit u = miners.stream().filter(m -> m.getId() == e.getKey()).findFirst().orElse(null);
                    return u != null && !e.getValue().equals(u.getPos());
                })
                .count();

        log.info("=== 默认 maxSteps=2×(40+40)=160 结果 ===");
        for (GameUnit m : miners) {
            Position next = steps.get(m.getId());
            boolean ok = next != null && !next.equals(m.getPos());
            log.info("  单位 {}@({},{}) → {}  {}",
                    m.getId(), m.getPos().getX(), m.getPos().getY(),
                    ok ? String.format("(%d,%d)", next.getX(), next.getY()) : "无法到达",
                    ok ? "✓" : "✗");
        }
        log.info("  成功移动: {}/{}", moved, miners.size());

        // 全部 5 个都应找到路径
        assertThat(moved).as("默认 maxSteps=2×(40+40)=160 下全部单位应找到路径").isEqualTo(5);

        // 验证终点不重叠：每个单位停在不同的格子上
        Set<Position> finalPositions = new HashSet<>(steps.values());
        assertThat(finalPositions.size()).as("全部单位第一步应不重叠").isEqualTo(miners.size());

        // ===== 人工设 maxSteps=10：复现旧行为的 bug =====
        MovementCoordinator mc10 = new MovementCoordinator(world);
        mc10.setMaxSteps(10);
        Map<Integer, Position> steps10 = mc10.planSquadMovement(
                new ArrayList<>(miners), target, Collections.emptyList(), 0.5, config,
                new ReservationTable(), new HashSet<>());

        long moved10 = steps10.entrySet().stream()
                .filter(e -> {
                    GameUnit u = miners.stream().filter(m -> m.getId() == e.getKey()).findFirst().orElse(null);
                    return u != null && !e.getValue().equals(u.getPos());
                })
                .count();

        log.info("=== (对比) 人为限制 maxSteps=10 结果 ===");
        for (GameUnit m : miners) {
            Position next = steps10.get(m.getId());
            boolean ok = next != null && !next.equals(m.getPos());
            log.info("  单位 {}@({},{}) → {}  {}",
                    m.getId(), m.getPos().getX(), m.getPos().getY(),
                    ok ? String.format("(%d,%d)", next.getX(), next.getY()) : "无法到达",
                    ok ? "✓" : "✗");
        }
        log.info("  成功移动: {}/{} (仅最近单位成功)", moved10, miners.size());

        // maxSteps=10 时仅有最近单位成功（旧行为确认为 bug）
        assertThat(moved10).as("maxSteps=10时仅最近单位成功").isEqualTo(1);
    }

    /** 从 classpath 资源加载地图文件 */
    private static String loadMapFromResource(String resourcePath) {
        InputStream is = MovementCoordinatorStressTest.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            log.warn("资源未找到: {}, 尝试绝对路径", resourcePath);
            is = MovementCoordinatorStressTest.class.getResourceAsStream("/" + resourcePath);
        }
        if (is == null) return null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line.trim());
            return sb.toString();
        } catch (Exception e) {
            log.error("加载地图资源失败: {}", resourcePath, e);
            return null;
        }
    }

    private static @NonNull Start buildStart(String data, int maxX, int maxY) {
        Start start = new Start();

        StartMap startMap = new StartMap();
        startMap.setData(data);
        startMap.setMaxX(maxX);
        startMap.setMaxY(maxY);
        start.setMap(startMap);

        StartPlayer startPlayer1= new StartPlayer();
        startPlayer1.setPlayerId(1111);
        ObjectIdRange objectIdRange1 = new ObjectIdRange();
        objectIdRange1.setMax(200);
        objectIdRange1.setMin(100);
        startPlayer1.setObjectIdRange(objectIdRange1);

        StartPlayer startPlayer2= new StartPlayer();
        startPlayer2.setPlayerId(2222);
        ObjectIdRange objectIdRange2 = new ObjectIdRange();
        objectIdRange2.setMax(600);
        objectIdRange2.setMin(500);
        startPlayer2.setObjectIdRange(objectIdRange2);
        start.setPlayers(List.of(startPlayer1, startPlayer2));
        return start;
    }

    /**
     * 验证多个单位到达目标附近时不会停在同一个格子上。
     *
     * <p>修复前：A* 终止条件 chebyshevDist≤1 允许不同单位在不同 step
     * 停在相同终点格，导致移动完成后单位重叠。
     *
     * <p>修复后：单位到达后，终点格从到达步数+1 到 maxSteps 全部预留，
     * 迫使后来者选择不同的相邻格。
     */
    @Test
    void multipleUnitsNearTarget_shouldNotOverlap() {
        // 开放地图 6×3，目标在 (3,1)，3 个单位从右侧不同位置出发
        Start start = buildStart("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", 6, 3);
        GameWorldState world = GameWorldState.fromMapString(start, 1111);

        List<GameUnit> units = List.of(
                new GameUnit(101, 1111, UnitType.FIGHTER, Position.of(5, 1), 100),
                new GameUnit(102, 1111, UnitType.FIGHTER, Position.of(5, 0), 100),
                new GameUnit(103, 1111, UnitType.FIGHTER, Position.of(5, 2), 100)
        );
        units.forEach(u -> world.getUnits().put(u.getId(), u));
        world.refreshOccupied();

        Position target = Position.of(3, 1);
        MovementCoordinator mc = new MovementCoordinator(world);
        mc.setMaxSteps(20); // 足够大
        ReservationTable rt = new ReservationTable();

        Map<Integer, Position> firstSteps = mc.planSquadMovement(
                new ArrayList<>(units), target, Collections.emptyList(), 0.0,
                AIConfig.aggressiveRush(), rt, new HashSet<>());

        // 从预留表中找到每个单位的最终位置（最高的 step 对应的位置）
        Map<Integer, Position> finalPositions = new HashMap<>();
        for (GameUnit u : units) {
            Position lastPos = u.getPos(); // 兜底
            for (int s = 20; s >= 1; s--) {
                for (int x = 0; x < 6; x++) {
                    for (int y = 0; y < 3; y++) {
                        if (rt.getReservation(s, x, y, 6) == u.getId()) {
                            lastPos = Position.of(x, y);
                            // 找到了最高 step，跳出
                            s = 0;
                            x = 6;
                            y = 3;
                        }
                    }
                }
            }
            finalPositions.put(u.getId(), lastPos);
        }

        log.info("单位终点位置:");
        for (GameUnit u : units) {
            Position end = finalPositions.get(u.getId());
            log.info("  单位 {}: 起点({},{}) → 终点({},{}), 距离目标={}",
                    u.getId(), u.getPos().getX(), u.getPos().getY(),
                    end.getX(), end.getY(), end.chebyshev(target));
        }

        // 核心断言：每个单位的终点应不同
        Set<Position> uniqueEndpoints = new HashSet<>(finalPositions.values());
        assertThat(uniqueEndpoints.size()).as("每个单位终点位置应唯一")
                .isEqualTo(units.size());

        // 所有终点应在目标距离 ≤1 内
        for (Position end : finalPositions.values()) {
            assertThat(end.chebyshev(target)).as("终点应在目标距离1内").isLessThanOrEqualTo(1);
        }

        // 第一步也不应重叠
        Set<Position> uniqueFirstSteps = new HashSet<>(firstSteps.values());
        assertThat(uniqueFirstSteps.size()).as("第一个步位置应唯一").isEqualTo(units.size());
    }

}
