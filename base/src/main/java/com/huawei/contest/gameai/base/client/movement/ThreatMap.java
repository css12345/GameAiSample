package com.huawei.contest.gameai.base.client.movement;

import com.huawei.contest.gameai.base.client.entity.GameUnit;
import com.huawei.contest.gameai.base.client.entity.GameWorldState;
import com.huawei.contest.gameai.base.client.entity.IUnit;
import com.huawei.contest.gameai.base.client.entity.Terrain;

import java.util.Arrays;
import java.util.List;

public class ThreatMap {
    private double[][] danger;
    private GameWorldState world;

    public ThreatMap(GameWorldState world) {
        this.world = world;
        this.danger = new double[world.getWidth()][world.getHeight()];
    }

    void update(List<IUnit> enemies) {
        for (int x = 0; x < world.getWidth(); x++) {
            Arrays.fill(danger[x], 0.0);
        }

        for (IUnit enemy : enemies) {
            if (!enemy.isAlive()) {
                continue;
            }
            if (enemy instanceof GameUnit unit) {
                switch (unit.type) {
                    case FIGHTER: addMeleeThreat(unit); break;
                    case ROCKET: addSplashThreat(unit, 2); break;
                    case GUARDIAN: addGuardianThreat(unit); break;
                }
            }
        }
    }

    private void addMeleeThreat(GameUnit unit) {
        int x = unit.getPos().getX(), y = unit.getPos().getY();
        double dmg = unit.getDamage();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx, ny = y + dy;
                if (inMap(nx, ny)) {
                    danger[nx][ny] += dmg;
                }
            }
        }
    }

    private void addSplashThreat(GameUnit unit, int range) {
        int x = unit.getPos().getX(), y = unit.getPos().getY();
        double dmg = unit.getDamage();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                int targetX = x + dx;
                int targetY = y + dy;
                if (!inMap(targetX, targetY)) {
                    continue;
                }
                // 该目标格及其8邻域均受到同等伤害
                for (int sx = -1; sx <= 1; sx++) {
                    for (int sy = -1; sy <= 1; sy++) {
                        int nx = targetX + sx;
                        int ny = targetY + sy;
                        if (inMap(nx, ny)) {
                            danger[nx][ny] += dmg;  // 全额累加
                        }
                    }
                }
            }
        }
    }

    private void addGuardianThreat(GameUnit unit) {
        int x = unit.getPos().getX(), y = unit.getPos().getY();
        double dmg = unit.getDamage();
        // 对距离1,2,3的环分别处理
        for (int dist = 1; dist <= 3; dist++) {
            for (int dx = -dist; dx <= dist; dx++) {
                for (int dy = -dist; dy <= dist; dy++) {
                    // 只选择切比雪夫距离恰好等于dist的格子（环）
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != dist) continue;
                    int nx = x + dx, ny = y + dy;
                    if (inMap(nx, ny)) {
                        danger[nx][ny] += dmg;  // 全额伤害
                    }
                }
            }
        }
    }

    private boolean inMap(int x, int y) {
        return x >= 0 && x < world.getWidth() && y >= 0 && y < world.getHeight();
    }

    public double getDanger(int x, int y) { return danger[x][y]; }
}
