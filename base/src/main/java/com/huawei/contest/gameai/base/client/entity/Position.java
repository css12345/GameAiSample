package com.huawei.contest.gameai.base.client.entity;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Position {
    private final int x, y;
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Position of(int x, int y) {
        return new Position(x, y);
    }

    public int chebyshev(Position other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }
}
