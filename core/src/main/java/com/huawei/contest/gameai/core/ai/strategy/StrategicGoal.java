package com.huawei.contest.gameai.core.ai.strategy;

import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.base.client.entity.UnitType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 战略目标：由战略层生成，战役层据此组建/调度小队。
 */
@Getter
@Setter
public class StrategicGoal {
    public final GoalType type;
    /** 目标位置 */
    public final Position targetPos;
    /** 期望兵力 */
    public final int desiredForce;
    /** 优先级（0-1，越大越优先） */
    public final double priority;
    /** 是否强制寻路（忽略威胁/绕路，用于绝杀） */
    public boolean forcePath;
    /** 精确角色需求（CLEAR_GUARDIAN 用） */
    public Map<UnitType, Integer> requiredRoles;
    /** 目标实体 ID（守护者/建筑等） */
    public int targetEntityId = -1;
    /** 是否已完成 */
    public boolean complete = false;

    public StrategicGoal(GoalType type, Position targetPos, int desiredForce, double priority) {
        this.type = type;
        this.targetPos = targetPos;
        this.desiredForce = desiredForce;
        this.priority = priority;
    }
}
