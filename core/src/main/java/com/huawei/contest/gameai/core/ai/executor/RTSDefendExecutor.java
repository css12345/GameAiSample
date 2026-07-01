package com.huawei.contest.gameai.core.ai.executor;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import lombok.extern.slf4j.Slf4j;

/**
 * 防守执行器：围绕防御点，把战斗单位分配到最近敌人。
 * 移动计划已在 planMovement 中指向防御点，这里只做火力分配。
 */
@Slf4j
public class RTSDefendExecutor extends AbstractGoalExecutor {
    @Override
    public void execute(Squad squad, IWorldState world, BaseBlackboard bb) {
        Position anchor = squad.getGoal() != null ? squad.getGoal().getTargetPos() : squad.getCenter();
        for (IUnit attacker : squad.getCombatUnits()) {
            IUnit enemy = nearestEnemy(world, attacker.getPos());
            if (enemy != null && enemy.getPos().chebyshev(attacker.getPos()) <= 6) {
                bb.getAssignedTargets().put(attacker.getId(), enemy.getId());
            }
        }
    }
}
