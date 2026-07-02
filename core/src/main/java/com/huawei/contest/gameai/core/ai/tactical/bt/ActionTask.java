package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/**
 * 动作节点基类：执行后 set ctx.generatedAction，返回 SUCCEEDED；
 * 无法执行时返回 FAILED。
 */
public abstract class ActionTask extends LeafTask<UnitContext> {
    @Override
    public Task.Status execute() {
        RtsAction action = doAction(getObject());
        if (action == null) return Task.Status.FAILED;
        getObject().setGeneratedAction(action);
        return Task.Status.SUCCEEDED;
    }

    /** 返回要执行的动作，返回 null 表示无法执行 */
    protected abstract RtsAction doAction(UnitContext ctx);

    @Override
    protected Task<UnitContext> copyTo(Task<UnitContext> task) {
        return task;
    }
}
