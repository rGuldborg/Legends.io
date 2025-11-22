package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChampionsController {

    @FXML private BorderPane rootPane;
    @FXML private TilePane championsGrid;
    @FXML private Label championNameLabel;
    @FXML private ImageView championImageView;
    @FXML private VBox roleDetailsContainer;

    private final List<String> displayedRoles = List.of("Top", "Jungle", "Mid", "ADC", "Support");
    private final Map<String, Map<String, Matchup>> matchupCache = new HashMap<>();
    private List<ChampionInfo> championInfos = new ArrayList<>();

    @FXML
    public void initialize() {
        loadChampionInfos();
        populateGrid();
        if (!championInfos.isEmpty()) {
            showChampionDetails(championInfos.get(0));
        } else {
            championNameLabel.setText("No champions found");
        }
    }

    @FXML
    private void onBackClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/fxml/game-view.fxml")
            );
            Node gameView = loader.load();

            StackPane stackPane = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (stackPane != null) {
                stackPane.getChildren().setAll(gameView);
            }
        } catch (IOException e) {
            System.err.println("Unable to navigate back to game view");
            e.printStackTrace();
        }
    }

    private void loadChampionInfos() {
        URL directoryUrl = getClass().getResource("/org/example/images/champSquare");
        if (directoryUrl == null) {
            System.err.println("champSquare directory not found on classpath.");
            return;
        }

        try {
            Path dirPath = Paths.get(directoryUrl.toURI());
            try (Stream<Path> stream = Files.list(dirPath)) {
                championInfos = stream
                        .filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                        .map(path -> new ChampionInfo(
                                beautifyName(path.getFileName().toString()),
                                "/org/example/images/champSquare/" + path.getFileName()
                        ))
                        .collect(Collectors.toCollection(ArrayList::new));
            }
        } catch (URISyntaxException | IOException e) {
            System.err.println("Failed to read champion assets:");
            e.printStackTrace();
        }
    }

    private void populateGrid() {
        championsGrid.getChildren().clear();
        for (ChampionInfo champion : championInfos) {
            VBox card = createChampionCard(champion);
            championsGrid.getChildren().add(card);
        }
    }

    private VBox createChampionCard(ChampionInfo champion) {
        ImageView avatar = new ImageView();
        avatar.setFitWidth(72);
        avatar.setFitHeight(72);
        avatar.setPreserveRatio(true);
        avatar.setSmooth(true);
        avatar.setImage(loadImage(champion.imagePath()));

        Label name = new Label(champion.name());
        name.getStyleClass().add("champion-card-name");

        VBox box = new VBox(6, avatar, name);
        box.getStyleClass().add("champion-card");
        box.setOnMouseClicked(event -> showChampionDetails(champion));
        return box;
    }

    private void showChampionDetails(ChampionInfo champion) {
        championNameLabel.setText(champion.name());
        Image championImage = loadImage(champion.imagePath());
        if (championImage != null) {
            championImageView.setImage(championImage);
        }

        roleDetailsContainer.getChildren().clear();
        Map<String, Matchup> matchups = matchupCache.computeIfAbsent(champion.name(), ignored -> synthesizeMatchups(champion));
        for (int i = 0; i < displayedRoles.size(); i++) {
            String role = displayedRoles.get(i);
            Matchup matchup = matchups.get(role);
            VBox content = new VBox(4);
            Label good = new Label("Good vs: " + String.join(", ", matchup.strongAgainst()));
            Label bad = new Label("Struggles vs: " + String.join(", ", matchup.weakAgainst()));
            content.getChildren().addAll(good, bad);
            TitledPane pane = new TitledPane(role, content);
            pane.setExpanded(i == 0);
            roleDetailsContainer.getChildren().add(pane);
        }
    }

    private Map<String, Matchup> synthesizeMatchups(ChampionInfo champion) {
        Map<String, Matchup> result = new LinkedHashMap<>();
        for (String role : displayedRoles) {
            List<String> strengths = pickOpponents(ROLE_STRENGTH_POOLS.get(role), champion.name() + role);
            List<String> weaknesses = pickOpponents(ROLE_THREAT_POOLS.get(role), role + champion.name());
            result.put(role, new Matchup(strengths, weaknesses));
        }
        return result;
    }

    private List<String> pickOpponents(List<String> pool, String seed) {
        if (pool == null || pool.isEmpty()) {
            return List.of("Coming soon");
        }

        int hash = Math.abs(seed.hashCode());
        List<String> working = new ArrayList<>(pool);
        List<String> picks = new ArrayList<>();
        for (int i = 0; i < Math.min(3, working.size()); i++) {
            int index = (hash + i * 7) % working.size();
            picks.add(working.remove(index));
        }
        return picks;
    }

    private Image loadImage(String resourcePath) {
        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(resourcePath)));
        } catch (Exception ex) {
            System.err.println("Unable to load image: " + resourcePath);
            return null;
        }
    }

    private String beautifyName(String rawFileName) {
        String withoutExt = rawFileName;
        int squareIndex = withoutExt.indexOf("Square");
        if (squareIndex >= 0) {
            withoutExt = withoutExt.substring(0, squareIndex);
        }

        withoutExt = withoutExt
                .replace("_Unreleased", "")
                .replace("_", " ");

        String decoded = URLDecoder.decode(withoutExt, StandardCharsets.UTF_8);
        decoded = decoded.replaceAll("\\s+", " ").trim();
        if (decoded.isEmpty()) {
            decoded = rawFileName;
        }
        return decoded;
    }

    private record ChampionInfo(String name, String imagePath) { }

    private record Matchup(List<String> strongAgainst, List<String> weakAgainst) { }

    private static final Map<String, List<String>> ROLE_STRENGTH_POOLS = Map.of(
            "Top", List.of("Ornn", "Sion", "Shen", "Gnar", "Sett", "Dr. Mundo", "Volibear", "Renekton"),
            "Jungle", List.of("Sejuani", "Amumu", "Vi", "Rammus", "Jarvan IV", "Ivern", "Nunu & Willump", "Nocturne"),
            "Mid", List.of("Galio", "Orianna", "Anivia", "Azir", "Twisted Fate", "Ahri", "Malzahar", "Annie"),
            "ADC", List.of("Caitlyn", "Ashe", "Varus", "Miss Fortune", "Jhin", "Twitch", "Ezreal", "Tristana"),
            "Support", List.of("Leona", "Thresh", "Braum", "Taric", "Nautilus", "Rell", "Senna", "Alistar")
    );

    private static final Map<String, List<String>> ROLE_THREAT_POOLS = Map.of(
            "Top", List.of("Fiora", "Camille", "Irelia", "Aatrox", "Jax", "Yone", "Riven", "Gwen"),
            "Jungle", List.of("Kindred", "Graves", "Lillia", "Bel'Veth", "Viego", "Kayn", "Evelynn", "Elise"),
            "Mid", List.of("Fizz", "Kassadin", "LeBlanc", "Akali", "Yasuo", "Syndra", "Vex", "Katarina"),
            "ADC", List.of("Draven", "Lucian", "Samira", "Aphelios", "Kai'Sa", "Xayah", "Vayne", "Zeri"),
            "Support", List.of("Morgana", "Zyra", "Renata Glasc", "Seraphine", "Lulu", "Janna", "Yuumi", "Soraka")
    );
}
