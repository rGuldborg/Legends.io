package org.example.model;

public record Summoner(
    String puuid,
    String gameName,
    String tagLine,
    int profileIconId,
    long summonerLevel
) {}
