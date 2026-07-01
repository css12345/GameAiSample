package com.huawei.contest.gameai.core.ai.executor;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import lombok.extern.slf4j.Slf4j;

/**
 * 骚扰执行器：优先攻击敌方矿工/医疗（高经济价值目标）。
 */
@Slf4j
public class RTSHarassExecutor extends AbstractGoalExecutor {
    @Override
    public void execute(Squad squad, IWorldState world, BaseBlackboard bb) {
        for (IUnit attacker : squad.getCombatUnits()) {
            IUnit best = null;
            int bestDist = Integer.MAX_VALUE;
            for (IUnit u : world.getEnemyUnits()) {
                if (!u.isAlive() || !(u instanceof GameUnit gu)) continue;
                if (gu.type != UnitType.MINER && gu.type != UnitType.MEDIC) continue;
                int d = u.getPos().chebyshev(attacker.getPos());
                if (d < bestDist) { bestDist = d; best = u; }
            }
            // 退而求其次：任意敌人
            if (best == null) best = nearestEnemy(world, attacker.getPos());
            if (best != null) {
                bb.getAssignedTargets().put(attacker.getId(), best.getId());
            }
        }
    }
}
