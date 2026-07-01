package com.huawei.contest.gameai.core.ai.campaign;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.strategy.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * RTS 生产管理器：根据姿态/金钱/人口排队生产。
 */
@Slf4j
@Getter
public class RTSProductionManager {
    private final BaseBlackboard bb;
    /** 本回合待发送的生产命令（基地ID → 角色） */
    private final List<int[]> produceQueue = new ArrayList<>(); // [baseId, roleOrdinal]

    public RTSProductionManager(BaseBlackboard bb) {
        this.bb = bb;
    }

    /** 清空生产队列（每回合开始调用） */
    public void clear() {
        produceQueue.clear();
    }

    /** 决策生产。返回需补充的单位类型列表 */
    public void planProduction(IWorldState world, int playerId, String stance) {
        clear();
        List<IBase> bases = world.getMyBases();
        if (bases.isEmpty()) return;
        IBase base = bases.get(0);
        // 基地正在生产/建造则跳过
        if (base.isProducing() || base.isBuilding()) return;

        long miners = world.getMyUnits().stream().filter(this::isMiner).count();
        long army = world.getMyUnits().stream().filter(this::isCombat).count();

        String s = stance == null ? IStanceEvaluator.ECONOMY : stance;
        switch (s) {
            case IStanceEvaluator.RUSH:
                // 优先补兵，矿工维持在 4
                if (miners < 4) {
                    produceQueue.add(new int[]{base.getId(), UnitType.MINER.ordinal()});
                } else if (army < bb.getConfig().getRushTargetArmy()) {
                    // 交替造 fighter/rocket
                    UnitType t = (army % 3 == 0) ? UnitType.ROCKET : UnitType.FIGHTER;
                    produceQueue.add(new int[]{base.getId(), t.ordinal()});
                }
                break;
            case IStanceEvaluator.ECONOMY:
                if (miners < bb.getConfig().getEconomyTargetMiners()) {
                    produceQueue.add(new int[]{base.getId(), UnitType.MINER.ordinal()});
                } else {
                    produceQueue.add(new int[]{base.getId(), UnitType.FIGHTER.ordinal()});
                }
                break;
            case IStanceEvaluator.DEFEND:
                if (miners < 4) {
                    produceQueue.add(new int[]{base.getId(), UnitType.MINER.ordinal()});
                } else {
                    // 防守倾向造 fighter + medic
                    UnitType t = (army % 4 == 0) ? UnitType.MEDIC : UnitType.FIGHTER;
                    produceQueue.add(new int[]{base.getId(), t.ordinal()});
                }
                break;
            case IStanceEvaluator.HARASS:
                if (miners < 4) {
                    produceQueue.add(new int[]{base.getId(), UnitType.MINER.ordinal()});
                } else {
                    produceQueue.add(new int[]{base.getId(), UnitType.ROCKET.ordinal()});
                }
                break;
        }
    }

    private boolean isMiner(IUnit u) {
        return u instanceof GameUnit gu && gu.type == UnitType.MINER && u.isAlive();
    }

    private boolean isCombat(IUnit u) {
        if (!(u instanceof GameUnit gu)) return false;
        return u.isAlive() && (gu.type == UnitType.FIGHTER || gu.type == UnitType.ROCKET);
    }
}
