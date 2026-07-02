package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.core.ai.action.MoveAction;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 默认：向小队目标点移动一步（简化：直接 move 到目标点坐标） */
public class MoveToFormationAction extends ActionTask {
    @Override
    protected RtsAction doAction(UnitContext ctx) {
        Position target = null;
        if (ctx.getSquad() != null && ctx.getSquad().getGoal() != null) {
            target = ctx.getSquad().getGoal().getTargetPos();
        }
        if (target == null && ctx.getBb().getMyBasePos() != null) {
            target = ctx.getBb().getMyBasePos();
        }
        if (target == null) return null;
        if (target.equals(ctx.getSelf().getPos())) return null;
        return new MoveAction(ctx.getSelf().getId(), target.getX(), target.getY());
    }
}
