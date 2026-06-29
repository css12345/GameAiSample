package com.huawei.contest.gameai.base.client.entity;

import com.huawei.contest.gameai.base.client.model.Building;
import com.huawei.contest.gameai.base.client.model.Producing;
import lombok.Getter;

public class GameBase implements IBase {
    private final int id;
    @Getter
    private final int playerId;
    private final Position pos;
    private final int hp;

    private Building building;
    private Producing producing;

    public GameBase(int id, int playerId, Position pos, int hp, Building building, Producing producing) {
        this.id = id;
        this.playerId = playerId;
        this.pos = pos;
        this.hp = hp;
        this.building = building;
        this.producing = producing;
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
    public int getHp() {
        return hp;
    }

    @Override
    public int getMaxHp() {
        return 3000;
    }

    @Override
    public boolean isAlive() {
        return hp > 0;
    }

    public boolean isBuilding() {
        if (building == null) {
            return false;
        }
        return building.getProgress() != building.getTotal();
    }

    public boolean isProducing() {
        if (isBuilding()) {
            return false;
        }
        if (producing == null) {
            return false;
        }
        return producing.getProgress() != producing.getTotal();
    }

    public UnitType getProducingUnit() {
        if (!isProducing()) {
            return null;
        }
        return UnitType.of(producing.getRole());
    }
}
