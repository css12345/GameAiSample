package com.huawei.contest.gameai.core.ai.action;

import com.huawei.contest.gameai.base.client.entity.UnitType;

/** 生产命令：基地 id 生产 role 角色单位 */
public class ProduceAction extends RtsAction {
    public String role;
    public ProduceAction(int id, UnitType type) {
        super(id, "produce");
        this.role = roleName(type);
    }

    public ProduceAction(int id, String roleName) {
        super(id, "produce");
        this.role = roleName;
    }

    private static String roleName(UnitType type) {
        return switch (type) {
            case MINER -> "miner";
            case FIGHTER -> "fighter";
            case ROCKET -> "rocket";
            case MEDIC -> "medic";
            case GUARDIAN -> "guardian";
        };
    }
}
