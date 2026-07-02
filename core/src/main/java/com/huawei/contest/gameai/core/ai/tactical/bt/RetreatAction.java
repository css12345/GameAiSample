package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.base.client.entity.IBase;
import com.huawei.contest.gameai.core.ai.action.MoveAction;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 撤退到己方基地附近一步 */
public class RetreatAction extends ActionTask {
    @Override
    protected RtsAction doAction(UnitContext ctx) {
        if (ctx.getBb().getMyBasePos() != null) {
            // 简化：直接朝基地移动一步（实际用移动计划或寻路）
            return new MoveAction(ctx.getSelf().getId(),
                    ctx.getBb().getMyBasePos().getX(), ctx.getBb().getMyBasePos().getY());
        }
        // 无基地位置，跟随小队移动计划
        if (ctx.getSquad() != null) {
            return MoveToPlannedStepAction.plannedStep(ctx);
        }
        return null;
    }
}
