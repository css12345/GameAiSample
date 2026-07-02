package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/**
 * 条件节点基类：返回 SUCCEEDED 或 FAILED。
 */
public abstract class ConditionTask extends LeafTask<UnitContext> {
    @Override
    public Task.Status execute() {
        return check(getObject()) ? Task.Status.SUCCEEDED : Task.Status.FAILED;
    }

    protected abstract boolean check(UnitContext ctx);

    @Override
    protected Task<UnitContext> copyTo(Task<UnitContext> task) {
        return task;
    }
}
