package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ChampionStats {
    private int wins;
    private int games;
    private Map<String, Integer> roleCounts;
    private Map<String, WinPlay> synergy;
    private Map<String, WinPlay> counters;

    public ChampionStats() {
        this(0, 0, new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public ChampionStats(int wins, int games, Map<String, Integer> roleCounts, Map<String, WinPlay> synergy, Map<String, WinPlay> counters) {
        this.wins = wins;
        this.games = games;
        this.roleCounts = roleCounts != null ? roleCounts : new HashMap<>();
        this.synergy = synergy != null ? synergy : new HashMap<>();
        this.counters = counters != null ? counters : new HashMap<>();
    }

    public void addGame(boolean win, String role) {
        games++;
        if (win) wins++;
        if (role != null && !role.isBlank()) {
            if (roleCounts == null) roleCounts = new HashMap<>();
            roleCounts.merge(role, 1, Integer::sum);
        }
    }

    public void addSynergy(String allyChamp, boolean win) {
        if (synergy == null) synergy = new HashMap<>();
        synergy.computeIfAbsent(allyChamp, k -> new WinPlay()).add(win);
    }

    public void addCounter(String enemyChamp, boolean win) {
        if (counters == null) counters = new HashMap<>();
        counters.computeIfAbsent(enemyChamp, k -> new WinPlay()).add(win);
    }

    public int wins() { return wins; }
    public int games() { return games; }
    public Map<String, Integer> roleCounts() { return roleCounts; }
    public Map<String, WinPlay> synergy() { return synergy; }
    public Map<String, WinPlay> counters() { return counters; }

    public int getWins() { return wins; }
    public int getGames() { return games; }
    public Map<String, Integer> getRoleCounts() { return roleCounts; }
    public Map<String, WinPlay> getSynergy() { return synergy; }
    public Map<String, WinPlay> getCounters() { return counters; }

    @JsonIgnore
    public double winRate() {
        return games == 0 ? 0.0 : (double) wins / games;
    }

    private static final double ROLE_SHARE_THRESHOLD = 0.02;

    @JsonIgnore
    public Role primaryRole() {
        return allRoles().stream().findFirst().orElse(Role.UNKNOWN);
    }

    @JsonIgnore
    public List<Role> allRoles() {
        if (roleCounts == null || roleCounts.isEmpty()) {
            return List.of(Role.UNKNOWN);
        }
        int totalGames = Math.max(games, roleCounts.values().stream().mapToInt(Integer::intValue).sum());
        int minimumRoleGames = totalGames > 0 ? (int) Math.ceil(totalGames * ROLE_SHARE_THRESHOLD) : 0;

        record RoleShare(Role role, int count) {}

        List<RoleShare> sortedRoles = roleCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> new RoleShare(mapRole(entry.getKey()), entry.getValue()))
                .filter(share -> share.role() != Role.UNKNOWN)
                .toList();

        List<Role> roles = sortedRoles.stream()
                .filter(share -> share.count() >= minimumRoleGames)
                .map(RoleShare::role)
                .distinct()
                .limit(2)
                .collect(Collectors.toList());

        if (roles.isEmpty()) {
            roles = sortedRoles.stream()
                    .map(RoleShare::role)
                    .distinct()
                    .limit(Math.min(2, sortedRoles.size()))
                    .toList();
        }

        return roles.isEmpty() ? List.of(Role.UNKNOWN) : roles;
    }

    private Role mapRole(String lane) {
        if (lane == null) return Role.UNKNOWN;
        return switch (lane.toUpperCase()) {
            case "TOP" -> Role.TOP;
            case "JUNGLE" -> Role.JUNGLE;
            case "MIDDLE", "MID" -> Role.MID;
            case "ADC", "BOTTOM", "BOT" -> Role.BOTTOM;
            case "SUPPORT", "UTILITY" -> Role.SUPPORT;
            default -> Role.UNKNOWN;
        };
    }
}