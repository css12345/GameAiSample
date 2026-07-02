package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 小队处于 MOVING 状态 */
public class SquadMovingCondition extends ConditionTask {
    @Override protected boolean check(UnitContext ctx) {
        return ctx.getSquad() != null && ctx.getSquad().isMoving();
    }
}
