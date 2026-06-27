package com.huawei.contest.gameai.base.client.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Inquire {
    private int round;
    private List<Object> objects = new ArrayList<>();
}
