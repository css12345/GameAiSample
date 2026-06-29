package com.huawei.contest.gameai.base.client.entity;

public enum UnitType {
    MINER("miner", 70, 0, 1),
    FIGHTER("fighter", 150, 14, 1),
    ROCKET("rocket", 175, 10, 2),
    MEDIC("medic", 80, 5, 1),
    GUARDIAN("guardian", 800, 12, 3);

    final String name;
    final int maxHp;
    final int damage;
    final int attackRange;

    UnitType(String name, int maxHp, int damage, int attackRange) {
        this.name = name;
        this.maxHp = maxHp;
        this.damage = damage;
        this.attackRange = attackRange;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getDamage() {
        return damage;
    }

    public int getAttackRange() {
        return attackRange;
    }

    public static UnitType of(String name) {
        for (UnitType value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return null;
    }
}
