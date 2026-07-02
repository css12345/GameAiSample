package com.huawei.contest.gameai.core.ai.tactical;

import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.branch.Selector;
import com.badlogic.gdx.ai.btree.branch.Sequence;
import com.huawei.contest.gameai.core.ai.tactical.bt.*;
import lombok.extern.slf4j.Slf4j;

/**
 * RTS 单位智能体：构建优先级行为树。
 *
 * <p>优先级（Selector，第一个成功即返回）：
 * <ol>
 *   <li>小队撤退 → 跟随计划步/撤回基地</li>
 *   <li>低血量 → 撤退</li>
 *   <li>非战斗且敌人≤5且可规避 → 规避</li>
 *   <li>战斗单位邻接敌人 → 攻击最近</li>
 *   <li>医疗 → 治疗友军</li>
 *   <li>矿工 → 采矿</li>
 *   <li>清野守护者任务 → 风筝攻击</li>
 *   <li>战斗任务 → 攻击分配目标</li>
 *   <li>小队移动有计划 → 跟随</li>
 *   <li>默认 → 向目标点移动</li>
 * </ol>
 */
@Slf4j
public class RTSUnitAgent extends UnitAgent {

    public RTSUnitAgent() {
        super(buildTree());
    }

    private static BehaviorTree<UnitContext> buildTree() {
        Selector<UnitContext> root = new Selector<>();

        // 1. 撤退 → 跟随计划
        Sequence<UnitContext> retreat = new Sequence<>();
        retreat.addChild(new SquadRetreatingCondition());
        retreat.addChild(new RetreatAction());
        root.addChild(retreat);

        // 2. 低血量 → 撤退
        Sequence<UnitContext> lowHp = new Sequence<>();
        lowHp.addChild(new LowHpCondition());
        lowHp.addChild(new RetreatAction());
        root.addChild(lowHp);

        // 3. 规避
        Sequence<UnitContext> evade = new Sequence<>();
        evade.addChild(new EnemyInRangeCondition());
        evade.addChild(new CanEvadeCondition());
        evade.addChild(new EvadeAction());
        root.addChild(evade);

        // 4. 战斗单位邻接敌人 → 攻击最近
        Sequence<UnitContext> attackNearest = new Sequence<>();
        attackNearest.addChild(new IsCombatUnitCondition());
        attackNearest.addChild(new AttackNearestEnemyAction());
        root.addChild(attackNearest);

        // 5. 医疗 → 治疗
        Sequence<UnitContext> heal = new Sequence<>();
        heal.addChild(new IsMedicCondition());
        heal.addChild(new HealAllyAction());
        root.addChild(heal);

        // 6. 矿工 → 采矿
        Sequence<UnitContext> mine = new Sequence<>();
        mine.addChild(new IsMinerCondition());
        mine.addChild(new MineResourceAction());
        root.addChild(mine);

        // 7. 清野守护者 → 风筝
        Sequence<UnitContext> kite = new Sequence<>();
        kite.addChild(new IsGuardianMissionCondition());
        kite.addChild(new KeepDistanceAndAttackAction());
        root.addChild(kite);

        // 8. 战斗任务 → 攻击分配目标
        Sequence<UnitContext> attackAssigned = new Sequence<>();
        attackAssigned.addChild(new IsInCombatMissionCondition());
        attackAssigned.addChild(new AttackAssignedTargetAction());
        root.addChild(attackAssigned);

        // 9. 小队移动有计划 → 跟随
        Sequence<UnitContext> followPlan = new Sequence<>();
        followPlan.addChild(new SquadMovingCondition());
        followPlan.addChild(new HasPlannedStepCondition());
        followPlan.addChild(new MoveToPlannedStepAction());
        root.addChild(followPlan);

        // 10. 默认 → 向目标点移动
        root.addChild(new MoveToFormationAction());

        return new BehaviorTree<>(root);
    }
}
