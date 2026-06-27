package com.huawei.contest.gameai.base.client.message;

import com.huawei.contest.gameai.base.client.model.Actions;

public class ActionsMessage extends Message<Actions> {
    public ActionsMessage() {
    }

    public ActionsMessage(Actions data) {
        super(data);
        this.name = "action";
    }
}
