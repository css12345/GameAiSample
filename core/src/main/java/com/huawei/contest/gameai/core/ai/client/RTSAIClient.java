package com.huawei.contest.gameai.core.ai.client;

import com.huawei.contest.gameai.base.client.TurnStrategy;
import com.huawei.contest.gameai.base.client.entity.*;
import com.huawei.contest.gameai.base.client.model.Inquire;
import com.huawei.contest.gameai.base.client.model.Start;
import com.huawei.contest.gameai.core.ai.action.ProduceAction;
import com.huawei.contest.gameai.core.ai.action.RtsAction;
import com.huawei.contest.gameai.core.ai.blackboard.BaseBlackboard;
import com.huawei.contest.gameai.core.ai.campaign.RTSCampaignCoordinator;
import com.huawei.contest.gameai.core.ai.campaign.RTSProductionManager;
import com.huawei.contest.gameai.core.ai.campaign.Squad;
import com.huawei.contest.gameai.core.ai.executor.*;
import com.huawei.contest.gameai.core.ai.strategy.GoalType;
import com.huawei.contest.gameai.core.ai.strategy.RTSGoalGenerator;
import com.huawei.contest.gameai.core.ai.strategy.RTSStanceEvaluator;
import com.huawei.contest.gameai.core.ai.tactical.RTSUnitAgent;
import com.huawei.contest.gameai.core.ai.tactical.UnitAgent;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * RTS AI 客户端：实现 {@link TurnStrategy}，编排战略/战役/战术三层。
 *
 * <p>onGameStart: 解析地图、初始化世界/黑板/战役协调器、注册执行器。
 * <p>onInquire: 加载快照→战役层 update→每单位行为树决策→收集动作+生产命令。
 */
@Slf4j
public class RTSAIClient implements TurnStrategy {

    private int playerId;
    private GameWorldState world;
    private final BaseBlackboard bb = new BaseBlackboard();
    private RTSCampaignCoordinator campaign;
    private final UnitAgent agent = new RTSUnitAgent();
    private Start startData;

    @Override
    public void onGameStart(Start start, int playerId) {
        this.playerId = playerId;
        this.startData = start;
        this.world = GameWorldState.fromMapString(start, playerId);
        // 黑板：基地位置
        if (!world.getMyBases().isEmpty()) {
            bb.setMyBasePos(world.getMyBases().get(0).getPos());
        }
        if (!world.getEnemyBases().isEmpty()) {
            bb.setEnemyBasePos(world.getEnemyBases().get(0).getPos());
        }
        // 注册目标执行器
        bb.registerExecutor(GoalType.ATTACK, new RTSAttackExecutor());
        bb.registerExecutor(GoalType.DEFEND, new RTSDefendExecutor());
        bb.registerExecutor(GoalType.HARASS_MINERS, new RTSHarassExecutor());
        bb.registerExecutor(GoalType.SCOUT, new RTSScoutExecutor());
        bb.registerExecutor(GoalType.FARM_NEUTRAL, new RTSFarmNeutralExecutor());
        bb.registerExecutor(GoalType.SUPPORT, new RTSSupportExecutor());
        bb.registerExecutor(GoalType.CLEAR_GUARDIAN, new RTSClearGuardianExecutor());
        // 战役协调器
        RTSProductionManager prod = new RTSProductionManager(bb);
        campaign = new RTSCampaignCoordinator(bb,
                new RTSStanceEvaluator(), new RTSGoalGenerator(), prod);
        log.info("RTSAIClient 初始化完成 playerId={} 地图={}x{}", playerId, world.getWidth(), world.getHeight());
    }

    @Override
    public List<Object> onInquire(Inquire inquire, int playerId) {
        if (world == null || campaign == null) {
            log.warn("世界未初始化，跳过回合 {}", inquire.getRound());
            return List.of();
        }
        // 1. 加载服务端快照
        world.loadInquireData(inquire);
        world.refreshOccupied();
        // 2. 战役层更新（战略目标→小队分配→移动计划→执行器火力分配→FSM）
        campaign.update(world, playerId);
        // 3. 每单位行为树决策
        List<Object> actions = new ArrayList<>();
        Map<Integer, Squad> unitToSquad = buildUnitSquadMap();
        for (IUnit u : world.getMyUnits()) {
            if (!u.isAlive()) continue;
            Squad squad = unitToSquad.get(u.getId());
            try {
                RtsAction action = agent.decide(u, world, bb, squad);
                if (action != null) {
                    actions.add(action);
                }
            } catch (Exception ex) {
                log.error("单位 {} 决策失败: {}", u.getId(), ex.getMessage());
            }
        }
        // 4. 生产命令
        for (int[] entry : campaign.getProductionManager().getProduceQueue()) {
            int baseId = entry[0];
            UnitType type = UnitType.values()[entry[1]];
            actions.add(new ProduceAction(baseId, type));
        }
        log.info("回合 {} 产出 {} 个动作", inquire.getRound(), actions.size());
        return actions;
    }

    /** 构建 unitId → 所属小队 映射 */
    private Map<Integer, Squad> buildUnitSquadMap() {
        Map<Integer, Squad> map = new HashMap<>();
        for (Squad s : campaign.getSquads().values()) {
            for (IUnit u : s.getUnits()) {
                map.put(u.getId(), s);
            }
        }
        return map;
    }
}
