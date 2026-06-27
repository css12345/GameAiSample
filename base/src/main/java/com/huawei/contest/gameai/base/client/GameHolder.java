package com.huawei.contest.gameai.base.client;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class GameHolder {
    private static AtomicBoolean gameOver = new AtomicBoolean(false);

    /**
     * 重置
     */
    public static void reset() {
        gameOver = new AtomicBoolean(false);
    }

    /**
     * 游戏是否已经结束
     *
     * @return 结束返回{@code true}，未结束返回{@code false}
     */
    public static boolean isGameOver() {
        return gameOver.get();
    }

    /**
     * Sets the game over.
     */
    public static void gameOver() {
        gameOver.set(true);
    }
}
