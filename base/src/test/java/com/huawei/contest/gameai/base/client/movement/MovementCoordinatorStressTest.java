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
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

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

}
