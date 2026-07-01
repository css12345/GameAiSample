package com.huawei.contest.gameai.core.ai.action;

/**
 * RTS 动作 DTO 抽象基类。子类设置 action 类型字段并补充专用字段。
 *
 * <p>fastjson 按字段名序列化为服务端协议 JSON，例如：
 * <pre>{"id":100444,"action":"move","position":[6,7]}</pre>
 */
public abstract class RtsAction {
    /** 执行单位/基地 ID */
    public int id;
    /** 动作类型：move/attack/bomb/pick/build/heal/produce */
    public String action;

    protected RtsAction(int id, String action) {
        this.id = id;
        this.action = action;
    }
}
