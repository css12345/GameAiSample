package com.huawei.contest.gameai.core.ai.strategy;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.config.RTSConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RTS 战略目标生成器：按 demo-7-1.md 逻辑生成目标。
 *
 * <p>优先级：
 * <ol>
 *   <li>基地告急 → 全防</li>
 *   <li>敌方基地残血 → 绝杀(forcePath)</li>
 *   <li>姿态常规目标</li>
 *   <li>守护者堵路/守富矿 → CLEAR_GUARDIAN</li>
 * </ol>
 */
@Slf4j
public class RTSGoalGenerator implements IGoalGenerator {

    @Override
    public List<StrategicGoal> generate(IWorldState world, int playerId, String stance, BaseBlackboard bb) {
        List<StrategicGoal> goals = new ArrayList<>();
        RTSConfig cfg = bb.getConfig();
        double baseHpPct = RTSStanceEvaluator.myBaseHpPercent(world);
        double enemyHpPct = RTSStanceEvaluator.enemyBaseHpPercent(world);
        IBase myBase = first(world.getMyBases());
        IBase enemyBase = first(world.getEnemyBases());

        // 1. 基地告急 → 全防
        int enemiesNearBase = myBase == null ? 0 : countEnemiesNear(world, myBase.getPos(), cfg.getBaseAlertRange());
        if (baseHpPct < cfg.getBaseEmergencyHpPercent() || enemiesNearBase >= cfg.getBaseEnemyAlertCount()) {
            bb.setGlobalDefendSignal(true);
            bb.setRetreatSignal(true);
            if (myBase != null) {
                goals.add(new StrategicGoal(GoalType.DEFEND, myBase.getPos(), 10, 1.0));
            }
            return goals;
        }

        // 2. 敌方基地残血 → 绝杀
        if (enemyBase != null && enemyHpPct < cfg.getEnemyBaseFinishingHpPercent()) {
            StrategicGoal finish = new StrategicGoal(GoalType.ATTACK, enemyBase.getPos(), 10, 1.0);
            finish.forcePath = true;
            goals.add(finish);
            return goals;
        }

        // 3. 姿态常规目标
        Position enemyMinersCenter = findEnemyMinersCenter(world);
        switch (stance) {
            case IStanceEvaluator.RUSH:
                if (enemyBase != null) {
                    goals.add(new StrategicGoal(GoalType.ATTACK, enemyBase.getPos(), cfg.getRushTargetArmy(), 1.0));
                }
                break;
            case IStanceEvaluator.DEFEND:
                if (myBase != null) {
                    goals.add(new StrategicGoal(GoalType.DEFEND, myBase.getPos(), 6, 1.0));
                }
                if (enemyMinersCenter != null) {
                    goals.add(new StrategicGoal(GoalType.HARASS_MINERS, enemyMinersCenter, 2, 0.6));
                }
                break;
            case IStanceEvaluator.HARASS:
                if (enemyBase != null) {
                    goals.add(new StrategicGoal(GoalType.ATTACK, enemyBase.getPos(), 4, 0.8));
                }
                if (enemyMinersCenter != null) {
                    goals.add(new StrategicGoal(GoalType.HARASS_MINERS, enemyMinersCenter, 2, 0.7));
                }
                break;
            case IStanceEvaluator.ECONOMY:
                Position resource = findNearestRichResource(world, myBase);
                if (resource != null) {
                    goals.add(new StrategicGoal(GoalType.FARM_NEUTRAL, resource, 2, 0.9));
                }
                if (enemyBase != null) {
                    goals.add(new StrategicGoal(GoalType.ATTACK, enemyBase.getPos(), 4, 0.5));
                }
                break;
        }

        // 4. 守护者堵路/守富矿 → 清野
        for (IUnit u : world.getEnemyUnits()) {
            if (!(u instanceof GameUnit gu) || gu.type != UnitType.GUARDIAN || !u.isAlive()) continue;
            if (isBlockingPath(world, gu) || hasNearbyRichResource(world, gu)) {
                StrategicGoal clear = new StrategicGoal(GoalType.CLEAR_GUARDIAN, gu.getPos(), 3, 0.8);
                clear.requiredRoles = Map.of(UnitType.ROCKET, 1, UnitType.MEDIC, 2);
                clear.targetEntityId = gu.getId();
                goals.add(clear);
            }
        }

        return goals;
    }

    /** 敌方矿工聚集中心（均值） */
    private Position findEnemyMinersCenter(IWorldState world) {
        int n = 0, sx = 0, sy = 0;
        for (IUnit u : world.getEnemyUnits()) {
            if (u.isAlive() && u instanceof GameUnit gu && gu.type == UnitType.MINER) {
                sx += u.getPos().getX();
                sy += u.getPos().getY();
                n++;
            }
        }
        return n == 0 ? null : Position.of(sx / n, sy / n);
    }

    /** 距己方基地最近的高价值中立资源（宝石矿优先） */
    private Position findNearestRichResource(IWorldState world, IBase base) {
        if (base == null) return null;
        Position best = null;
        int bestDist = Integer.MAX_VALUE;
        // GameWorldState 的 resources 通过 getEnemyUnits 无法拿到，这里用守护者附近推断
        // 简化：返回距基地最近的中立守护者位置（其旁必有资源）
        for (IUnit u : world.getEnemyUnits()) {
            if (!(u instanceof GameUnit gu) || gu.type != UnitType.GUARDIAN) continue;
            int d = gu.getPos().chebyshev(base.getPos());
            if (d < bestDist) {
                bestDist = d;
                best = gu.getPos();
            }
        }
        return best;
    }

    /** 守护者是否堵在己方基地到敌方基地的路径附近（简化：切比雪夫距离≤4 视为堵路） */
    private boolean isBlockingPath(IWorldState world, IUnit guardian) {
        IBase myBase = first(world.getMyBases());
        IBase enemyBase = first(world.getEnemyBases());
        if (myBase == null || enemyBase == null) return false;
        // 简化：守护者在两基地连线附近即视为堵路
        Position p = guardian.getPos();
        int d1 = p.chebyshev(myBase.getPos());
        int d2 = p.chebyshev(enemyBase.getPos());
        int total = myBase.getPos().chebyshev(enemyBase.getPos());
        return (d1 + d2) <= total + 4;
    }

    /** 守护者附近是否有宝石矿（简化：标记所有守护者都守资源，由世界状态补充时再细化） */
    private boolean hasNearbyRichResource(IWorldState world, IUnit guardian) {
        // GameWorldState 暴露 resources 有限，这里保守返回 true，让战役层按需清野
        return true;
    }

    private int countEnemiesNear(IWorldState world, Position pos, int range) {
        int c = 0;
        for (IUnit u : world.getEnemyUnits()) {
            if (u.isAlive() && u.getPos().chebyshev(pos) <= range) c++;
        }
        return c;
    }

    private IBase first(List<IBase> bases) {
        return bases.isEmpty() ? null : bases.get(0);
    }
}
