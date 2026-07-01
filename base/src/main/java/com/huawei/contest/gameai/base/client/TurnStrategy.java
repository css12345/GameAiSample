package com.huawei.contest.gameai.base.client;

import com.huawei.contest.gameai.base.client.model.Inquire;
import com.huawei.contest.gameai.base.client.model.Start;

import java.util.List;

/**
 * 回合决策接口：base 模块定义，由具体 AI 实现（如 core 模块的 RTSAIClient）。
 *
 * <p>base 不依赖任何 AI 实现，仅通过此接口把每回合的 {@link Inquire} 交给 AI，
 * 由 AI 返回要发送给服务端的动作列表。动作元素可以是任意可被 fastjson 序列化
 * 的对象（推荐强类型 DTO，字段名匹配服务端协议）。
 */
public interface TurnStrategy {

    /** 游戏开始：收到地图与玩家配置，AI 可在此初始化世界状态/黑板等。 */
    void onGameStart(Start start, int playerId);

    /** 每回合询问：基于服务端下发的世界快照决策，返回动作列表（元素为 Action DTO）。 */
    List<Object> onInquire(Inquire inquire, int playerId);

    /** 默认空实现：不决策，每回合返回空动作（保持原有行为）。 */
    TurnStrategy NO_OP = new TurnStrategy() {
        @Override public void onGameStart(Start start, int playerId) { }
        @Override public List<Object> onInquire(Inquire inquire, int playerId) { return List.of(); }
    };
}
