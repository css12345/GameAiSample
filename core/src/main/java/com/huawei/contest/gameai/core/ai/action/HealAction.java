package com.huawei.contest.gameai.core.ai.action;

/** 医疗命令：医疗兵 id 治疗 targetId 友军 */
public class HealAction extends RtsAction {
    public int targetId;
    public HealAction(int id, int targetId) {
        super(id, "heal");
        this.targetId = targetId;
    }
}
