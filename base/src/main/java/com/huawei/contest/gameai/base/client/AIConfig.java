package com.huawei.contest.gameai.base.client;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AIConfig {
    private double pathDangerWeight = 3.0;
    private double pathMaxDangerThreshold = 1.5;
    private double aggressiveDangerMod = 0.2;
    public static AIConfig aggressiveRush() { return new AIConfig(); }
}
