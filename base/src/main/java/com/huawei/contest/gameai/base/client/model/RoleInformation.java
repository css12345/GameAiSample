package com.huawei.contest.gameai.base.client.model;

import lombok.Data;

@Data
public class RoleInformation {
    private int id;
    private String role;
    private int gold;
    private int life;
    private int[] position;
    private Producing producing;
    private Building building;
}
