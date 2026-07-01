package com.huawei.contest.gameai.core.ai.campaign;

import com.badlogic.gdx.ai.fsm.State;
import com.huawei.contest.gameai.base.client.entity.IBase;
import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.IWorldState;
import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;

/**
 * 小队状态机：FORMING → MOVING → EXECUTING / RETREATING → RETURNING → IDLE。
 *
 * <p>实现 gdx-ai {@link State}，enter/exit 钩子用于状态切换时的副作用，
 * update 每回合由 {@link Squad#update} 调用以驱动转换。
 */
public enum SquadState implements State<Squad> {
    /** 编队中：等待人数到齐 */
    FORMING {
        @Override public void update(Squad s, IWorldState world, BaseBlackboard bb) {
            if (s.getUnits().isEmpty()) return;
            if (s.getGoal() == null) { s.getFsm().changeState(IDLE); return; }
            // 人数达到目标 desiredForce 的 50% 即出发
            int desired = Math.max(1, s.getGoal().getDesiredForce() / 2);
            if (s.getUnits().size() >= desired) {
                s.getFsm().changeState(MOVING);
            }
        }
    },
    /** 移动中：向目标点移动 */
    MOVING {
        @Override public void update(Squad s, IWorldState world, BaseBlackboard bb) {
            if (s.getGoal() == null) { s.getFsm().changeState(IDLE); return; }
            int range = bb.getConfig().getSquadArriveRange();
            if (s.allUnitsNearTarget(s.getGoal().getTargetPos(), range)) {
                s.getFsm().changeState(EXECUTING);
                return;
            }
            // 全局撤退信号 → 撤退
            if (bb.isRetreatSignal() || bb.isGlobalDefendSignal()) {
                s.getFsm().changeState(RETREATING);
            }
            // 血量过低 → 撤退
            if (s.getAvgHpPercent() < bb.getConfig().getSupportHpPercentTrigger()) {
                s.getFsm().changeState(RETREATING);
            }
        }
    },
    /** 执行中：在目标点执行任务 */
    EXECUTING {
        @Override public void update(Squad s, IWorldState world, BaseBlackboard bb) {
            if (s.getGoal() == null || s.getGoal().isComplete()) {
                s.getFsm().changeState(IDLE);
                return;
            }
            // 血量过低 → 撤退
            if (s.getAvgHpPercent() < bb.getConfig().getSupportHpPercentTrigger()) {
                s.getFsm().changeState(RETREATING);
                return;
            }
            // 发出支援请求
            int enemies = countEnemiesNear(world, s.getCenter(), 6);
            int allies = s.getCombatUnits().size();
            if (enemies > allies * bb.getConfig().getSupportEnemyRatioTrigger()
                    && s.getAvgHpPercent() < bb.getConfig().getSupportHpPercentTrigger()) {
                bb.addSupportRequest(new com.huawei.contest.gameai.core.ai.blackboard.SupportRequest(
                        s.getId(), s.getCenter(), Math.max(1, enemies - allies),
                        bb.getConfig().getSupportTtl(), 2));
            }
        }
    },
    /** 撤退中：回己方基地 */
    RETREATING {
        @Override public void update(Squad s, IWorldState world, BaseBlackboard bb) {
            IBase base = world.getMyBases().isEmpty() ? null : world.getMyBases().get(0);
            if (base == null) { s.getFsm().changeState(IDLE); return; }
            if (s.allUnitsNearBase(base, bb.getConfig().getSquadArriveRange())) {
                s.getFsm().changeState(RETURNING);
            }
        }
    },
    /** 返回/重组：回基地附近待命 */
    RETURNING {
        @Override public void update(Squad s, IWorldState world, BaseBlackboard bb) {
            // 血量恢复或撤退信号解除 → 重新编队
            if (s.getAvgHpPercent() > 0.7 && !bb.isRetreatSignal()) {
                s.getFsm().changeState(FORMING);
            }
        }
    },
    /** 空闲：无目标待命 */
    IDLE {
        @Override public void update(Squad s, IWorldState world, BaseBlackboard bb) {
            s.setIdleTurns(s.getIdleTurns() + 1);
            if (s.getGoal() != null) {
                s.setIdleTurns(0);
                s.getFsm().changeState(FORMING);
            }
        }
    };

    @Override public void enter(Squad entity) { }
    @Override public void exit(Squad entity) { }
    @Override public void update(Squad s) { } // 用带 world/bb 的重载
    @Override public boolean onMessage(Squad e, com.badlogic.gdx.ai.msg.Telegram telegram) {
        return false;
    }

    public void update(Squad s, IWorldState world, BaseBlackboard bb) { }

    protected static int countEnemiesNear(IWorldState world, Position pos, int range) {
        int c = 0;
        for (IUnit u : world.getEnemyUnits()) {
            if (u.isAlive() && u.getPos().chebyshev(pos) <= range) c++;
        }
        return c;
    }
}
