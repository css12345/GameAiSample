package com.huawei.contest.gameai.core.ai;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.base.client.model.RoleInformation;
import com.huawei.contest.gameai.base.client.model.Start;
import com.huawei.contest.gameai.core.ai.action.*;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import com.huawei.contest.gameai.core.ai.campaign.SquadState;
import com.huawei.contest.gameai.core.ai.strategy.GoalType;
import com.huawei.contest.gameai.core.ai.strategy.StrategicGoal;
import com.huawei.contest.gameai.core.ai.tactical.RTSUnitAgent;
import com.huawei.contest.gameai.core.ai.tactical.UnitAgent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 战术层测试：行为树节点在各种情境下产出正确动作。
 */
class TacticalLayerTest {
    private final int myId = 100001, oppId = 200001;
    private final UnitAgent agent = new RTSUnitAgent();

    private GameWorldState buildWorld(List<RoleInformation> objs) {
        Start start = TestFixture.buildOpenMap(myId, oppId);
        GameWorldState w = GameWorldState.fromMapString(start, myId);
        w.loadInquireData(TestFixture.inquire(1, objs));
        w.refreshOccupied();
        return w;
    }

    private Squad squadWithGoal(GoalType type, Position target) {
        Squad s = new Squad(1);
        s.setGoal(new StrategicGoal(type, target, 3, 1.0));
        return s;
    }

    @Test
    void lowHpFighter_shouldRetreat() {
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "fighter", 20, 20, 10)  // 10/150 < 20%
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        bb.setMyBasePos(w.getMyBases().get(0).getPos());
        IUnit self = w.getMyUnits().get(0);
        RtsAction a = agent.decide(self, w, bb, null);
        assertThat(a).isInstanceOf(MoveAction.class);
        // 应朝基地方向
        MoveAction m = (MoveAction) a;
        assertThat(m.position[0]).isEqualTo(35);
    }

    @Test
    void fighterAdjacentEnemy_shouldAttack() {
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "fighter", 20, 20, 150),
                TestFixture.unit(200002, "fighter", 21, 20, 150)  // 邻接
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        bb.setMyBasePos(w.getMyBases().get(0).getPos());
        IUnit self = w.getMyUnits().get(0);
        // 放入战斗任务小队（EXECUTING），跳过规避分支，触发攻击最近
        Squad s = squadWithGoal(GoalType.ATTACK, Position.of(21, 20));
        s.addUnit(self);
        s.getFsm().changeState(SquadState.EXECUTING);
        RtsAction a = agent.decide(self, w, bb, s);
        assertThat(a).isInstanceOf(AttackAction.class);
        assertThat(((AttackAction) a).targetId).isEqualTo(200002);
    }

    @Test
    void medic_shouldHealLowHpAlly() {
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "medic", 20, 20, 80),
                TestFixture.unit(100003, "fighter", 21, 20, 20)  // 20/150 低血
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        bb.setMyBasePos(w.getMyBases().get(0).getPos());
        IUnit medic = w.getMyUnits().stream()
                .filter(u -> ((GameUnit) u).type == UnitType.MEDIC).findFirst().orElseThrow();
        RtsAction a = agent.decide(medic, w, bb, null);
        assertThat(a).isInstanceOf(HealAction.class);
        assertThat(((HealAction) a).targetId).isEqualTo(100003);
    }

    @Test
    void minerAdjacentResource_shouldPick() {
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "miner", 5, 5, 70),
                TestFixture.resource(1, "goldmine", 6, 5, 500)  // 邻接
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        bb.setMyBasePos(w.getMyBases().get(0).getPos());
        IUnit miner = w.getMyUnits().get(0);
        RtsAction a = agent.decide(miner, w, bb, null);
        assertThat(a).isInstanceOf(PickAction.class);
        assertThat(((PickAction) a).targetId).isEqualTo(1);
    }

    @Test
    void guardianMission_shouldKeepDistance() {
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "rocket", 20, 20, 175),
                TestFixture.unit(4, "guardian", 23, 20, 800)  // 距离3
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        bb.setMyBasePos(w.getMyBases().get(0).getPos());
        IUnit rocket = w.getMyUnits().get(0);
        // 守护者 id=4，position=(23,20)
        Squad s = squadWithGoal(GoalType.CLEAR_GUARDIAN, Position.of(23, 20));
        s.getGoal().targetEntityId = 4;
        s.addUnit(rocket);
        // 强制 EXECUTING 状态以触发清野分支
        s.getFsm().changeState(SquadState.EXECUTING);
        RtsAction a = agent.decide(rocket, w, bb, s);
        // 火箭射程2，守护者距离3 → 范围外应靠近(MoveAction) 或范围内 AttackAction
        assertThat(a).isInstanceOfAny(MoveAction.class, AttackAction.class);
    }
}
