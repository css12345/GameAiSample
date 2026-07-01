package com.huawei.contest.gameai.core.ai.blackboard;

import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.core.ai.config.RTSConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局共享黑板：战略/战役/战术层共享的态势信息与信号。
 */
@Getter
@Setter
public class BaseBlackboard {
    // ===== 全局姿态与信号 =====
    /** 当前战略姿态：RUSH / DEFEND / ECONOMY / HARASS */
    private String currentStance = "ECONOMY";
    /** 主目标 ID（敌方基地/守护者等） */
    private int primaryTargetId = -1;
    /** 全局撤退信号 */
    private boolean retreatSignal = false;
    /** 集结信号 */
    private boolean groupUpSignal = false;
    /** 全局防御信号（基地告急） */
    private boolean globalDefendSignal = false;
    /** 全局防御目标位置 */
    private Position globalDefendTarget;

    // ===== 基地位置 =====
    private Position myBasePos;
    private Position enemyBasePos;

    // ===== 支援系统 =====
    private List<SupportRequest> supportRequests = new ArrayList<>();

    // ===== 火力分配 =====
    /** unitId → 分配的攻击目标 ID（由 GoalExecutor 填充，战术层读取） */
    private Map<Integer, Integer> assignedTargets = new HashMap<>();

    // ===== 配置 =====
    private RTSConfig config = new RTSConfig();

    /** 清空回合间临时状态（信号、火力分配、已过期的支援请求） */
    public void cleanRequests() {
        // 衰减 TTL 并移除过期支援请求
        supportRequests.removeIf(r -> {
            r.setTtl(r.getTtl() - 1);
            return r.getTtl() <= 0;
        });
        assignedTargets.clear();
        // 信号每回合由战略层重新评估
        retreatSignal = false;
        globalDefendSignal = false;
        groupUpSignal = false;
    }

    /** 添加支援请求 */
    public void addSupportRequest(SupportRequest req) {
        supportRequests.add(req);
    }
}
