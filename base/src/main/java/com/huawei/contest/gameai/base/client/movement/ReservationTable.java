package com.huawei.contest.gameai.base.client.movement;

import com.huawei.contest.gameai.base.client.entity.Position;

import java.util.*;

public class ReservationTable {
    private final Map<Integer, Map<Integer, Integer>> timeSlots = new HashMap<>();

    public int getReservation(int step, int x, int y, int width) {
        return timeSlots.getOrDefault(step, Collections.emptyMap()).getOrDefault(y * width + x, -1);
    }

    public void reserve(int step, int x, int y, int width, int unitId) {
        timeSlots.computeIfAbsent(step, k -> new HashMap<>()).put(y * width + x, unitId);
    }

    public void clear() { timeSlots.clear(); }

    /** 高效获取某个单位的所有预留 step（用于路径重建），按 step 升序 */
    public List<Map.Entry<Integer, Position>> getUnitReservations(int unitId, int width) {
        List<Map.Entry<Integer, Position>> result = new ArrayList<>();
        for (var stepEntry : timeSlots.entrySet()) {
            int step = stepEntry.getKey();
            for (var posEntry : stepEntry.getValue().entrySet()) {
                if (posEntry.getValue() == unitId) {
                    int posIdx = posEntry.getKey();
                    result.add(new AbstractMap.SimpleEntry<>(step, new Position(posIdx % width, posIdx / width)));
                }
            }
        }
        result.sort(Map.Entry.comparingByKey());
        return result;
    }
}
