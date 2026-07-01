package com.huawei.contest.gameai.core.ai.action;

/** 火箭兵轰炸命令：单位 id 向 position[x,y] 投掷火箭弹 */
public class BombAction extends RtsAction {
    public int[] position;
    public BombAction(int id, int x, int y) {
        super(id, "bomb");
        this.position = new int[]{x, y};
    }
}
