package com.huawei.contest.gameai.core.ai.action;

/** 建造基地命令：矿工 id 在 position[x,y] 建造基地 */
public class BuildAction extends RtsAction {
    public int[] position;
    public BuildAction(int id, int x, int y) {
        super(id, "build");
        this.position = new int[]{x, y};
    }
}
