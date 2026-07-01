package com.huawei.contest.gameai.core.ai.blackboard;

import com.huawei.contest.gameai.base.client.entity.Position;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 支援请求：小队在寡不敌众时发出，附近/空闲小队可接受。
 */
@Getter
@Setter
public class SupportRequest {
    /** 请求方小队 ID */
    private final int requesterSquadId;
    /** 支援目标位置（请求方当前位置中心） */
    private final Position location;
    /** 紧急度：敌我兵力差，越大越紧急 */
    private final int urgency;
    /** 存活回合数（每回合 -1，归零失效） */
    private int ttl;
    /** 最大接受方数量 */
    private final int maxAcceptors;
    /** 已接受的小队 ID 列表 */
    private final List<Integer> acceptedBySquadIds = new ArrayList<>();

    public SupportRequest(int requesterSquadId, Position location, int urgency, int ttl, int maxAcceptors) {
        this.requesterSquadId = requesterSquadId;
        this.location = location;
        this.urgency = urgency;
        this.ttl = ttl;
        this.maxAcceptors = maxAcceptors;
    }

    /** 是否已被足够小队接受 */
    public boolean isSatisfied() {
        return acceptedBySquadIds.size() >= maxAcceptors;
    }

    /** 小队接受此请求 */
    public void accept(int squadId) {
        if (!acceptedBySquadIds.contains(squadId) && !isSatisfied()) {
            acceptedBySquadIds.add(squadId);
        }
    }
}
