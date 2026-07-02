package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 低血量：战斗中 <10%，其他 <20% */
public class LowHpCondition extends ConditionTask {
    @Override protected boolean check(UnitContext ctx) {
        double pct = ctx.hpPercent();
        double threshold = ctx.isInCombatMission()
                ? ctx.getBb().getConfig().getCombatLowHpPercent()
                : ctx.getBb().getConfig().getNormalLowHpPercent();
        return pct < threshold;
    }
}
