package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 小队移动计划中有 self 的下一步 */
public class HasPlannedStepCondition extends ConditionTask {
    @Override protected boolean check(UnitContext ctx) {
        if (ctx.getSquad() == null) return false;
        return ctx.getSquad().getMovementPlan().containsKey(ctx.getSelf().getId());
    }
}
