package com.huawei.contest.gameai.core.ai.executor;

import com.huawei.contest.gameai.base.client.entity.IWorldState;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import lombok.extern.slf4j.Slf4j;

/**
 * 侦察执行器：移动到目标点（移动计划已由 planMovement 填充），
 * 不做火力分配，战术层默认向目标移动。
 */
@Slf4j
public class RTSScoutExecutor extends AbstractGoalExecutor {
    @Override
    public void execute(Squad squad, IWorldState world, BaseBlackboard bb) {
        // 无特殊火力分配，靠移动计划驱动
    }
}
