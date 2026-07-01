package com.huawei.contest.gameai.core.ai.strategy;

import com.huawei.contest.gameai.base.client.entity.IWorldState;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;

import java.util.List;

/**
 * 战略目标生成器：基于姿态生成目标列表。
 */
public interface IGoalGenerator {
    List<StrategicGoal> generate(IWorldState world, int playerId, String stance, BaseBlackboard bb);
}
