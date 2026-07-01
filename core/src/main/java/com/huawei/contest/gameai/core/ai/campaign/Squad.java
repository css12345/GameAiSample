package com.huawei.contest.gameai.core.ai.campaign;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.huawei.contest.gameai.base.client.AIConfig;
import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.base.client.movement.MovementCoordinator;
import com.huawei.contest.gameai.base.client.movement.ReservationTable;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.blackboard.SupportRequest;
import com.huawei.contest.gameai.core.ai.strategy.GoalExecutor;
import com.huawei.contest.gameai.core.ai.strategy.StrategicGoal;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 小队：一组单位 + 目标 + 状态机 + 移动计划。
 *
 * <p>状态机由 {@link SquadState} 驱动，每回合 {@link #update()} tick 一次。
 */
@Slf4j
@Getter
@Setter
public class Squad {
    public final int id;
    public final List<IUnit> units = new ArrayList<>();
    public StrategicGoal goal;
    public StrategicGoal previousGoal;
    public final StateMachine<Squad, SquadState> fsm = new DefaultStateMachine<>(this, SquadState.FORMING);
    /** unitId → 下一步位置（由 MovementCoordinator 填充） */
    public Map<Integer, Position> movementPlan = new LinkedHashMap<>();
    /** 已接受的支援请求 */
    public SupportRequest acceptedSupport;
    /** 卡住回合数（连续无进展） */
    public int stuckTurns;
    /** 空闲回合数 */
    public int idleTurns;
    /** 平均血量百分比 */
    public double avgHpPercent = 1.0;
    /** 小队中心 */
    public Position center = Position.of(0, 0);

    public Squad(int id) {
        this.id = id;
    }

    /** 添加单位 */
    public void addUnit(IUnit unit) {
        if (!units.contains(unit)) units.add(unit);
    }

    /** 移除死亡单位 */
    public void pruneDead() {
        units.removeIf(u -> !u.isAlive());
    }

    /** 是否为空 */
    public boolean isEmpty() {
        return units.isEmpty();
    }

    /** 战斗单位（fighter/rocket/guardian） */
    public List<IUnit> getCombatUnits() {
        List<IUnit> result = new ArrayList<>();
        for (IUnit u : units) {
            if (u instanceof GameUnit gu && (gu.type == UnitType.FIGHTER
                    || gu.type == UnitType.ROCKET || gu.type == UnitType.GUARDIAN)) {
                result.add(u);
            }
        }
        return result;
    }

    /** 矿工 */
    public List<IUnit> getMiners() {
        List<IUnit> result = new ArrayList<>();
        for (IUnit u : units) {
            if (u instanceof GameUnit gu && gu.type == UnitType.MINER) result.add(u);
        }
        return result;
    }

    /** 是否含医疗 */
    public boolean hasMedic() {
        for (IUnit u : units) {
            if (u instanceof GameUnit gu && gu.type == UnitType.MEDIC) return true;
        }
        return false;
    }

    /** 计算小队中心 */
    public Position getCenter() {
        if (units.isEmpty()) return center;
        int sx = 0, sy = 0;
        for (IUnit u : units) {
            sx += u.getPos().getX();
            sy += u.getPos().getY();
        }
        return Position.of(sx / units.size(), sy / units.size());
    }

    /** 所有单位是否在目标 range 内 */
    public boolean allUnitsNearTarget(Position target, int range) {
        if (target == null) return false;
        for (IUnit u : units) {
            if (u.getPos().chebyshev(target) > range) return false;
        }
        return true;
    }

    /** 所有单位是否在己方基地 range 内 */
    public boolean allUnitsNearBase(IBase base, int range) {
        if (base == null) return false;
        return allUnitsNearTarget(base.getPos(), range);
    }

    /** 是否在撤退状态 */
    public boolean isRetreating() {
        return fsm.getCurrentState() == SquadState.RETREATING;
    }

    /** 是否在移动状态 */
    public boolean isMoving() {
        return fsm.getCurrentState() == SquadState.MOVING;
    }

    /** 是否在执行状态 */
    public boolean isExecuting() {
        return fsm.getCurrentState() == SquadState.EXECUTING;
    }

    /** 是否为战斗任务（ATTACK/DEFEND/HARASS/CLEAR_GUARDIAN/SUPPORT） */
    public boolean isCombatMission() {
        if (goal == null) return false;
        return switch (goal.type) {
            case ATTACK, DEFEND, HARASS_MINERS, CLEAR_GUARDIAN, SUPPORT -> true;
            default -> false;
        };
    }

    /** 执行目标（委托给 goal.executor） */
    public void executeGoal(IWorldState world, BaseBlackboard bb) {
        if (goal == null) return;
        GoalExecutor executor = bb.getExecutor(goal.type);
        if (executor != null) {
            executor.execute(this, world, bb);
        }
    }

    /** 每回合更新：重算中心/血量、tick FSM、执行目标 */
    public void update(IWorldState world, BaseBlackboard bb) {
        pruneDead();
        if (units.isEmpty()) return;
        center = getCenter();
        avgHpPercent = computeAvgHp();
        // FSM 转换
        fsm.getCurrentState().update(this, world, bb);
        // 执行目标
        executeGoal(world, bb);
    }

    private double computeAvgHp() {
        if (units.isEmpty()) return 0;
        double sum = 0;
        for (IUnit u : units) {
            sum += (double) u.getHp() / u.getMaxHp();
        }
        return sum / units.size();
    }
}
