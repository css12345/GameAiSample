package com.huawei.contest.gameai.base.client.entity;

public class GameUnit implements IUnit {
    private final int id;
    private final int playerId;
    public final UnitType type;
    private final Position pos;
    private final int hp;

    public GameUnit(int id, int playerId, UnitType type, Position pos, int hp) {
        this.id = id;
        this.playerId = playerId;
        this.type = type;
        this.pos = pos;
        this.hp = hp;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getPlayerId() {
        return playerId;
    }

    @Override
    public Position getPos() {
        return pos;
    }

    @Override
    public int getHp() {
        return hp;
    }

    @Override
    public int getMaxHp() {
        return type.getMaxHp();
    }

    @Override
    public boolean isAlive() {
        return hp > 0;
    }

    @Override
    public int getAttackRange() {
        return type.getAttackRange();
    }

    @Override
    public int getDamage() {
        return type.getDamage();
    }
}
