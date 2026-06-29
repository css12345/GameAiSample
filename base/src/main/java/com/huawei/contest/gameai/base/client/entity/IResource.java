package com.huawei.contest.gameai.base.client.entity;

public interface IResource {
    int getId();
    Position getPos();
    int getAmount();
    boolean isAlive(); // 资源耗尽视为死亡
}
