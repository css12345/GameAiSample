package com.huawei.contest.gameai.base.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public abstract class MessageUtils {
    /**
     * 格式化数据为交互消息
     * <p>
     * 消息长度 + JSON字符串
     *
     * @param data 对象
     * @return 格式化后的消息
     */
    public static String format(Object data) {
        String msg = JSON.toJSONString(data, SerializerFeature.DisableCircularReferenceDetect);
        String uniMsg = string2Unicode(msg);
        return String.format(Locale.ENGLISH, "%05d%s", uniMsg.length(), uniMsg);
    }

    /**
     * 中文字符转换为unicode
     *
     * @param input 输入字符串
     * @return 将中文编码后的json字符串
     */
    public static String string2Unicode(String input) {
        if (StringUtils.isBlank(input)) {
            return StringUtils.EMPTY;
        }

        char[] bytes = input.toCharArray();
        StringBuffer unicode = new StringBuffer();
        for (char ch : bytes) {
            // 标准ASCII范围内的字符，直接输出
            if (ch <= 127) {
                unicode.append(ch);
                continue;
            }

            String hexString = Integer.toHexString(ch);
            unicode.append("\\u");
            // 不够四位进行补0操作
            if (hexString.length() < 4) {
                unicode.append("0000", hexString.length(), 4);
            }
            unicode.append(hexString);
        }
        return unicode.toString();
    }
}
