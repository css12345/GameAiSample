package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.core.ai.action.MoveAction;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 规避：移到安全邻格 */
public class EvadeAction extends ActionTask {
    @Override
    protected RtsAction doAction(UnitContext ctx) {
        com.huawei.contest.gameai.base.client.entity.Position safe = CanEvadeCondition.findSafeNeighbor(ctx);
        if (safe == null) return null;
        return new MoveAction(ctx.getSelf().getId(), safe.getX(), safe.getY());
    }
}
