package com.huawei.contest.gameai.core.ai.tactical;

import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.IWorldState;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import lombok.extern.slf4j.Slf4j;

/**
 * 单位智能体：持行为树，decide() 每回合调用一次产出动作。
 */
@Slf4j
public abstract class UnitAgent {
    protected final BehaviorTree<UnitContext> tree;

    protected UnitAgent(BehaviorTree<UnitContext> tree) {
        this.tree = tree;
    }

    public RtsAction decide(IUnit self, IWorldState world, BaseBlackboard bb, Squad squad) {
        UnitContext ctx = new UnitContext(self, world, bb, squad);
        tree.setObject(ctx);
        tree.step();
        return ctx.getGeneratedAction();
    }
}
