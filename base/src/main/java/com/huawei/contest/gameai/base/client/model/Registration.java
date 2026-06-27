package com.huawei.contest.gameai.base.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Registration {
    private int playerId;

    private String playerName;

    private String version;

    public Registration() {
        this(1111, "test", "v1.0");
    }
}
