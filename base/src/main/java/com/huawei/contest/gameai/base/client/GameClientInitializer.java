package com.huawei.contest.gameai.base.client;

import com.huawei.contest.gameai.base.client.model.Registration;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

class GameClientInitializer extends ChannelInitializer<SocketChannel> {
    private final Registration gameRegistration;

    GameClientInitializer(Registration gameRegistration) {
        this.gameRegistration = gameRegistration;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));
        // 配置Server端处理器
        pipeline.addLast(new GameClientHandler(gameRegistration));
    }
}
