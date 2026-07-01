package com.huawei.contest.gameai.core.ai.strategy;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.config.RTSConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * RTS 战略姿态评估器：基于兵力比/经济比/基地血量给出姿态。
 */
@Slf4j
public class RTSStanceEvaluator implements IStanceEvaluator {

    @Override
    public String evaluate(IWorldState world, int playerId, BaseBlackboard bb) {
        double armyRatio = armyRatio(world);
        double econRatio = econRatio(world);
        double baseHpPct = myBaseHpPercent(world);
        RTSConfig cfg = bb.getConfig();

        // 基地告急 → 防守
        if (baseHpPct < cfg.getBaseEmergencyHpPercent()) {
            return DEFEND;
        }
        // 兵力优势 → 进攻
        if (armyRatio >= cfg.getArmyAdvantageThreshold()) {
            return RUSH;
        }
        // 经济劣势 → 发展经济
        if (econRatio < cfg.getEconDisadvantageThreshold()) {
            return ECONOMY;
        }
        // 兵力均势但经济不弱 → 骚扰
        if (armyRatio >= 0.8) {
            return HARASS;
        }
        // 兵力劣势 → 防守
        return DEFEND;
    }

    /** 我方战斗力单位数 / 敌方战斗力单位数（fighter+rocket+guardian，不含矿工/医疗） */
    public static double armyRatio(IWorldState world) {
        long mine = combatUnits(world.getMyUnits());
        long foe = combatUnits(world.getEnemyUnits());
        if (foe == 0) return mine == 0 ? 1.0 : 10.0;
        return (double) mine / foe;
    }

    /** 我方矿工数 / 敌方矿工数 */
    public static double econRatio(IWorldState world) {
        long mine = miners(world.getMyUnits());
        long foe = miners(world.getEnemyUnits());
        if (foe == 0) return mine == 0 ? 1.0 : 10.0;
        return (double) mine / foe;
    }

    /** 己方基地血量百分比（0-1），无基地返回 0 */
    public static double myBaseHpPercent(IWorldState world) {
        List<IBase> bases = world.getMyBases();
        if (bases.isEmpty()) return 0;
        IBase b = bases.get(0);
        return (double) b.getHp() / b.getMaxHp();
    }

    /** 敌方基地血量百分比 */
    public static double enemyBaseHpPercent(IWorldState world) {
        List<IBase> bases = world.getEnemyBases();
        if (bases.isEmpty()) return 1.0;
        IBase b = bases.get(0);
        return (double) b.getHp() / b.getMaxHp();
    }

    private static long combatUnits(List<? extends IUnit> units) {
        return units.stream().filter(u -> {
            if (!u.isAlive() || !(u instanceof GameUnit gu)) return false;
            return gu.type == UnitType.FIGHTER || gu.type == UnitType.ROCKET || gu.type == UnitType.GUARDIAN;
        }).count();
    }

    private static long miners(List<? extends IUnit> units) {
        return units.stream().filter(u -> u.isAlive() && u instanceof GameUnit gu
                && gu.type == UnitType.MINER).count();
    }
}
