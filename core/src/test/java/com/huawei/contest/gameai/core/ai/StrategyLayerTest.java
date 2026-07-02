package com.huawei.contest.gameai.core.ai;

import com.huawei.contest.gameai.base.client.entity.GameWorldState;
import com.huawei.contest.gameai.base.client.model.RoleInformation;
import com.huawei.contest.gameai.base.client.model.Start;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.strategy.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 战略层测试：姿态评估 + 目标生成。
 */
class StrategyLayerTest {
    private final int myId = 100001, oppId = 200001;

    private GameWorldState buildWorld(List<RoleInformation> objs) {
        Start start = TestFixture.buildOpenMap(myId, oppId);
        GameWorldState w = GameWorldState.fromMapString(start, myId);
        var inq = TestFixture.inquire(1, objs);
        w.loadInquireData(inq);
        w.refreshOccupied();
        return w;
    }

    @Test
    void baseEmergency_shouldReturnDefend() {
        // 己方基地残血
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 500),   // 3000max, 500<30%
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "fighter", 35, 34, 150),
                TestFixture.unit(200002, "fighter", 10, 10, 150)
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        String stance = new RTSStanceEvaluator().evaluate(w, myId, bb);
        assertThat(stance).isEqualTo(IStanceEvaluator.DEFEND);
    }

    @Test
    void armyAdvantage_shouldReturnRush() {
        // 我方5兵 vs 敌方0兵
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "fighter", 35, 34, 150),
                TestFixture.unit(100003, "fighter", 35, 33, 150),
                TestFixture.unit(100004, "rocket", 35, 32, 175),
                TestFixture.unit(200002, "miner", 10, 10, 70)
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        String stance = new RTSStanceEvaluator().evaluate(w, myId, bb);
        assertThat(stance).isEqualTo(IStanceEvaluator.RUSH);
    }

    @Test
    void econDisadvantage_shouldReturnEconomy() {
        // 我方0矿工 vs 敌方5矿工，双方各1兵(均势) → ECONOMY
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "fighter", 35, 34, 150),
                TestFixture.unit(200002, "fighter", 10, 10, 150),
                TestFixture.unit(200003, "miner", 3, 2, 70),
                TestFixture.unit(200004, "miner", 2, 2, 70),
                TestFixture.unit(200005, "miner", 3, 1, 70),
                TestFixture.unit(200006, "miner", 2, 3, 70),
                TestFixture.unit(200007, "miner", 1, 3, 70)
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        String stance = new RTSStanceEvaluator().evaluate(w, myId, bb);
        assertThat(stance).isEqualTo(IStanceEvaluator.ECONOMY);
    }

    @Test
    void enemyBaseLowHp_shouldGenerateFinishingBlow() {
        // 敌方基地残血 → ATTACK + forcePath
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 400),  // <20%
                TestFixture.unit(100002, "fighter", 35, 34, 150),
                TestFixture.unit(200002, "miner", 10, 10, 70)
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        bb.setMyBasePos(w.getMyBases().get(0).getPos());
        bb.setEnemyBasePos(w.getEnemyBases().get(0).getPos());
        List<StrategicGoal> goals = new RTSGoalGenerator().generate(w, myId, IStanceEvaluator.RUSH, bb);
        assertThat(goals).isNotEmpty();
        StrategicGoal g = goals.get(0);
        assertThat(g.getType()).isEqualTo(GoalType.ATTACK);
        assertThat(g.isForcePath()).isTrue();
    }

    @Test
    void guardianNearPath_shouldGenerateClearGuardianGoal() {
        // 守护者在两基地之间 → CLEAR_GUARDIAN + requiredRoles
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "fighter", 35, 34, 150),
                TestFixture.unit(4, "guardian", 20, 20, 800)  // 中间守护者
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        bb.setMyBasePos(w.getMyBases().get(0).getPos());
        bb.setEnemyBasePos(w.getEnemyBases().get(0).getPos());
        List<StrategicGoal> goals = new RTSGoalGenerator().generate(w, myId, IStanceEvaluator.RUSH, bb);
        // 应包含 CLEAR_GUARDIAN 目标
        boolean hasClear = goals.stream().anyMatch(g -> g.getType() == GoalType.CLEAR_GUARDIAN);
        assertThat(hasClear).isTrue();
        // 验证 requiredRoles
        StrategicGoal clear = goals.stream()
                .filter(g -> g.getType() == GoalType.CLEAR_GUARDIAN).findFirst().orElseThrow();
        assertThat(clear.getRequiredRoles()).isNotNull();
        assertThat(clear.getRequiredRoles()).containsKey(com.huawei.contest.gameai.base.client.entity.UnitType.ROCKET);
        assertThat(clear.getRequiredRoles()).containsKey(com.huawei.contest.gameai.base.client.entity.UnitType.MEDIC);
    }
}
