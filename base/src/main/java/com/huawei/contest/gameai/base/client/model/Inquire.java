package com.huawei.contest.gameai.base.client.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class Inquire {
    private int round;
    private List<Object> objects = new ArrayList<>();
}
