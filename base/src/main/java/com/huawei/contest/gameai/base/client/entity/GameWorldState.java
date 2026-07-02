package com.huawei.contest.gameai.base.client.entity;

import com.huawei.contest.gameai.base.client.model.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GameWorldState implements IWorldState {
    private int width;
    private int height;
    private boolean[][] staticObstacles;
    private final Set<Position> occupiedPositions = new HashSet<>();
    private StartPlayer my;
    private StartPlayer opponent;

    private int round;
    @Getter
    private final Map<Integer, GameUnit> units = new HashMap<>();
    private final Map<Integer, GameBase> bases = new HashMap<>();
    private final Map<Integer, GameResource> resources = new HashMap<>();


    public static GameWorldState fromMapString(Start start, int myPlayerId) {
        GameWorldState state = new GameWorldState();
        List<StartPlayer> players = start.getPlayers();
        if (players.get(0).getPlayerId() == myPlayerId) {
            state.my = players.get(0);
            state.opponent = players.get(1);
        } else {
            state.my = players.get(1);
            state.opponent = players.get(0);
        }


        StartMap startMap = start.getMap();
        String mapData = startMap.getData();
        state.width = startMap.getMaxX();
        state.height = startMap.getMaxY();
        state.staticObstacles = new boolean[startMap.getMaxX()][startMap.getMaxY()];
        String[] tokens = mapData.split(",");
        for (int mapY = 0; mapY < state.height; mapY++) {          // mapY: 0 是字符串中的第0行（最上面）
            int worldY = state.height - 1 - mapY;                 // 转换到世界坐标：最上面是 y=height-1
            for (int x = 0; x < state.width; x++) {
                int idx = mapY * state.width + x;
                if (idx >= tokens.length) {
                    log.error("invalid idx {}, width is {}, height is {}, x is {}, mapY is {}", idx, state.width, state.height, x, mapY);
                    break;            // 安全保护
                }
                String token = tokens[idx].trim();
                char code = token.trim().charAt(0);
                Terrain t = Terrain.fromCode(code);
                if (t.isStaticObstacle()) {
                    state.staticObstacles[x][worldY] = true;
                }
            }
        }
        return state;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public boolean isWalkableForPlanning(int x, int y, Set<Position> vacatedPostiones) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        if (staticObstacles[x][y]) {
            return false;
        }
        if(!occupiedPositions.contains(Position.of(x, y))) {
            return true;
        }

        return vacatedPostiones != null && vacatedPostiones.contains(Position.of(x, y));
    }

    @Override
    public boolean isWalkable(int x, int y) {
        return isWalkableForPlanning(x, y, Collections.emptySet());
    }

    public void refreshOccupied() {
        occupiedPositions.clear();
        for (GameUnit u : units.values()) {
            if (u.isAlive()) {
                occupiedPositions.add(u.getPos());
            }
        }
        for (GameBase b : bases.values()) {
            if (b.isAlive()) {
                occupiedPositions.add(b.getPos());
            }
        }
        for (GameResource r : resources.values()) {
            if (r.isAlive()) {
                occupiedPositions.add(r.getPos());
            }
        }
    }

    /** 获取所有资源（金矿/宝石矿） */
    public Map<Integer, GameResource> getResources() {
        return resources;
    }

    @Override
    public List<? extends IUnit> getMyUnits() {
        return units.values().stream()
                .filter(u -> u.getPlayerId() == my.getPlayerId() && u.isAlive())
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends IUnit> getEnemyUnits() {
        return units.values().stream()
                .filter(u -> u.getPlayerId() == opponent.getPlayerId() && u.isAlive())
                .collect(Collectors.toList());
    }

    @Override
    public List<IBase> getMyBases() {
        return bases.values().stream()
                .filter(b -> b.getPlayerId() == my.getPlayerId() && b.isAlive())
                .collect(Collectors.toList());
    }

    @Override
    public List<IBase> getEnemyBases() {
        return bases.values().stream()
                .filter(b -> b.getPlayerId() == opponent.getPlayerId() && b.isAlive())
                .collect(Collectors.toList());
    }

    @Override
    public int opponent() {
        return opponent.getPlayerId();
    }

    public void loadInquireData(Inquire inquire) {
        units.clear();
        bases.clear();
        resources.clear();
        round = inquire.getRound();
        for (RoleInformation object : inquire.getObjects()) {
            int id = object.getId();
            int playerId;
            if (my.getObjectIdRange().getMin() <= id && id <= my.getObjectIdRange().getMax()) {
                playerId = my.getPlayerId();
            } else {
                playerId = opponent.getPlayerId();
            }

            int x = object.getPosition()[0];
            int y = object.getPosition()[1];
            int life = object.getLife();

            switch (object.getRole()) {
                case "station":
                    bases.put(id, new GameBase(id, playerId, new Position(x, y), life, object.getBuilding(), object.getProducing()));
                    break;
                case "miner":
                    units.put(id, new GameUnit(id, playerId, UnitType.MINER, new Position(x, y), life));
                    break;
                case "fighter":
                    units.put(id, new GameUnit(id, playerId, UnitType.FIGHTER, new Position(x, y), life));
                    break;
                case "rocket":
                    units.put(id, new GameUnit(id, playerId, UnitType.ROCKET, new Position(x, y), life));
                    break;
                case "medic":
                    units.put(id, new GameUnit(id, playerId, UnitType.MEDIC, new Position(x, y), life));
                    break;
                case "guardian":
                    units.put(id, new GameUnit(id, -1, UnitType.GUARDIAN, new Position(x, y), life));
                    break;
                case "goldmine", "gemmine":
                    resources.put(id, new GameResource(id, new Position(x, y), object.getGold()));
                    break;
            }
        }

    }
}
