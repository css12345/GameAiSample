package com.huawei.contest.gameai.core.ai.strategy;

/**
 * 战略目标类型。
 */
public enum GoalType {
    /** 进攻（敌方基地/主力） */
    ATTACK,
    /** 防守（己方基地/要点） */
    DEFEND,
    /** 骚扰敌方矿工 */
    HARASS_MINERS,
    /** 侦察未探索区域 */
    SCOUT,
    /** 开采中立资源（需先清守护者） */
    FARM_NEUTRAL,
    /** 增援友军 */
    SUPPORT,
    /** 清除堵路/守资源的中立守护者 */
    CLEAR_GUARDIAN
}
