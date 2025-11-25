package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.service.RiotStatsService;
import org.example.service.StatsService;
import org.example.model.Match;
import org.example.model.RankedStats;
import org.example.model.Summoner;
import javafx.scene.chart.LineChart;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProfileController {

    @FXML
    private ComboBox<String> regionComboBox;

    @FXML
    private TextField summonerNameField;

    @FXML
    private TextField hashtagField;

    @FXML
    private Button lookupButton;

    @FXML
    private ImageView summonerIconView;

    @FXML
    private Label summonerNameLabel;

    @FXML
    private ImageView rankIconView;

    @FXML
    private Label rankLabel;

    @FXML
    private ListView<String> matchesListView;

    @FXML
    private Button updateButton;

    @FXML
    private LineChart<String, Number> lpChart;

    private StatsService statsService;
    private String currentPatchVersion = "14.24.1"; // Default or fetch dynamically

    @FXML
    public void initialize() {
        regionComboBox.setItems(FXCollections.observableArrayList(
            "BR1", "EUN1", "EUW1", "JP1", "KR", "LA1", "LA2", "NA1", "OC1", "TR1", "RU"
        ));
        regionComboBox.getSelectionModel().select("EUW1");
        // Fetch current patch version for image URLs
        try (InputStream is = getClass().getResourceAsStream("/org/example/data/champion-map.json")) {
            if (is != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(is);
                currentPatchVersion = rootNode.get("version").asText("14.24.1");
            }
        } catch (Exception e) {
            System.err.println("Could not load current patch version: " + e.getMessage());
        }
    }

    @FXML
    private void onLookup() {
        String region = regionComboBox.getValue();
        String gameName = summonerNameField.getText();
        String tagLine = hashtagField.getText();

        if (region == null || gameName.isBlank() || tagLine.isBlank()) {
            // Display error message
            return;
        }

        statsService = new RiotStatsService(System.getenv("RIOT_API_KEY"), region);
        
        statsService.fetchSummoner(gameName, tagLine, region).ifPresentOrElse(summoner -> {
            summonerNameLabel.setText(summoner.gameName() + "#" + summoner.tagLine());
            // Update summoner icon
            String iconUrl = "http://ddragon.leagueoflegends.com/cdn/" + currentPatchVersion + "/img/profileicon/" + summoner.profileIconId() + ".png";
            summonerIconView.setImage(new Image(iconUrl));

            statsService.fetchRankedStats(summoner.puuid(), region).ifPresentOrElse(rankedStats -> {
                rankLabel.setText(rankedStats.tier() + " " + rankedStats.rank() + " " + rankedStats.leaguePoints() + "LP");
                // Update rank icon
                String rankIconUrl = "/org/example/images/tiers/" + rankedStats.tier().toLowerCase() + ".png"; // Assuming resource path
                rankIconView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(rankIconUrl)).toString()));
            }, () -> {
                rankLabel.setText("Unranked");
                rankIconView.setImage(null);
            });

            List<Match> matches = statsService.fetchMatches(summoner.puuid(), region);
            matchesListView.getItems().setAll(matches.stream()
                .map(this::formatMatchForDisplay)
                .collect(Collectors.toList()));
        }, () -> {
            summonerNameLabel.setText("Summoner Not Found");
            summonerIconView.setImage(null);
            rankLabel.setText("");
            rankIconView.setImage(null);
            matchesListView.getItems().clear();
        });
    }

    private String formatMatchForDisplay(Match match) {
        String result = match.win() ? "Win" : "Loss";
        return String.format("%s - %s (%d/%d/%d)", result, match.championName(), match.kills(), match.deaths(), match.assists());
    }
}
