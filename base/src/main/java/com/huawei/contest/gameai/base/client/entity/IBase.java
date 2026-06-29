package com.huawei.contest.gameai.base.client.entity;

public interface IBase {
    int getId();
    Position getPos();
    int getHp();
    int getMaxHp();
    boolean isAlive();
    boolean isBuilding();

    boolean isProducing();

    UnitType getProducingUnit();
}
