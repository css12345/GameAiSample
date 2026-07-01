package com.huawei.contest.gameai.core.ai.executor;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import lombok.extern.slf4j.Slf4j;

/**
 * 清除守护者执行器：火箭兵风筝（保持最大射程），医疗跟进治疗。
 * 火力分配到 goal.targetEntityId 指定的守护者。
 */
@Slf4j
public class RTSClearGuardianExecutor extends AbstractGoalExecutor {
    @Override
    public void execute(Squad squad, IWorldState world, BaseBlackboard bb) {
        int guardianId = squad.getGoal() != null ? squad.getGoal().getTargetEntityId() : -1;
        IUnit guardian = null;
        if (guardianId > 0) {
            for (IUnit u : world.getEnemyUnits()) {
                if (u.getId() == guardianId) { guardian = u; break; }
            }
        }
        if (guardian == null) {
            if (squad.getGoal() != null) squad.getGoal().setComplete(true);
            return;
        }
        // 所有战斗单位攻击该守护者
        for (IUnit attacker : squad.getCombatUnits()) {
            bb.getAssignedTargets().put(attacker.getId(), guardian.getId());
        }
        // 战术层 KeepDistanceAndAttackAction 会基于角色保持射程
    }
}
