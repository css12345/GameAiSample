package com.huawei.contest.gameai.base.client.entity;

public enum Terrain {
    EMPTY('0', false),
    STATION('1', false),
    MINER('2', false),
    FIGHTER('3', false),
    ROCKET('4', false),
    MEDIC('5', false),
    GUARDIAN('6', false),
    GOLD_MINE('7', false),
    GEM_MINE('8', false),
    TREE('9', true),
    MOUNTAIN('a', true);

    private final char code;
    private final boolean staticObstacle;

    Terrain(char code, boolean staticObstacle) {
        this.code = code;
        this.staticObstacle = staticObstacle;
    }

    public static Terrain fromCode(char code) {
        for (Terrain t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return EMPTY;
    }

    public boolean isStaticObstacle() {
        return staticObstacle;
    }
}
