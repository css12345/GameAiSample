package com.huawei.contest.gameai.base.client.movement;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultConnection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;
import com.huawei.contest.gameai.base.client.entity.GameWorldState;
import com.huawei.contest.gameai.base.client.entity.GridNode;
import com.huawei.contest.gameai.base.client.entity.Position;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Set;

public class GridGraph implements IndexedGraph<GridNode> {
    private final GameWorldState world;
    @Getter
    private final int width, height;
    @Setter
    private Set<Position> vacatedPositions = Collections.emptySet();

    public GridGraph(GameWorldState world) {
        this.world = world;
        this.width = world.getWidth();
        this.height = world.getHeight();
    }

    @Override
    public int getIndex(GridNode node) {
        return node.getY() * width + node.getX();
    }

    @Override
    public int getNodeCount() {
        return width * height;
    }

    @Override
    public Array<Connection<GridNode>> getConnections(GridNode fromNode) {
        Array<Connection<GridNode>> conns = new Array<>();
        int x = fromNode.getX(), y = fromNode.getY();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx, ny = y + dy;
                if (world.isWalkableForPlanning(nx, ny, vacatedPositions)) {
                    // 对角线防止穿墙角
                    if (dx != 0 && dy != 0) {
                        if (!world.isWalkableForPlanning(x + dx, y, vacatedPositions) ||
                                !world.isWalkableForPlanning(x, y + dy, vacatedPositions)) {
                            continue;
                        }
                    }
                    conns.add(new DefaultConnection<>(fromNode, new GridNode(nx, ny)));
                }
            }
        }
        return conns;
    }

    /** 轻量级可走检查（不创建 Connection/GridNode 对象，A* 内联展开用） */
    public boolean isWalkableAt(int x, int y) {
        return world.isWalkableForPlanning(x, y, vacatedPositions);
    }
}
