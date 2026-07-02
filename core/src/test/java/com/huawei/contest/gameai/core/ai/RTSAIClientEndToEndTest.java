package com.huawei.contest.gameai.core.ai;

import com.huawei.contest.gameai.base.client.entity.UnitType;
import com.huawei.contest.gameai.base.client.model.RoleInformation;
import com.huawei.contest.gameai.base.client.model.Start;
import com.huawei.contest.gameai.core.ai.action.*;
import com.huawei.contest.gameai.core.ai.client.RTSAIClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端：用日志中的 Inquire(round=100) 数据喂给 RTSAIClient，
 * 断言产出的 actions 非空且符合协议。
 *
 * <p>场景：玩家 100xxx（基地@(35,35)，4矿工@(34-36,34-35)）vs 玩家 200xxx
 * （基地@(4,4)，5矿工@(1-3,1-3)，3医疗@(3-5,3)）。
 */
class RTSAIClientEndToEndTest {
    private RTSAIClient ai;
    private final int myId = 100001; // 注意：基地 id 用 100001，单位 id 100002+
    private final int oppId = 200001;

    @BeforeEach
    void setUp() {
        ai = new RTSAIClient();
        Start start = TestFixture.buildOpenMap(myId, oppId);
        ai.onGameStart(start, myId);
    }

    @Test
    void shouldProduceActionsForRound100() {
        List<RoleInformation> objs = round100Objects();
        var inquire = TestFixture.inquire(100, objs);
        List<Object> actions = ai.onInquire(inquire, myId);

        assertThat(actions).isNotEmpty();
        // 所有动作必须有 id 和 action 字段
        for (Object a : actions) {
            assertThat(a).isInstanceOf(RtsAction.class);
            RtsAction ra = (RtsAction) a;
            assertThat(ra.id).isPositive();
            assertThat(ra.action).isNotBlank();
        }
        System.out.println("Round 100 actions: " + actions.size());
        actions.forEach(System.out::println);
    }

    @Test
    void minersShouldPickOrMove() {
        List<RoleInformation> objs = round100Objects();
        var inquire = TestFixture.inquire(100, objs);
        List<Object> actions = ai.onInquire(inquire, myId);

        // 至少有一些动作产出（移动/采矿/生产）
        assertThat(actions).isNotEmpty();
        // 验证动作类型合法
        Set<String> validActions = Set.of("move", "attack", "bomb", "pick", "build", "heal", "produce");
        for (Object a : actions) {
            assertThat(((RtsAction) a).action).isIn(validActions);
        }
    }

    /** 构造 round=100 的对象列表（基地+单位+资源+守护者） */
    private List<RoleInformation> round100Objects() {
        List<RoleInformation> objs = new ArrayList<>();
        // 资源
        objs.add(TestFixture.resource(1, "goldmine", 2, 1, 208));
        objs.add(TestFixture.resource(2, "goldmine", 38, 1, 500));
        objs.add(TestFixture.resource(3, "goldmine", 1, 2, 326));
        objs.add(TestFixture.resource(5, "gemmine", 25, 7, 1000));
        objs.add(TestFixture.resource(6, "gemmine", 25, 8, 1000));
        objs.add(TestFixture.resource(8, "gemmine", 30, 18, 1000));
        objs.add(TestFixture.resource(10, "goldmine", 20, 19, 500));
        objs.add(TestFixture.resource(11, "gemmine", 30, 19, 1000));
        objs.add(TestFixture.resource(12, "gemmine", 9, 20, 1000));
        objs.add(TestFixture.resource(13, "goldmine", 19, 20, 500));
        objs.add(TestFixture.resource(15, "gemmine", 9, 21, 1000));
        objs.add(TestFixture.resource(17, "gemmine", 14, 31, 1000));
        objs.add(TestFixture.resource(18, "gemmine", 14, 32, 1000));
        objs.add(TestFixture.resource(20, "goldmine", 38, 37, 500));
        objs.add(TestFixture.resource(21, "goldmine", 1, 38, 500));
        objs.add(TestFixture.resource(22, "goldmine", 37, 38, 500));
        // 守护者（中立，id<100 视为敌方）
        objs.add(TestFixture.unit(4, "guardian", 19, 7, 800));
        objs.add(TestFixture.unit(7, "guardian", 26, 8, 800));
        objs.add(TestFixture.unit(9, "guardian", 8, 19, 800));
        objs.add(TestFixture.unit(14, "guardian", 31, 20, 800));
        objs.add(TestFixture.unit(16, "guardian", 13, 31, 800));
        objs.add(TestFixture.unit(19, "guardian", 20, 32, 800));
        // 基地
        objs.add(TestFixture.base(200001, 4, 4, 3000));   // 敌方
        objs.add(TestFixture.base(100001, 35, 35, 3000)); // 己方
        // 敌方单位（200xxx）
        objs.add(TestFixture.unit(200002, "miner", 3, 2, 70));
        objs.add(TestFixture.unit(200003, "miner", 2, 2, 70));
        objs.add(TestFixture.unit(200004, "miner", 3, 1, 70));
        objs.add(TestFixture.unit(200005, "miner", 2, 3, 70));
        objs.add(TestFixture.unit(200006, "miner", 1, 3, 70));
        objs.add(TestFixture.unit(200007, "medic", 3, 3, 80));
        objs.add(TestFixture.unit(200008, "medic", 4, 3, 80));
        objs.add(TestFixture.unit(200009, "medic", 5, 3, 80));
        // 己方单位（100xxx）
        objs.add(TestFixture.unit(100002, "miner", 34, 34, 70));
        objs.add(TestFixture.unit(100003, "miner", 35, 34, 70));
        objs.add(TestFixture.unit(100004, "miner", 36, 34, 70));
        objs.add(TestFixture.unit(100005, "miner", 34, 35, 70));
        return objs;
    }
}
