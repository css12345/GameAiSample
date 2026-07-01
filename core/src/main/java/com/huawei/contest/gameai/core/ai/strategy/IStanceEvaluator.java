package com.huawei.contest.gameai.core.ai.strategy;

import com.huawei.contest.gameai.base.client.entity.IWorldState;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;

/**
 * 战略姿态评估器：基于当前态势给出 RUSH/DEFEND/ECONOMY/HARASS。
 */
public interface IStanceEvaluator {
    String evaluate(IWorldState world, int playerId, BaseBlackboard bb);

    String RUSH = "RUSH";
    String DEFEND = "DEFEND";
    String ECONOMY = "ECONOMY";
    String HARASS = "HARASS";
}
