package com.huawei.contest.gameai.core.ai.campaign;

import com.huawei.contest.gameai.base.client.AIConfig;
import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.base.client.movement.MovementCoordinator;
import com.huawei.contest.gameai.base.client.movement.ReservationTable;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.blackboard.SupportRequest;
import com.huawei.contest.gameai.core.ai.strategy.StrategicGoal;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 战役协调器（抽象）：模板方法 update() 编排每回合流程。
 *
 * <p>子类实现抽象钩子：getUnassignedUnits / generateGoals / doProduction /
 * evaluateGlobalSignals / getEnemies / isMiner / getSquadPriority。
 */
@Slf4j
@Getter
public abstract class CampaignCoordinator {
    protected final BaseBlackboard bb;
    protected final Map<Integer, Squad> squads = new LinkedHashMap<>();
    protected int nextSquadId = 1;
    /** 跨小队共享的预留表/腾空集合（每回合重建） */
    protected ReservationTable globalResTable;
    protected Set<Position> globalVacates;

    protected CampaignCoordinator(BaseBlackboard bb) {
        this.bb = bb;
    }

    /** 每回合主流程 */
    public void update(IWorldState world, int playerId) {
        // 0. 重建共享寻路状态
        globalResTable = new ReservationTable();
        globalVacates = new HashSet<>();
        // 所有己方单位起点标记腾空（跨小队可穿过）
        for (IUnit u : world.getMyUnits()) {
            globalVacates.add(u.getPos());
        }

        // 1. 清理死亡单位
        pruneDead();

        // 2. 评估全局信号
        evaluateGlobalSignals(world, playerId);

        // 3. 生成战略目标
        List<StrategicGoal> goals = generateGoals(world, playerId);

        // 4. 获取未分配单位
        List<IUnit> available = getUnassignedUnits(world);

        // 5. 分配单位到小队
        assignUnits(available, goals);

        // 6. 生产
        doProduction(world, playerId);

        // 7. 支援请求处理
        generateSupportRequests(world);
        processSupportRequests(world);

        // 8. 紧急抢占
        preemptForEmergency(world);

        // 9. 合并/解散
        checkForMerge();
        checkForDisband();

        // 10. 规划移动
        planMovement(world);

        // 11. 小队更新（FSM + 执行目标）
        for (Squad s : squads.values()) {
            s.update(world, bb);
        }

        // 12. 清理回合临时状态
        bb.cleanRequests();
    }

    // ===== 抽象钩子 =====
    protected abstract List<IUnit> getUnassignedUnits(IWorldState world);
    protected abstract List<StrategicGoal> generateGoals(IWorldState world, int playerId);
    protected abstract void doProduction(IWorldState world, int playerId);
    protected abstract void evaluateGlobalSignals(IWorldState world, int playerId);
    protected abstract List<IUnit> getEnemies(IWorldState world);
    protected abstract boolean isMiner(IUnit u);
    protected abstract double getSquadPriority(Squad s);

    // ===== 默认实现：分配单位 =====
    protected void assignUnits(List<IUnit> available, List<StrategicGoal> newGoals) {
        // 先给已有小队补员
        Iterator<IUnit> it = available.iterator();
        while (it.hasNext()) {
            IUnit u = it.next();
            for (Squad s : squads.values()) {
                if (s.getGoal() != null && !s.getGoal().isComplete()
                        && s.getUnits().size() < s.getGoal().getDesiredForce()) {
                    s.addUnit(u);
                    it.remove();
                    break;
                }
            }
        }
        // 为新目标创建小队
        for (StrategicGoal goal : newGoals) {
            if (goal.isComplete()) continue;
            if (available.isEmpty()) break;
            Squad squad = new Squad(nextSquadId++);
            squad.setGoal(goal);
            // 分配单位（按角色需求或通用）
            int need = Math.min(goal.getDesiredForce(), available.size());
            Iterator<IUnit> ait = available.iterator();
            int assigned = 0;
            while (ait.hasNext() && assigned < need) {
                IUnit u = ait.next();
                squad.addUnit(u);
                ait.remove();
                assigned++;
            }
            if (!squad.isEmpty()) {
                squads.put(squad.getId(), squad);
            }
        }
    }

    // ===== 默认实现：规划移动 =====
    protected void planMovement(IWorldState world) {
        if (!(world instanceof GameWorldState gws)) return;
        MovementCoordinator mc = new MovementCoordinator(gws);
        for (Squad s : squads.values()) {
            if (s.getUnits().isEmpty() || s.getGoal() == null) continue;
            if (s.isExecuting() || s.isRetreating()) {
                // 执行/撤退中不重规划移动（由战术层就近动作）
                continue;
            }
            Position target = s.isRetreating() && !world.getMyBases().isEmpty()
                    ? world.getMyBases().get(0).getPos() : s.getGoal().getTargetPos();
            if (target == null) continue;
            List<IUnit> units = new ArrayList<>(s.getUnits());
            List<IUnit> enemies = new ArrayList<>(getEnemies(world));
            double aggressiveness = s.getGoal().isForcePath() ? 1.0
                    : (s.isCombatMission() ? 0.7 : 0.3);
            AIConfig cfg = bb.getConfig();
            Map<Integer, Position> plan = mc.planSquadMovement(
                    units, target, enemies, aggressiveness, cfg, globalResTable, globalVacates);
            s.setMovementPlan(plan);
        }
        mc.cleanup();
    }

    // ===== 默认实现：支援请求生成 =====
    protected void generateSupportRequests(IWorldState world) {
        // EXECUTING 小队寡不敌众时已在 SquadState 中发请求
    }

    protected void processSupportRequests(IWorldState world) {
        for (SupportRequest req : bb.getSupportRequests()) {
            if (req.isSatisfied()) continue;
            // 找最近的空闲/附近小队接受
            for (Squad s : squads.values()) {
                if (req.isSatisfied()) break;
                if (s.getAcceptedSupport() != null) continue;
                if (s.getFsm().getCurrentState() == SquadState.IDLE
                        || s.getFsm().getCurrentState() == SquadState.RETURNING) {
                    Position c = s.getCenter();
                    if (c.chebyshev(req.getLocation()) <= 15) {
                        req.accept(s.getId());
                        s.setAcceptedSupport(req);
                    }
                }
            }
        }
    }

    protected void preemptForEmergency(IWorldState world) {
        // 高紧急度支援请求抢占低优先级小队
        for (SupportRequest req : bb.getSupportRequests()) {
            if (req.getUrgency() < bb.getConfig().getSupportHighUrgency()) continue;
            for (Squad s : squads.values()) {
                if (req.isSatisfied()) break;
                if (s.getAcceptedSupport() != null) continue;
                if (getSquadPriority(s) < 0.5) {
                    req.accept(s.getId());
                    s.setAcceptedSupport(req);
                    s.getFsm().changeState(SquadState.MOVING);
                }
            }
        }
    }

    protected void checkForMerge() {
        List<Squad> idleSquads = new ArrayList<>();
        for (Squad s : squads.values()) {
            if (s.getFsm().getCurrentState() == SquadState.IDLE) idleSquads.add(s);
        }
        // 合并目标相近的空闲小队
        for (int i = 0; i < idleSquads.size(); i++) {
            Squad a = idleSquads.get(i);
            for (int j = i + 1; j < idleSquads.size(); j++) {
                Squad b = idleSquads.get(j);
                if (a.getGoal() != null && b.getGoal() != null
                        && a.getGoal().getTargetPos().chebyshev(b.getGoal().getTargetPos()) <= 5) {
                    b.getUnits().forEach(a::addUnit);
                    b.getUnits().clear();
                }
            }
        }
    }

    protected void checkForDisband() {
        squads.values().removeIf(s -> {
            if (s.isEmpty()) return true;
            if (s.getIdleTurns() > bb.getConfig().getSquadIdleDisbandTurns()) return true;
            return false;
        });
    }

    protected void pruneDead() {
        for (Squad s : squads.values()) s.pruneDead();
        squads.values().removeIf(Squad::isEmpty);
    }
}
