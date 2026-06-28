package com.huawei.contest.gameai.base.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;

@Slf4j
class GameClientTest {
    private static final String TEST_SERVER_HOST = "10.189.52.49";
    private static final int TEST_SERVER_PORT = 30000;

    @Test
    @Disabled
    void testSinglePlayer() {
        GameClient redClient = null;
        try {
            redClient = GameClient.connect(TEST_SERVER_HOST, TEST_SERVER_PORT).withPlayer(1111, "Test Red", "v1.0");
            redClient.start();
            while (!GameHolder.isGameOver()) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            log.error("Sleep is interrupted");
        } finally {
            Optional.ofNullable(redClient).ifPresent(GameClient::stop);
        }
    }

    @Test
    @Disabled
    void testMultiPlayer() {
        GameClient redClient = null;
        GameClient blueClient = null;
        try {
            redClient = GameClient.connect(TEST_SERVER_HOST, TEST_SERVER_PORT).withPlayer(1111, "Red", "v1.0");
            redClient.start();
            blueClient = GameClient.connect(TEST_SERVER_HOST, TEST_SERVER_PORT).withPlayer(2222, "Blue", "v1.0");
            blueClient.start();
            while (!GameHolder.isGameOver()) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            log.error("Sleep is interrupted");
        } finally {
            Optional.ofNullable(redClient).ifPresent(GameClient::stop);
            Optional.ofNullable(blueClient).ifPresent(GameClient::stop);
        }
    }
}