package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

public class IsInCombatMissionCondition extends ConditionTask {
    @Override protected boolean check(UnitContext ctx) { return ctx.isInCombatMission(); }
}
