package org.example.service;

import org.example.model.ChampionStats;
import org.example.model.ChampionSummary;
import org.example.model.Match;
import org.example.model.RankedStats;
import org.example.model.RecommendationContext;
import org.example.model.Summoner;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StatsService {
    List<ChampionSummary> fetchRecommended(RecommendationContext context);

    Optional<ChampionStats> findChampionStats(String championId);

    Map<String, ChampionStats> allChampionStats();

    Optional<ChampionSummary> fetchChampionSummary(String championId, RecommendationContext context);

    Optional<Summoner> fetchSummoner(String gameName, String tagLine, String region);

    List<Match> fetchMatches(String puuid, String region);

    Optional<RankedStats> fetchRankedStats(String summonerId, String region);
}
