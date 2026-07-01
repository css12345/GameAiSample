package com.huawei.contest.gameai.base.client;

import com.huawei.contest.gameai.base.client.model.Registration;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GameClient {

    private final String host;

    private final int port;

    private Bootstrap bootstrap;

    private EventLoopGroup receivingGroup;

    private Channel connectChannel;

    private Registration gameRegistration = new Registration();

    private TurnStrategy turnStrategy = TurnStrategy.NO_OP;

    private GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 连接监听本地IP和指定端口的服务
     *
     * @param port 端口号
     * @return 游戏客户端
     */
    public static GameClient connect(int port) {
        return connect("127.0.0.1", port);
    }

    /**
     * 连接监听指定地址和端口的服务
     *
     * @param host 地址
     * @param port 端口号
     * @return 游戏客户端
     */
    public static GameClient connect(String host, int port) {
        return new GameClient(Objects.toString(host, "127.0.0.1"), port);
    }

    /**
     * 指定玩家信息
     *
     * @param playerId 玩家ID
     * @param playerName 玩家名称
     * @param version 版本
     * @return 游戏客户端
     */
    public GameClient withPlayer(int playerId, String playerName, String version) {
        gameRegistration = new Registration(playerId, playerName, version);
        return this;
    }

    /**
     * 指定回合决策策略（AI）。未调用时使用 NO_OP（每回合空动作）。
     */
    public GameClient withTurnStrategy(TurnStrategy strategy) {
        this.turnStrategy = strategy == null ? TurnStrategy.NO_OP : strategy;
        return this;
    }

    /**
     * 启动客户端
     */
    public void start() {
        GameHolder.reset();
        bootstrap = new Bootstrap();
        receivingGroup = new NioEventLoopGroup();
        bootstrap.group(receivingGroup)
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress(host, port))
                .handler(new GameClientInitializer(gameRegistration, turnStrategy));
        Executors.newSingleThreadExecutor().execute(() -> {
            while (true) {
                if (GameHolder.isGameOver()) {
                    log.info("Game is over, not need retry to connect Game Server {}-{}!", host, port);
                    shutdownGracefully();
                    break;
                }
                try {
                    connectChannel = bootstrap.connect(host, port).sync().channel();
                    log.info("Succeed to connect Game Server {}-{}!", host, port);
                    break;
                } catch (Exception ex) {
                    log.error("Failed to connect and wait seconds!");
                    waitToRetry();
                }
            }
        });
    }

    /**
     * 停止客户端
     */
    public void stop() {
        if (connectChannel != null) {
            try {
                connectChannel.closeFuture().sync();
            } catch (InterruptedException ex) {
                log.error("Failed to close connect channel cause by {}", ExceptionUtils.getRootCauseMessage(ex));
            }
        }
        shutdownGracefully();
    }

    private void shutdownGracefully() {
        Optional.ofNullable(receivingGroup).ifPresent(EventExecutorGroup::shutdownGracefully);
    }

    private void waitToRetry() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException ex) {
            log.error("Retry is interrupted!");
        }
    }
}
