package com.huawei.contest.gameai.base.client.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StartPlayer {
    private Integer playerId;

    private ObjectIdRange objectIdRange;
}
