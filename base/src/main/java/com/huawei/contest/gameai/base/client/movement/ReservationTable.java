package com.huawei.contest.gameai.base.client.movement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReservationTable {
    private final Map<Integer, Map<Integer, Integer>> timeSlots = new HashMap<>();

    public int getReservation(int step, int x, int y, int width) {
        return timeSlots.getOrDefault(step, Collections.emptyMap()).getOrDefault(y * width + x, -1);
    }

    public void reserve(int step, int x, int y, int width, int unitId) {
        timeSlots.computeIfAbsent(step, k -> new HashMap<>()).put(y * width + x, unitId);
    }

    public void clear() { timeSlots.clear(); }
}
