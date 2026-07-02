package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.core.ai.action.AttackAction;
import com.huawei.contest.gameai.core.ai.action.MoveAction;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/**
 * 风筝攻击守护者：保持最大攻击距离，太近则后退，范围内则攻击。
 */
public class KeepDistanceAndAttackAction extends ActionTask {
    @Override
    protected RtsAction doAction(UnitContext ctx) {
        if (ctx.getSquad() == null || ctx.getSquad().getGoal() == null) return null;
        int guardianId = ctx.getSquad().getGoal().getTargetEntityId();
        IUnit guardian = null;
        for (IUnit u : ctx.getWorld().getEnemyUnits()) {
            if (u.getId() == guardianId) { guardian = u; break; }
        }
        if (guardian == null || !guardian.isAlive()) return null;

        Position me = ctx.getSelf().getPos();
        Position gp = guardian.getPos();
        int dist = me.chebyshev(gp);
        int range = ctx.getSelf().getAttackRange();
        int safe = ctx.getBb().getConfig().getKiteSafeRange();

        // 在攻击范围内 → 攻击
        if (dist <= range) {
            return new AttackAction(ctx.getSelf().getId(), guardianId);
        }
        // 太近（< 安全距离）→ 后退一步
        if (dist < safe) {
            int dx = Integer.signum(me.getX() - gp.getX());
            int dy = Integer.signum(me.getY() - gp.getY());
            int nx = me.getX() + (dx == 0 ? 1 : dx);
            int ny = me.getY() + (dy == 0 ? 1 : dy);
            if (ctx.getWorld().isWalkable(nx, ny)) {
                return new MoveAction(ctx.getSelf().getId(), nx, ny);
            }
        }
        // 范围外 → 靠近一步
        int dx = Integer.signum(gp.getX() - me.getX());
        int dy = Integer.signum(gp.getY() - me.getY());
        int nx = me.getX() + dx, ny = me.getY() + dy;
        if (ctx.getWorld().isWalkable(nx, ny)) {
            return new MoveAction(ctx.getSelf().getId(), nx, ny);
        }
        return null;
    }
}
