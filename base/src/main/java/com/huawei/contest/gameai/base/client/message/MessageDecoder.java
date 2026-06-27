package com.huawei.contest.gameai.base.client.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class MessageDecoder {
    private StringBuffer receivingMessage = new StringBuffer();

    private String finalMessage = "";

    private JSONObject jsonObject;

    private int length = -1;

    @Getter
    private boolean expired = false;

    /**
     * 接收消息
     *
     * @param next 下一段消息
     */
    public void receive(String next) {
        receivingMessage.append(next);
    }

    /**
     * 是否结束了
     *
     * @return 结束返回{@code true}，未结束返回{@code false}
     */
    public boolean isFinished() {
        if (length == -1) {
            String prefix = receivingMessage.substring(0, 5);
            length = Integer.parseInt(prefix);
        }
        String jsonString = receivingMessage.substring(5);
        int realLength = jsonString.length();
        // 如果接收够了
        if (realLength >= length) {
            // 这是第一部分消息
            finalMessage = receivingMessage.substring(0, length + 5);
            jsonObject = JSON.parseObject(getReceivingMessage());
            length = -1;
            // 剩余的接上缓存上
            if (finalMessage.length() < receivingMessage.length()) {
                receivingMessage = new StringBuffer(receivingMessage.substring(finalMessage.length()));
                expired = true;
            } else {
                receivingMessage = new StringBuffer();
                expired = false;
            }
            return true;
        }
        return false;
    }

    /**
     * 获取消息名称
     *
     * @return 消息名称
     */
    public String getMessageName() {
        return Objects.toString(jsonObject.getString("msg_name"), "");
    }

    /**
     * 获取消息数据
     *
     * @return 消息数据
     */
    public synchronized String getMessageData() {
        return Objects.toString(jsonObject.getString("msg_data"), "");
    }

    public String getReceivingMessage() {
        return finalMessage.substring(5);
    }
}
