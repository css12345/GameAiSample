package com.huawei.contest.gameai.base.client.message;

import com.huawei.contest.gameai.base.client.model.Ready;

public class ReadyMessage extends Message<Ready> {

    public ReadyMessage() {
    }

    public ReadyMessage(Ready data) {
        super(data);
        this.name = "ready";
    }
}
