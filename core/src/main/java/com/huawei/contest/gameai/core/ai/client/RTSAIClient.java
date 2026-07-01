package com.huawei.contest.gameai.core.ai.client;

import com.huawei.contest.gameai.base.client.TurnStrategy;
import com.huawei.contest.gameai.base.client.model.Inquire;
import com.huawei.contest.gameai.base.client.model.Start;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * RTS AI 客户端入口：实现 {@link TurnStrategy}，编排战略/战役/战术三层。
 *
 * <p>当前为骨架：onGameStart/onInquire 暂返回空，后续阶段填充三层逻辑。
 */
@Slf4j
public class RTSAIClient implements TurnStrategy {

    private int playerId;

    @Override
    public void onGameStart(Start start, int playerId) {
        this.playerId = playerId;
        log.info("RTSAIClient onGameStart playerId={}", playerId);
        // TODO: 解析地图、初始化世界状态/黑板/战役协调器
    }

    @Override
    public List<Object> onInquire(Inquire inquire, int playerId) {
        log.info("RTSAIClient onInquire round={} playerId={}", inquire.getRound(), playerId);
        // TODO: loadInquireData → campaign.update() → 每单位 decide() → 收集 actions
        return List.of();
    }
}
