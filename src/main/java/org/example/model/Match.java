package org.example.model;

import java.util.List;

public record Match(
    String matchId,
    long gameCreation,
    boolean win,
    String championName,
    int kills,
    int deaths,
    int assists,
    List<String> items
) {}
