package com.huawei.contest.gameai.core.ai.action;

/** 单点攻击命令：单位 id 攻击 targetId */
public class AttackAction extends RtsAction {
    public int targetId;
    public AttackAction(int id, int targetId) {
        super(id, "attack");
        this.targetId = targetId;
    }
}
