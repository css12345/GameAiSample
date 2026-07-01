package com.huawei.contest.gameai.core.ai.executor;

import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 进攻执行器：目标评分 + 配额贪心火力分配。
 *
 * <p>评分 = 威胁(敌伤害) - 血量越低越优先 + 距离 + 角色加成(医疗>火箭>战士)。
 * 把小队战斗单位按配额分配到目标，写入 bb.assignedTargets 供战术层读取。
 */
@Slf4j
public class RTSAttackExecutor extends AbstractGoalExecutor {

    @Override
    public void execute(Squad squad, IWorldState world, BaseBlackboard bb) {
        List<IUnit> attackers = squad.getCombatUnits();
        if (attackers.isEmpty()) return;

        // 收集候选目标：敌方单位 + 敌方基地
        List<IUnit> targets = new ArrayList<>();
        for (IUnit u : world.getEnemyUnits()) {
            if (u.isAlive() && !(u instanceof GameUnit gu && gu.type == UnitType.GUARDIAN)) {
                targets.add(u);
            }
        }
        // 若无敌方单位，目标改为敌方基地（由战术层就近攻击）
        if (targets.isEmpty()) {
            for (IBase b : world.getEnemyBases()) {
                if (b.isAlive()) {
                    // 用敌方基地附近的小队目标——这里不分配具体 targetId，战术层会就近攻击建筑
                }
            }
            return;
        }

        // 目标评分排序（升序，分低优先攻击）
        Position center = squad.getCenter();
        targets.sort(Comparator.comparingDouble(t -> scoreTarget(t, center)));

        // 配额贪心分配：每个目标分配 ceil(attackers/targets) 个攻击者
        int quota = Math.max(1, attackers.size() / Math.max(1, targets.size()));
        int assigned = 0;
        int targetIdx = 0;
        for (IUnit attacker : attackers) {
            IUnit target = targets.get(targetIdx);
            bb.getAssignedTargets().put(attacker.getId(), target.getId());
            assigned++;
            if (assigned >= quota) {
                assigned = 0;
                targetIdx = Math.min(targetIdx + 1, targets.size() - 1);
            }
        }
        log.debug("小队{}分配{}个攻击者到{}个目标", squad.getId(), attackers.size(), targets.size());
    }

    private double scoreTarget(IUnit target, Position from) {
        double threat = target.getDamage();           // 威胁：伤害越高越优先处理
        double hpFactor = 1.0 - (double) target.getHp() / target.getMaxHp(); // 残血优先
        double dist = target.getPos().chebyshev(from);
        double roleBonus = 0;
        if (target instanceof GameUnit gu) {
            if (gu.type == UnitType.MEDIC) roleBonus = 30;     // 优先杀医疗
            else if (gu.type == UnitType.ROCKET) roleBonus = 15;
        }
        // 分数越低越优先：威胁/角色减分，血量/距离加分
        return dist + hpFactor * 5 - threat * 0.5 - roleBonus;
    }
}
