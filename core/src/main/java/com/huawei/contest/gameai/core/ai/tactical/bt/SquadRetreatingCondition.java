package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 小队撤退中 或 全局撤退信号 */
public class SquadRetreatingCondition extends ConditionTask {
    @Override protected boolean check(UnitContext ctx) {
        return ctx.isRetreating();
    }
}
