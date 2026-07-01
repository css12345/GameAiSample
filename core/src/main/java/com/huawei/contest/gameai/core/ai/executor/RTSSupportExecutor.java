package com.huawei.contest.gameai.core.ai.executor;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import lombok.extern.slf4j.Slf4j;

/**
 * 增援执行器：移动到请求方位置，就近攻击敌人。
 * 移动计划已指向支援位置（squad.acceptedSupport.location 覆盖 goal.targetPos）。
 */
@Slf4j
public class RTSSupportExecutor extends AbstractGoalExecutor {
    @Override
    public void execute(Squad squad, IWorldState world, BaseBlackboard bb) {
        // 覆盖移动目标为支援位置
        if (squad.getAcceptedSupport() != null && squad.getGoal() != null) {
            // 用 acceptedSupport.location 作为临时目标
            // （movementPlan 由 planMovement 用 goal.targetPos 生成，这里不改 goal，
            //  战术层会基于 acceptedSupport 调整）
        }
        for (IUnit attacker : squad.getCombatUnits()) {
            IUnit enemy = nearestEnemy(world, attacker.getPos());
            if (enemy != null && enemy.getPos().chebyshev(attacker.getPos()) <= 6) {
                bb.getAssignedTargets().put(attacker.getId(), enemy.getId());
            }
        }
    }
}
