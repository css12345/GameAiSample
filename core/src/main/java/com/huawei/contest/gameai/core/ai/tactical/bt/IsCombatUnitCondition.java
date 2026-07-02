package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

public class IsCombatUnitCondition extends ConditionTask {
    @Override protected boolean check(UnitContext ctx) { return ctx.isCombatUnit(); }
}
