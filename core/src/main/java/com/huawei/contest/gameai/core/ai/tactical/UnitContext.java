package com.huawei.contest.gameai.core.ai.tactical;

import com.huawei.contest.gameai.base.client.entity.GameUnit;
import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.IWorldState;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import com.huawei.contest.gameai.core.ai.campaign.SquadState;
import com.huawei.contest.gameai.core.ai.strategy.GoalType;
import lombok.Getter;
import lombok.Setter;

/**
 * 单位上下文（行为树黑板）：每回合每单位构建一次，行为树节点读取此对象决策。
 */
@Getter
@Setter
public class UnitContext {
    public final IUnit self;
    public final IWorldState world;
    public final BaseBlackboard bb;
    /** 所属小队（可能为 null：未编队单位） */
    public final Squad squad;
    /** 是否在战斗任务中（ATTACK/DEFEND/HARASS/CLEAR_GUARDIAN/SUPPORT） */
    public final boolean isInCombatMission;
    /** 是否在撤退（小队 RETREATING 或全局撤退信号） */
    public final boolean isRetreating;
    /** 行为树产出的动作（节点 set，decide() 读取） */
    public RtsAction generatedAction;

    public UnitContext(IUnit self, IWorldState world, BaseBlackboard bb, Squad squad) {
        this.self = self;
        this.world = world;
        this.bb = bb;
        this.squad = squad;
        this.isInCombatMission = squad != null && squad.isCombatMission();
        this.isRetreating = (squad != null && squad.isRetreating()) || bb.isRetreatSignal();
    }

    /** self 是否为战斗单位 */
    public boolean isCombatUnit() {
        if (!(self instanceof GameUnit gu)) return false;
        return gu.type == com.huawei.contest.gameai.base.client.entity.UnitType.FIGHTER
                || gu.type == com.huawei.contest.gameai.base.client.entity.UnitType.ROCKET
                || gu.type == com.huawei.contest.gameai.base.client.entity.UnitType.GUARDIAN;
    }

    public boolean isMedic() {
        return self instanceof GameUnit gu
                && gu.type == com.huawei.contest.gameai.base.client.entity.UnitType.MEDIC;
    }

    public boolean isMiner() {
        return self instanceof GameUnit gu
                && gu.type == com.huawei.contest.gameai.base.client.entity.UnitType.MINER;
    }

    /** 是否为清野守护者任务 */
    public boolean isGuardianMission() {
        return squad != null && squad.getGoal() != null
                && squad.getGoal().getType() == GoalType.CLEAR_GUARDIAN;
    }

    /** self 血量百分比 */
    public double hpPercent() {
        return (double) self.getHp() / self.getMaxHp();
    }
}
