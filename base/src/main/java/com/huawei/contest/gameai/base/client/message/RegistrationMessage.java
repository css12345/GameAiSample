package com.huawei.contest.gameai.base.client.message;

import com.huawei.contest.gameai.base.client.model.Registration;

public class RegistrationMessage extends Message<Registration> {
    public RegistrationMessage() {
    }

    public RegistrationMessage(Registration data) {
        super(data);
        this.name = "registration";
    }
}
