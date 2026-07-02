package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 能规避：有安全邻格（非威胁/非障碍） */
public class CanEvadeCondition extends ConditionTask {
    @Override protected boolean check(UnitContext ctx) {
        return findSafeNeighbor(ctx) != null;
    }

    public static Position findSafeNeighbor(UnitContext ctx) {
        Position p = ctx.getSelf().getPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = p.getX() + dx, ny = p.getY() + dy;
                if (ctx.getWorld().isWalkable(nx, ny)) {
                    return Position.of(nx, ny);
                }
            }
        }
        return null;
    }
}
