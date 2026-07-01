package com.huawei.contest.gameai.core.ai.action;

/** 移动命令：单位 id 移动到 position[x,y] */
public class MoveAction extends RtsAction {
    public int[] position;
    public MoveAction(int id, int x, int y) {
        super(id, "move");
        this.position = new int[]{x, y};
    }
}
