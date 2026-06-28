package com.huawei.contest.gameai.base.client;

import com.huawei.contest.gameai.base.client.message.ActionsMessage;
import com.huawei.contest.gameai.base.client.message.MessageDecoder;
import com.huawei.contest.gameai.base.client.message.ReadyMessage;
import com.huawei.contest.gameai.base.client.message.RegistrationMessage;
import com.huawei.contest.gameai.base.client.model.Actions;
import com.huawei.contest.gameai.base.client.model.Inquire;
import com.huawei.contest.gameai.base.client.model.Ready;
import com.huawei.contest.gameai.base.client.model.Registration;
import com.huawei.contest.gameai.base.client.model.Start;

import com.alibaba.fastjson.JSON;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;

@Slf4j
public class GameClientHandler extends SimpleChannelInboundHandler<String> {
    private static final String GAME_START = "gameStart";

    private static final String GAME_INQUIRE = "inquire";

    private static final String GAME_OVER = "gameOver";

    private final MessageDecoder messageDecoder = new MessageDecoder();

    private final Registration gameRegistration;

    public GameClientHandler(Registration gameRegistration) {
        this.gameRegistration = gameRegistration;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Connect to server {}", ctx.channel().remoteAddress());
        register(ctx);
        super.channelActive(ctx);
    }

    private void register(ChannelHandlerContext ctx) {
        ChannelFuture future = ctx.writeAndFlush(MessageUtils.format(new RegistrationMessage(gameRegistration)));
        future.addListener((ChannelFutureListener) channelFuture -> {
            // 消息实际发送成功或失败都记录一个日志，级别不同
            if (future.isSuccess()) {
                log.info("Succeed to register player {}!", gameRegistration);
            } else {
                log.error("Failed to register, retry again...!");
                Thread.sleep(500);
                register(ctx);
            }
        });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        messageDecoder.receive(msg);
        if (!messageDecoder.isFinished() || messageDecoder.isExpired()) {
            return;
        }
        try {
            String messageName = messageDecoder.getMessageName();
            int playerId = gameRegistration.getPlayerId();
            if (GAME_START.equals(messageName)) {
                log.info("Player {} receive start message!", playerId);
                // 游戏开始，记录地图和玩家数据，初始化优秀，然后响应READY
                Start start = JSON.parseObject(messageDecoder.getMessageData(), Start.class);
                // 响应
                ctx.writeAndFlush(MessageUtils.format(prepareForGame(start)));
            } else if (GAME_INQUIRE.equals(messageName)) {
                // 询问动作，先刷新服务端传来的活动对象数据，推算动作，然后响应消息
                Inquire inquire = JSON.parseObject(messageDecoder.getMessageData(), Inquire.class);
                log.info("Player {} received round {} inquire message!", playerId, inquire.getRound());
                ctx.writeAndFlush(MessageUtils.format(actionForGame(inquire)));
                // 返回响应
            } else if (GAME_OVER.equals(messageName)) {
                log.info("Player {} game over!", playerId);
                GameHolder.gameOver();
            } else {
                log.info("Player {} game over with unknown message name: {}!", playerId, messageName);
            }
        } catch (Exception ex) {
            log.error("Failed to play game cause by {}", ExceptionUtils.getStackTrace(ex));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Failed to connect cause by {}", ExceptionUtils.getStackTrace(cause));
        // 释放资源
        ctx.close();
    }

    private ReadyMessage prepareForGame(Start gameStart) {
        Ready gameReady = new Ready();
        gameReady.setPlayerId(gameRegistration.getPlayerId());
        log.info("gameStart is {}", gameStart);
        // TODO: 完成游戏准备操作
        return new ReadyMessage(gameReady);
    }

    private ActionsMessage actionForGame(Inquire gameInquire) {
        Actions gameActions = new Actions();
        gameActions.setPlayerId(gameRegistration.getPlayerId());
        gameActions.setRound(gameInquire.getRound());
        log.info("gameInquire is {}", gameInquire);
        // TODO: 添加行动动作
        gameActions.setActions(new ArrayList<>());
        return new ActionsMessage(gameActions);
    }
}
