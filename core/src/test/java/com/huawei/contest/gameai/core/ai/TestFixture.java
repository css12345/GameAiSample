package com.huawei.contest.gameai.core.ai;

import com.huawei.contest.gameai.base.client.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试辅助：构造 Start / Inquire 等模型对象。
 */
public class TestFixture {

    /** 40x40 全空地地图（用于简化测试） */
    public static Start buildOpenMap(int playerId, int opponentId) {
        return buildMap(40, 40, emptyMapData(40, 40), playerId, opponentId);
    }

    public static Start buildMap(int w, int h, String data, int playerId, int opponentId) {
        Start start = new Start();
        StartMap map = new StartMap();
        map.setData(data);
        map.setMaxX(w);
        map.setMaxY(h);
        start.setMap(map);

        // ID 范围按 playerId 前缀推断：100xxx → 100000-199999, 200xxx → 200000-299999
        StartPlayer p1 = new StartPlayer();
        p1.setPlayerId(playerId);
        ObjectIdRange r1 = new ObjectIdRange();
        r1.setMin((playerId / 100000) * 100000);
        r1.setMax((playerId / 100000) * 100000 + 99999);
        p1.setObjectIdRange(r1);

        StartPlayer p2 = new StartPlayer();
        p2.setPlayerId(opponentId);
        ObjectIdRange r2 = new ObjectIdRange();
        r2.setMin((opponentId / 100000) * 100000);
        r2.setMax((opponentId / 100000) * 100000 + 99999);
        p2.setObjectIdRange(r2);

        start.setPlayers(List.of(p1, p2));
        return start;
    }

    public static String emptyMapData(int w, int h) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w * h; i++) {
            if (i > 0) sb.append(',');
            sb.append('0');
        }
        return sb.toString();
    }

    /** 构造单位信息 */
    public static RoleInformation unit(int id, String role, int x, int y, int life) {
        RoleInformation ri = new RoleInformation();
        ri.setId(id);
        ri.setRole(role);
        ri.setPosition(new int[]{x, y});
        ri.setLife(life);
        return ri;
    }

    /** 构造资源 */
    public static RoleInformation resource(int id, String role, int x, int y, int gold) {
        RoleInformation ri = new RoleInformation();
        ri.setId(id);
        ri.setRole(role);
        ri.setPosition(new int[]{x, y});
        ri.setGold(gold);
        return ri;
    }

    /** 构造基地 */
    public static RoleInformation base(int id, int x, int y, int life) {
        return unit(id, "station", x, y, life);
    }

    public static Inquire inquire(int round, List<RoleInformation> objects) {
        Inquire q = new Inquire();
        q.setRound(round);
        q.setObjects(objects);
        return q;
    }
}
