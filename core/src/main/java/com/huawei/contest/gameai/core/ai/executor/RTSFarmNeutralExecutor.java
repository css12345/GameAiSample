package com.huawei.contest.gameai.core.ai.executor;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import lombok.extern.slf4j.Slf4j;

/**
 * 开采中立资源执行器：矿工采矿，战斗单位清附近守护者。
 */
@Slf4j
public class RTSFarmNeutralExecutor extends AbstractGoalExecutor {
    @Override
    public void execute(Squad squad, IWorldState world, BaseBlackboard bb) {
        // 战斗单位攻击附近守护者
        for (IUnit attacker : squad.getCombatUnits()) {
            IUnit guardian = null;
            int bestDist = Integer.MAX_VALUE;
            for (IUnit u : world.getEnemyUnits()) {
                if (!u.isAlive() || !(u instanceof GameUnit gu)) continue;
                if (gu.type != UnitType.GUARDIAN) continue;
                int d = u.getPos().chebyshev(attacker.getPos());
                if (d < bestDist) { bestDist = d; guardian = u; }
            }
            if (guardian != null) {
                bb.getAssignedTargets().put(attacker.getId(), guardian.getId());
            }
        }
        // 矿工分配到最近资源（通过目标位置附近找资源——简化：战术层 MineResourceAction 处理）
    }
}
