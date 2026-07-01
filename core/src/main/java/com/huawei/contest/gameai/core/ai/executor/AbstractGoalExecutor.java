package com.huawei.contest.gameai.core.ai.executor;

import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.IWorldState;
import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import com.huawei.contest.gameai.core.ai.strategy.GoalExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * 目标执行器基类：提供通用辅助方法。
 */
@Slf4j
public abstract class AbstractGoalExecutor implements GoalExecutor {

    /** 找距 pos 最近的敌方活单位 */
    protected IUnit nearestEnemy(IWorldState world, Position pos) {
        IUnit best = null;
        int bestDist = Integer.MAX_VALUE;
        for (IUnit u : world.getEnemyUnits()) {
            if (!u.isAlive()) continue;
            int d = u.getPos().chebyshev(pos);
            if (d < bestDist) { bestDist = d; best = u; }
        }
        return best;
    }

    /** 找距 pos 最近的己方活单位（不含 self） */
    protected IUnit nearestAlly(IWorldState world, Position pos, int excludeId) {
        IUnit best = null;
        int bestDist = Integer.MAX_VALUE;
        for (IUnit u : world.getMyUnits()) {
            if (!u.isAlive() || u.getId() == excludeId) continue;
            int d = u.getPos().chebyshev(pos);
            if (d < bestDist) { bestDist = d; best = u; }
        }
        return best;
    }
}
