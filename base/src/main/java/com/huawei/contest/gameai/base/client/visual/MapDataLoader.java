package com.huawei.contest.gameai.base.client.visual;

import com.huawei.contest.gameai.base.client.model.ObjectIdRange;
import com.huawei.contest.gameai.base.client.model.Start;
import com.huawei.contest.gameai.base.client.model.StartMap;
import com.huawei.contest.gameai.base.client.model.StartPlayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 地图数据管理器，从文件系统加载地图。
 * 地图文件位于 base/src/test/resources/maps/ 目录。
 */
public class MapDataLoader {

    public record MapInfo(String name, int width, int height, String data) {}

    private static final Map<String, MapInfo> MAPS = new LinkedHashMap<>();

    // 地图文件所在目录
    private static final String MAPS_DIR = "base/src/test/resources/maps";

    // 所有标准地图均为 40x40
    private static final int DEFAULT_SIZE = 40;

    static {
        reload();
    }

    /** 从文件系统重新加载所有地图 */
    public static void reload() {
        MAPS.clear();
        Path dir = Paths.get(MAPS_DIR);
        if (!Files.isDirectory(dir)) {
            // 尝试从 classpath 加载
            loadFromClasspath();
            return;
        }
        try (var files = Files.list(dir)) {
            files.filter(f -> f.toString().endsWith(".txt"))
                    .sorted()
                    .forEach(MapDataLoader::loadFromFile);
        } catch (IOException e) {
            System.err.println("加载地图失败: " + e.getMessage());
        }
        if (MAPS.isEmpty()) {
            loadFromClasspath();
        }
    }

    private static void loadFromFile(Path file) {
        try {
            String name = file.getFileName().toString().replace(".txt", "");
            String data = Files.readString(file).trim();
            MAPS.put(name, new MapInfo(name, DEFAULT_SIZE, DEFAULT_SIZE, data));
            System.out.println("已加载地图: " + name + " (" + DEFAULT_SIZE + "x" + DEFAULT_SIZE + ")");
        } catch (IOException e) {
            System.err.println("加载地图文件失败 " + file + ": " + e.getMessage());
        }
    }

    /** classpath 回退：从 test resources 加载 */
    private static void loadFromClasspath() {
        String[] names = {"default", "blue_storm", "3_corridors", "lost_temple",
                "turtle_rock", "twisted_meadows", "xy"};
        for (String name : names) {
            String resourcePath = "maps/" + name + ".txt";
            InputStream is = MapDataLoader.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                // 再次尝试不带 maps/ 前缀
                is = MapDataLoader.class.getResourceAsStream("/maps/" + name + ".txt");
            }
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line.trim());
                    }
                    MAPS.put(name, new MapInfo(name, DEFAULT_SIZE, DEFAULT_SIZE, sb.toString()));
                } catch (IOException e) {
                    System.err.println("加载 classpath 地图失败 " + name + ": " + e.getMessage());
                }
            }
        }
    }

    public static Map<String, MapInfo> getMaps() {
        return MAPS;
    }

    public static MapInfo getMap(String name) {
        return MAPS.get(name);
    }

    /** 构建 Start 对象，用于 GameWorldState.fromMapString() */
    public static Start buildStart(String mapName, int myPlayerId, int opponentPlayerId) {
        MapInfo info = MAPS.get(mapName);
        if (info == null) {
            throw new IllegalArgumentException("Unknown map: " + mapName + ". Available: " + MAPS.keySet());
        }
        return buildStart(info.width, info.height, info.data, myPlayerId, opponentPlayerId);
    }

    /** 用自定义 map 数据构建 Start */
    public static Start buildStart(int width, int height, String mapData, int myPlayerId, int opponentPlayerId) {
        Start start = new Start();

        StartMap startMap = new StartMap();
        startMap.setData(mapData);
        startMap.setMaxX(width);
        startMap.setMaxY(height);
        start.setMap(startMap);

        StartPlayer player1 = new StartPlayer();
        player1.setPlayerId(myPlayerId);
        ObjectIdRange range1 = new ObjectIdRange();
        range1.setMin(100);
        range1.setMax(200);
        player1.setObjectIdRange(range1);

        StartPlayer player2 = new StartPlayer();
        player2.setPlayerId(opponentPlayerId);
        ObjectIdRange range2 = new ObjectIdRange();
        range2.setMin(500);
        range2.setMax(600);
        player2.setObjectIdRange(range2);

        start.setPlayers(List.of(player1, player2));
        return start;
    }
}
