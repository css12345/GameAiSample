package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.base.client.entity.GameResource;
import com.huawei.contest.gameai.base.client.entity.GameWorldState;
import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.core.ai.action.PickAction;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 采矿：邻接矿石则 pick，否则无动作（由移动计划靠近） */
public class MineResourceAction extends ActionTask {
    @Override
    protected RtsAction doAction(UnitContext ctx) {
        if (!(ctx.getWorld() instanceof GameWorldState gws)) return null;
        // 找邻接的活资源
        com.huawei.contest.gameai.base.client.entity.Position p = ctx.getSelf().getPos();
        for (var res : gws.getResources().values()) {
            if (!res.isAlive()) continue;
            if (res.getPos().chebyshev(p) <= 1) {
                return new PickAction(ctx.getSelf().getId(), res.getId());
            }
        }
        return null;
    }
}
