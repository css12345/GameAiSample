package com.huawei.contest.gameai.core;

import com.huawei.contest.gameai.base.client.GameClient;
import com.huawei.contest.gameai.base.client.GameHolder;
import com.huawei.contest.gameai.core.ai.client.RTSAIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Optional;

@Slf4j
@SpringBootApplication
public class CoreApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }

    @Override
    public void run(String[] args) {
        GameClient gameClient = null;
        try {
            String ip = args[1];
            int port = Integer.parseInt(args[2]);
            int playerId = Integer.parseInt(args[0]);
            RTSAIClient aiClient = new RTSAIClient();
            gameClient = GameClient.connect(ip, port)
                    .withPlayer(playerId, "Test Red", "v1.0")
                    .withTurnStrategy(aiClient);
            gameClient.start();
            while (!GameHolder.isGameOver()) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            log.error("Sleep is interrupted");
        } finally {
            Optional.ofNullable(gameClient).ifPresent(GameClient::stop);
        }
    }
}
