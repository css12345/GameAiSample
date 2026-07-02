package com.huawei.contest.gameai.core.ai.tactical.bt;

import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.core.ai.action.HealAction;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.tactical.UnitContext;

/** 治疗 3 格内血量最低的友军 */
public class HealAllyAction extends ActionTask {
    @Override
    protected RtsAction doAction(UnitContext ctx) {
        int range = ctx.getBb().getConfig().getMedicHealRange();
        IUnit lowest = null;
        double lowestPct = 1.0;
        for (IUnit u : ctx.getWorld().getMyUnits()) {
            if (!u.isAlive() || u.getId() == ctx.getSelf().getId()) continue;
            int d = u.getPos().chebyshev(ctx.getSelf().getPos());
            if (d > range) continue;
            double pct = (double) u.getHp() / u.getMaxHp();
            if (pct < 1.0 && pct < lowestPct) {
                lowestPct = pct;
                lowest = u;
            }
        }
        if (lowest == null) return null;
        return new HealAction(ctx.getSelf().getId(), lowest.getId());
    }
}
