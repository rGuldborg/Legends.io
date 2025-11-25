package org.example.model;

public record RankedStats(
    String tier,
    String rank,
    int leaguePoints,
    int wins,
    int losses
) {}
