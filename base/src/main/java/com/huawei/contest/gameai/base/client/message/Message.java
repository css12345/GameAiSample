package com.huawei.contest.gameai.base.client.message;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Message<T> {
    @JSONField(name = "msg_name")
    protected String name;

    @JSONField(name = "msg_data")
    protected T data;

    public Message() {
    }

    public Message(T data) {
        this.data = data;
    }
}
