package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.core.ai.action.AttackAction;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 攻击黑板分配的目标（由 GoalExecutor 填充 assignedTargets） */
public class AttackAssignedTargetAction extends ActionTask {
    @Override
    protected RtsAction doAction(UnitContext ctx) {
        Integer targetId = ctx.getBb().getAssignedTargets().get(ctx.getSelf().getId());
        if (targetId == null) return null;
        // 确认目标存在且在攻击范围
        IUnit target = null;
        for (IUnit u : ctx.getWorld().getEnemyUnits()) {
            if (u.getId() == targetId) { target = u; break; }
        }
        if (target == null || !target.isAlive()) return null;
        // 范围内直接攻击，否则返回 null（让上层移动靠近）
        if (target.getPos().chebyshev(ctx.getSelf().getPos()) > ctx.getSelf().getAttackRange()) {
            return null;
        }
        return new AttackAction(ctx.getSelf().getId(), targetId);
    }
}
