package org.example.service.lcu;

import java.util.List;

public record ChampSelectSnapshot(
        boolean inChampSelect,
        Side firstPickSide,
        List<String> allyBans,
        List<String> enemyBans,
        List<String> allyPicks,
        List<String> enemyPicks,
        String statusText
) {

    public enum Side {
        ALLY, ENEMY, UNKNOWN
    }

    public static ChampSelectSnapshot waiting(String message) {
        return new ChampSelectSnapshot(
                false,
                Side.UNKNOWN,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                message
        );
    }
}