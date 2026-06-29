package com.huawei.contest.gameai.base.client.entity;

import java.util.List;

public interface IWorldState {
    int getWidth();
    int getHeight();
    boolean isWalkable(int x, int y);
    List<? extends IUnit> getMyUnits();
    List<? extends IUnit> getEnemyUnits();
    List<IBase> getMyBases();
    List<IBase> getEnemyBases();
    int opponent();
}
