package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class WinPlay {
    private int wins;
    private int games;

    public WinPlay() {
        this(0, 0);
    }

    public WinPlay(int wins, int games) {
        this.wins = wins;
        this.games = games;
    }

    public void add(boolean win) {
        games++;
        if (win) wins++;
    }

    public int wins() { return wins; }
    public int games() { return games; }

    public int getWins() { return wins; }
    public int getGames() { return games; }

    @JsonIgnore
    public double winRate() { return games == 0 ? 0.0 : (double) wins / games; }
}
