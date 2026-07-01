package com.huawei.contest.gameai.core.ai.strategy;

import com.huawei.contest.gameai.base.client.entity.IWorldState;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;

/**
 * 目标执行器：战役层把小队分配给目标后，调用执行器填充火力分配/移动计划。
 *
 * <p>执行器不直接产生单位动作（那是战术层行为树的职责），而是把"打谁/去哪"
 * 写入黑板供战术层读取。
 */
public interface GoalExecutor {
    void execute(Squad squad, IWorldState world, BaseBlackboard bb);
}
