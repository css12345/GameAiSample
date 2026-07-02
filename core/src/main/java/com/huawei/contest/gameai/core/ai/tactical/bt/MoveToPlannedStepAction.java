package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.core.ai.action.MoveAction;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 跟随小队移动计划的下一步 */
public class MoveToPlannedStepAction extends ActionTask {
    @Override
    protected RtsAction doAction(UnitContext ctx) {
        return plannedStep(ctx);
    }

    public static RtsAction plannedStep(UnitContext ctx) {
        if (ctx.getSquad() == null) return null;
        Position next = ctx.getSquad().getMovementPlan().get(ctx.getSelf().getId());
        if (next == null || next.equals(ctx.getSelf().getPos())) return null;
        return new MoveAction(ctx.getSelf().getId(), next.getX(), next.getY());
    }
}
