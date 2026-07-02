package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 范围内有敌人（默认 5 格）且非战斗/非撤退 */
public class EnemyInRangeCondition extends ConditionTask {
    private final int range;
    public EnemyInRangeCondition() { this(5); }
    public EnemyInRangeCondition(int range) { this.range = range; }

    @Override protected boolean check(UnitContext ctx) {
        if (ctx.isInCombatMission() || ctx.isRetreating()) return false;
        for (IUnit u : ctx.getWorld().getEnemyUnits()) {
            if (u.isAlive() && u.getPos().chebyshev(ctx.getSelf().getPos()) <= range) {
                return true;
            }
        }
        return false;
    }
}
