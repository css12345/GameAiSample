package com.huawei.contest.gameai.core.ai.action;

/** 采矿命令：矿工 id 采集 targetId 矿石 */
public class PickAction extends RtsAction {
    public int targetId;
    public PickAction(int id, int targetId) {
        super(id, "pick");
        this.targetId = targetId;
    }
}
