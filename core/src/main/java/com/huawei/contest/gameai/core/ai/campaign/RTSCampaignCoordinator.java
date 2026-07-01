package com.huawei.contest.gameai.core.ai.campaign;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.strategy.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RTS 战役协调器：组装姿态评估器 + 目标生成器 + 生产管理器 + 执行器。
 */
@Slf4j
public class RTSCampaignCoordinator extends CampaignCoordinator {
    private final IStanceEvaluator stanceEvaluator;
    private final IGoalGenerator goalGenerator;
    private final RTSProductionManager productionManager;
    private int stuckDetectionTurns = 0;

    public RTSCampaignCoordinator(BaseBlackboard bb,
                                  IStanceEvaluator stanceEvaluator,
                                  IGoalGenerator goalGenerator,
                                  RTSProductionManager productionManager) {
        super(bb);
        this.stanceEvaluator = stanceEvaluator;
        this.goalGenerator = goalGenerator;
        this.productionManager = productionManager;
    }

    @Override
    protected List<IUnit> getUnassignedUnits(IWorldState world) {
        Set<Integer> assigned = new HashSet<>();
        for (Squad s : squads.values()) {
            for (IUnit u : s.getUnits()) assigned.add(u.getId());
        }
        return world.getMyUnits().stream()
                .filter(IUnit::isAlive)
                .filter(u -> !assigned.contains(u.getId()))
                .collect(Collectors.toList());
    }

    @Override
    protected List<StrategicGoal> generateGoals(IWorldState world, int playerId) {
        String stance = stanceEvaluator.evaluate(world, playerId, bb);
        bb.setCurrentStance(stance);
        List<StrategicGoal> goals = goalGenerator.generate(world, playerId, stance, bb);
        log.debug("姿态={} 生成目标={}个", stance, goals.size());
        return goals;
    }

    @Override
    protected void doProduction(IWorldState world, int playerId) {
        productionManager.planProduction(world, playerId, bb.getCurrentStance());
    }

    @Override
    protected void evaluateGlobalSignals(IWorldState world, int playerId) {
        double baseHpPct = RTSStanceEvaluator.myBaseHpPercent(world);
        IBase myBase = world.getMyBases().isEmpty() ? null : world.getMyBases().get(0);
        int enemiesNear = myBase == null ? 0
                : (int) world.getEnemyUnits().stream()
                .filter(u -> u.isAlive() && u.getPos().chebyshev(myBase.getPos()) <= bb.getConfig().getBaseAlertRange())
                .count();
        if (baseHpPct < bb.getConfig().getBaseEmergencyHpPercent() || enemiesNear >= bb.getConfig().getBaseEnemyAlertCount()) {
            bb.setGlobalDefendSignal(true);
            bb.setRetreatSignal(true);
            if (myBase != null) bb.setGlobalDefendTarget(myBase.getPos());
        }
    }

    @Override
    protected List<IUnit> getEnemies(IWorldState world) {
        return new ArrayList<>(world.getEnemyUnits());
    }

    @Override
    protected boolean isMiner(IUnit u) {
        return u instanceof GameUnit gu && gu.type == UnitType.MINER;
    }

    @Override
    protected double getSquadPriority(Squad s) {
        return s.getGoal() == null ? 0 : s.getGoal().getPriority();
    }

    @Override
    protected void assignUnits(List<IUnit> available, List<StrategicGoal> newGoals) {
        // 优先满足 requiredRoles 目标（CLEAR_GUARDIAN）
        for (StrategicGoal goal : newGoals) {
            if (goal.getRequiredRoles() == null || goal.getRequiredRoles().isEmpty()) continue;
            Squad squad = findOrCreateSquadForGoal(goal);
            for (Map.Entry<UnitType, Integer> role : goal.getRequiredRoles().entrySet()) {
                int need = role.getValue();
                for (IUnit u : new ArrayList<>(available)) {
                    if (need <= 0) break;
                    if (u instanceof GameUnit gu && gu.type == role.getKey()) {
                        squad.addUnit(u);
                        available.remove(u);
                        need--;
                    }
                }
            }
            // 兜底：用 fighter 填充未满足的角色
            if (squad.getUnits().size() < goal.getRequiredRoles().values().stream().mapToInt(Integer::intValue).sum()) {
                Iterator<IUnit> it = available.iterator();
                while (it.hasNext() && squad.getUnits().size() < goal.getDesiredForce()) {
                    squad.addUnit(it.next());
                    it.remove();
                }
            }
        }
        // 常规目标走默认分配
        super.assignUnits(available, newGoals);
    }

    @Override
    protected void planMovement(IWorldState world) {
        super.planMovement(world);
        // 检测卡住的小队：连续 stuckTurns≥阈值 注入清障目标
        for (Squad s : squads.values()) {
            if (s.getMovementPlan().isEmpty() && s.isMoving() && s.getGoal() != null) {
                s.setStuckTurns(s.getStuckTurns() + 1);
            } else if (!s.getMovementPlan().isEmpty()) {
                s.setStuckTurns(0);
            }
            if (s.getStuckTurns() >= bb.getConfig().getStuckTurnThreshold()) {
                // 注入清除路障目标（找最近敌人/守护者）
                IUnit blocker = findNearestBlocker(world, s.getCenter());
                if (blocker != null) {
                    StrategicGoal clear = new StrategicGoal(GoalType.CLEAR_GUARDIAN, blocker.getPos(), 3, 0.9);
                    clear.targetEntityId = blocker.getId();
                    s.setGoal(clear);
                    s.setStuckTurns(0);
                    log.info("小队{}卡住，注入清障目标@{}", s.getId(), blocker.getPos());
                }
            }
        }
    }

    private Squad findOrCreateSquadForGoal(StrategicGoal goal) {
        for (Squad s : squads.values()) {
            if (s.getGoal() != null && s.getGoal().getType() == goal.getType()
                    && s.getGoal().getTargetPos().equals(goal.getTargetPos())) {
                return s;
            }
        }
        Squad squad = new Squad(nextSquadId++);
        squad.setGoal(goal);
        squads.put(squad.getId(), squad);
        return squad;
    }

    private IUnit findNearestBlocker(IWorldState world, Position center) {
        IUnit nearest = null;
        int bestDist = Integer.MAX_VALUE;
        for (IUnit u : world.getEnemyUnits()) {
            if (!u.isAlive()) continue;
            int d = u.getPos().chebyshev(center);
            if (d < bestDist) {
                bestDist = d;
                nearest = u;
            }
        }
        return nearest;
    }

    public RTSProductionManager getProductionManager() {
        return productionManager;
    }
}
