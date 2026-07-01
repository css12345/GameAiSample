package com.huawei.contest.gameai.core.ai.config;

import com.huawei.contest.gameai.base.client.AIConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * RTS AI 全局配置：扩展 {@link AIConfig} 的寻路参数，增加战略/战役/战术层参数。
 */
@Setter
@Getter
public class RTSConfig extends AIConfig {
    // ===== 战略层 =====
    /** 兵力比阈值：我方/敌方 > 此值倾向进攻 */
    private double armyAdvantageThreshold = 1.2;
    /** 经济比阈值：我方矿工/敌方矿工 < 此值倾向经济 */
    private double econDisadvantageThreshold = 0.8;
    /** 基地告急血量百分比 */
    private double baseEmergencyHpPercent = 0.3;
    /** 敌方基地残血绝杀阈值 */
    private double enemyBaseFinishingHpPercent = 0.2;
    /** 基地附近敌人数量告急阈值 */
    private int baseEnemyAlertCount = 3;
    /** 基地告急感知范围 */
    private int baseAlertRange = 8;

    // ===== 战役层 =====
    /** 小队编队最小人数（低于此不解散但标记不足） */
    private int squadMinSize = 2;
    /** 小队目标完成距离 */
    private int squadArriveRange = 2;
    /** 小队停滞阈值（连续 N 步无进展视为卡住） */
    private int stuckTurnThreshold = 2;
    /** 支援请求：敌我兵力比触发阈值 */
    private double supportEnemyRatioTrigger = 1.5;
    /** 支援请求：平均血量百分比触发阈值 */
    private double supportHpPercentTrigger = 0.5;
    /** 支援请求 TTL（回合） */
    private int supportTtl = 5;
    /** 高紧急度阈值（超过则抢占低优先级小队） */
    private int supportHighUrgency = 7;
    /** 小队空闲回合解散阈值 */
    private int squadIdleDisbandTurns = 10;

    // ===== 战术层 =====
    /** 战斗中低血撤退阈值（百分比） */
    private double combatLowHpPercent = 0.10;
    /** 非战斗低血撤退阈值 */
    private double normalLowHpPercent = 0.20;
    /** 规避感知范围 */
    private int evadeSenseRange = 5;
    /** 医疗治疗范围 */
    private int medicHealRange = 3;
    /** 风筝安全距离（守护者清野） */
    private int kiteSafeRange = 3;

    // ===== 生产 =====
    /** RUSH 姿态目标兵力 */
    private int rushTargetArmy = 8;
    /** ECONOMY 姿态目标矿工 */
    private int economyTargetMiners = 6;

    public static RTSConfig aggressiveRush() {
        RTSConfig c = new RTSConfig();
        return c;
    }

    public static RTSConfig economicBoom() {
        RTSConfig c = new RTSConfig();
        c.setEconDisadvantageThreshold(1.0);
        c.setRushTargetArmy(4);
        c.setEconomyTargetMiners(10);
        return c;
    }
}
