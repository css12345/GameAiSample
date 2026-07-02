package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.core.ai.action.AttackAction;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 攻击邻接（切比雪夫≤攻击范围）的最近敌人 */
public class AttackNearestEnemyAction extends ActionTask {
    @Override
    protected RtsAction doAction(UnitContext ctx) {
        IUnit nearest = null;
        int bestDist = Integer.MAX_VALUE;
        for (IUnit u : ctx.getWorld().getEnemyUnits()) {
            if (!u.isAlive()) continue;
            int d = u.getPos().chebyshev(ctx.getSelf().getPos());
            if (d <= ctx.getSelf().getAttackRange() && d < bestDist) {
                bestDist = d;
                nearest = u;
            }
        }
        if (nearest == null) return null;
        return new AttackAction(ctx.getSelf().getId(), nearest.getId());
    }
}
