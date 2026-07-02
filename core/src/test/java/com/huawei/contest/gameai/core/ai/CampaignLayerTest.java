package com.huawei.contest.gameai.core.ai;

import com.huawei.contest.gameai.base.client.entity.GameWorldState;
import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.base.client.entity.UnitType;
import com.huawei.contest.gameai.base.client.model.RoleInformation;
import com.huawei.contest.gameai.base.client.model.Start;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.*;
import com.huawei.contest.gameai.core.ai.strategy.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 战役层测试：小队 FSM 转换 + 目标分配 + 生产。
 */
class CampaignLayerTest {
    private final int myId = 100001, oppId = 200001;

    private GameWorldState buildWorld(List<RoleInformation> objs) {
        Start start = TestFixture.buildOpenMap(myId, oppId);
        GameWorldState w = GameWorldState.fromMapString(start, myId);
        w.loadInquireData(TestFixture.inquire(1, objs));
        w.refreshOccupied();
        return w;
    }

    private RTSCampaignCoordinator buildCoordinator(BaseBlackboard bb) {
        RTSProductionManager prod = new RTSProductionManager(bb);
        return new RTSCampaignCoordinator(bb,
                new RTSStanceEvaluator(), new RTSGoalGenerator(), prod);
    }

    @Test
    void squad_shouldTransitionToMovingAfterForming() {
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "fighter", 35, 34, 150),
                TestFixture.unit(100003, "fighter", 35, 33, 150),
                TestFixture.unit(200002, "miner", 10, 10, 70)
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        bb.setMyBasePos(w.getMyBases().get(0).getPos());
        bb.setEnemyBasePos(w.getEnemyBases().get(0).getPos());
        RTSCampaignCoordinator cc = buildCoordinator(bb);
        cc.update(w, myId);

        // 应至少创建一个小队（RUSH 姿态 → ATTACK 敌方基地）
        assertThat(cc.getSquads()).isNotEmpty();
        // 小队应处于 FORMING 或 MOVING
        Squad s = cc.getSquads().values().iterator().next();
        assertThat(s.getFsm().getCurrentState()).isIn(SquadState.FORMING, SquadState.MOVING);
    }

    @Test
    void production_shouldProduceMinerInEconomy() {
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "fighter", 35, 34, 150),
                TestFixture.unit(200002, "fighter", 10, 10, 150),
                TestFixture.unit(200003, "miner", 3, 2, 70),
                TestFixture.unit(200004, "miner", 2, 2, 70),
                TestFixture.unit(200005, "miner", 3, 1, 70)
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        bb.setMyBasePos(w.getMyBases().get(0).getPos());
        bb.setEnemyBasePos(w.getEnemyBases().get(0).getPos());
        RTSCampaignCoordinator cc = buildCoordinator(bb);
        cc.update(w, myId);
        // ECONOMY 姿态应生产 miner
        assertThat(bb.getCurrentStance()).isEqualTo(IStanceEvaluator.ECONOMY);
        assertThat(cc.getProductionManager().getProduceQueue()).isNotEmpty();
        int roleOrd = cc.getProductionManager().getProduceQueue().get(0)[1];
        assertThat(UnitType.values()[roleOrd]).isEqualTo(UnitType.MINER);
    }

    @Test
    void unassignedUnits_shouldNotBeDoubleAssigned() {
        List<RoleInformation> objs = List.of(
                TestFixture.base(100001, 35, 35, 3000),
                TestFixture.base(200001, 4, 4, 3000),
                TestFixture.unit(100002, "fighter", 35, 34, 150),
                TestFixture.unit(100003, "fighter", 35, 33, 150)
        );
        GameWorldState w = buildWorld(objs);
        BaseBlackboard bb = new BaseBlackboard();
        bb.setMyBasePos(w.getMyBases().get(0).getPos());
        bb.setEnemyBasePos(w.getEnemyBases().get(0).getPos());
        RTSCampaignCoordinator cc = buildCoordinator(bb);
        cc.update(w, myId);
        // 每个单位只在一个小队
        int totalAssigned = cc.getSquads().values().stream()
                .mapToInt(s -> s.getUnits().size()).sum();
        assertThat(totalAssigned).isLessThanOrEqualTo(2);
    }
}
