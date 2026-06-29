package com.huawei.contest.gameai.base.client.entity;

public class GameResource implements IResource {
    private final int id;
    private final Position pos;
    private final int amount;

    public GameResource(int id, Position pos, int amount) {
        this.id = id;
        this.pos = pos;
        this.amount = amount;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Position getPos() {
        return pos;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public boolean isAlive() {
        return amount > 0;
    }
}
