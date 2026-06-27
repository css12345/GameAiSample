package com.huawei.contest.gameai.base.client.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Message<T> {
    protected String name;

    protected T data;

    public Message() {
    }

    public Message(T data) {
        this.data = data;
    }
}
