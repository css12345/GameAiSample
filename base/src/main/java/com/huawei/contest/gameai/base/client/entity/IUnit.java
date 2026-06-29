package com.huawei.contest.gameai.base.client.entity;

public interface IUnit {
    int getId();
    int getPlayerId();
    Position getPos();
    int getHp();
    int getMaxHp();
    boolean isAlive();
    int getAttackRange();
    int getDamage();
}
