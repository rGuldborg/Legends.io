package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ThemeManager;
import org.example.service.lcu.ChampSelectSnapshot;
import org.example.service.lcu.LeagueClientChampSelectWatcher;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;


import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MainController {

    private static final DateTimeFormatter FOOTER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ACTIVE_TAB_CLASS = "tab-chip-active";

    @FXML private TextField searchField;
    @FXML private Button themeToggleButton;
    @FXML private Button refreshButton;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;
    @FXML private Label profileTab;
    @FXML private Label gameTab;
    @FXML private Label championsTab;
    @FXML private Label helpTab;
    @FXML private BorderPane windowHeader;
    @FXML private StackPane contentArea;
    @FXML private Node moonIcon;
    @FXML private Node sunIcon;
    @FXML private Label footerLastUpdatedLabel;
    @FXML private Label patchLabel;
    @FXML private Label lcuStatusLabel;
    @FXML private Circle lcuStatusIndicator;

    private final LeagueClientChampSelectWatcher clientWatcher = new LeagueClientChampSelectWatcher();
    private final SimpleObjectProperty<ChampSelectSnapshot> lcuSnapshot = new SimpleObjectProperty<>();

    private boolean darkMode = true; // start in DARK MODE
    private double xOffset;
    private double yOffset;
    private GameController gameController;
    private ChampionsController championsController;
    private Node profileView;
    private Node gameView;
    private Node championsView;
    private Node helpView;

    @FXML
    public void initialize() {
        System.out.println("MainController initialized");

        // Start med DARK mode
        ThemeManager.applyTheme("dark.css");

        // Ingen tekst - kun SVG ikoner i FXML
        themeToggleButton.setText("");
        if (refreshButton != null) refreshButton.setText("");
        flattenButtons(themeToggleButton, refreshButton,
                minimizeButton, maximizeButton, closeButton);
        updateThemeIcon();
        Platform.runLater(() -> {
            if (windowHeader != null) {
                windowHeader.requestFocus();
            }
        });

        // Load the primary PROFILE view by default
        showProfileView();
        setActiveTab(profileTab);
        ensureChampionsViewInitialized();
        updateSnapshotTimestamp();
        updatePatchVersion();
        startLcuWatcher();
    }

    public ReadOnlyObjectProperty<ChampSelectSnapshot> lcuSnapshotProperty() {
        return lcuSnapshot;
    }

    public void stop() {
        clientWatcher.stop();
    }

    private void startLcuWatcher() {
        lcuSnapshot.addListener((obs, oldV, newV) -> Platform.runLater(() -> updateLcuStatusIndicator(newV)));
        clientWatcher.addListener(lcuSnapshot::set);
        clientWatcher.start();
    }

    private void updateLcuStatusIndicator(ChampSelectSnapshot snapshot) {
        if (lcuStatusIndicator == null || lcuStatusLabel == null) {
            return;
        }
        lcuStatusIndicator.getStyleClass().removeAll(
                "lcu-status-indicator-connected",
                "lcu-status-indicator-disconnected",
                "lcu-status-indicator-idle",
                "lcu-status-indicator-unknown"
        );

        String styleClass;
        String statusText;
        if (snapshot == null) {
            styleClass = "lcu-status-indicator-unknown";
            statusText = "Unknown";
        } else if (snapshot.inChampSelect()) {
            styleClass = "lcu-status-indicator-connected";
            statusText = "Connected";
        } else if ("Looking for League client...".equals(snapshot.statusText())) {
            styleClass = "lcu-status-indicator-disconnected";
            statusText = "Disconnected";
        } else if ("Client idle (no champ select).".equals(snapshot.statusText())) {
            styleClass = "lcu-status-indicator-idle";
            statusText = "Client Idle (In-game not detected)";
        } else {
            styleClass = "lcu-status-indicator-unknown";
            statusText = "Unknown";
        }
        lcuStatusIndicator.getStyleClass().add(styleClass);
        lcuStatusLabel.setText(statusText);
    }

    @FXML
    private void onThemeToggle() {
        darkMode = !darkMode;

        themeToggleButton.setText(""); // hold teksten tom
        updateThemeIcon();

        if (darkMode) {
            ThemeManager.applyTheme("dark.css");
        } else {
            ThemeManager.applyTheme("light.css");
        }
    }

    @FXML
    private void onRefresh() {
        System.out.println("REFRESH CLICKED");
        updateSnapshotTimestamp();
    }

    @FXML
    private void onProfileNav() {
        setActiveTab(profileTab);
        showProfileView();
    }

    @FXML
    private void onGameNav() {
        setActiveTab(gameTab);
        showGameView();
    }

    @FXML
    private void onChampionsNav() {
        setActiveTab(championsTab);
        showChampionView();
    }

    @FXML
    private void onHelpNav() {
        setActiveTab(helpTab);
        showHelpView();
    }

    @FXML
    private void onProfileTabClicked(MouseEvent event) {
        onProfileNav();
    }

    @FXML
    private void onGameTabClicked(MouseEvent event) {
        onGameNav();
    }

    @FXML
    private void onChampionsTabClicked(MouseEvent event) {
        onChampionsNav();
    }

    @FXML
    private void onHelpTabClicked(MouseEvent event) {
        onHelpNav();
    }

    @FXML
    private void onProfileTabKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            onProfileNav();
            event.consume();
        }
    }

    @FXML
    private void onGameTabKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            onGameNav();
            event.consume();
        }
    }

    @FXML
    private void onChampionsTabKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            onChampionsNav();
            event.consume();
        }
    }

    @FXML
    private void onHelpTabKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            onHelpNav();
            event.consume();
        }
    }

    @FXML
    private void onMaximize() {
        var stage = getStage();
        if (stage != null) {
            stage.setMaximized(!stage.isMaximized());
        }
    }

    @FXML
    private void onMinimize() {
        var stage = getStage();
        if (stage != null) {
            stage.setIconified(true);
        }
    }

    @FXML
    private void onClose() {
        var stage = getStage();
        if (stage != null) {
            stage.close();
        }
    }

    @FXML
    private void onHeaderPressed(MouseEvent event) {
        var stage = getStage();
        if (stage != null) {
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        }
    }

    @FXML
    private void onHeaderDragged(MouseEvent event) {
        var stage = getStage();
        if (stage != null) {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        }
    }

    private void showProfileView() {
        Node view = getProfileView();
        if (view != null) {
            contentArea.getChildren().setAll(view);
        }
    }

    private Node getProfileView() {
        if (profileView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/fxml/profile-view.fxml"));
                profileView = loader.load();
            } catch (Exception e) {
                System.err.println("Could not load profile view.");
                e.printStackTrace();
            }
        }
        return profileView;
    }

    private void showGameView() {
        Node view = getGameView();
        if (view != null) {
            contentArea.getChildren().setAll(view);
        }
    }

    private Node getGameView() {
        if (gameView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/fxml/game-view.fxml"));
                gameView = loader.load();
                gameController = loader.getController();
                gameController.bindLcu(lcuSnapshot);
            } catch (Exception e) {
                System.err.println("Could not load game view.");
                e.printStackTrace();
            }
        }
        return gameView;
    }

    private void showChampionView() {
        ensureChampionsViewInitialized();
        if (championsView != null) {
            contentArea.getChildren().setAll(championsView);
        }
    }

    private void ensureChampionsViewInitialized() {
        if (championsView != null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/fxml/champions-view.fxml"));
            championsView = loader.load();
            championsController = loader.getController();
            if (searchField != null) {
                championsController.bindSearchField(searchField);
            }
            championsController.setShowViewRequest(this::showChampionViewFromSearch);
        } catch (Exception e) {
            System.err.println("Could not load champions view.");
            e.printStackTrace();
        }
    }

    private void showHelpView() {
        Node view = getHelpView();
        if (view != null) {
            contentArea.getChildren().setAll(view);
        }
    }

    private Node getHelpView() {
        if (helpView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/fxml/help-view.fxml"));
                helpView = loader.load();
            } catch (Exception e) {
                System.err.println("Could not load help view.");
                e.printStackTrace();
            }
        }
        return helpView;
    }

    private void showChampionViewFromSearch() {
        Platform.runLater(() -> {
            setActiveTab(championsTab);
            showChampionView();
        });
    }

    private Stage getStage() {
        return windowHeader != null && windowHeader.getScene() != null
                ? (Stage) windowHeader.getScene().getWindow()
                : null;
    }

    private void flattenButtons(Button... buttons) {
        for (Button b : buttons) {
            if (b == null) continue;
            b.setBackground(javafx.scene.layout.Background.EMPTY);
            b.setBorder(javafx.scene.layout.Border.EMPTY);
            b.setPadding(javafx.geometry.Insets.EMPTY);
            b.setFocusTraversable(false);
        }
    }

    private void updateThemeIcon() {
        if (moonIcon != null && sunIcon != null) {
            moonIcon.setVisible(darkMode);
            sunIcon.setVisible(!darkMode);
        }
    }

    private void updateSnapshotTimestamp() {
        if (footerLastUpdatedLabel == null) {
            return;
        }
        File snapshotFile = new File("data/snapshot.json");
        if (!snapshotFile.exists()) {
            footerLastUpdatedLabel.setText("Last updated: never");
            return;
        }
        Instant lastModified = Instant.ofEpochMilli(snapshotFile.lastModified());
        LocalDateTime localDateTime = LocalDateTime.ofInstant(lastModified, ZoneId.systemDefault());
        footerLastUpdatedLabel.setText("Last updated: " + FOOTER_TIME_FORMAT.format(localDateTime));
    }

    private void setActiveTab(Label activeTab) {
        Label[] tabs = {profileTab, gameTab, championsTab, helpTab};
        for (Label tab : tabs) {
            if (tab == null) continue;
            tab.getStyleClass().remove(ACTIVE_TAB_CLASS);
        }
        if (activeTab != null && !activeTab.getStyleClass().contains(ACTIVE_TAB_CLASS)) {
            activeTab.getStyleClass().add(ACTIVE_TAB_CLASS);
        }
    }

    private void updatePatchVersion() {
        if (patchLabel == null) {
            return;
        }
        try (InputStream is = getClass().getResourceAsStream("/org/example/data/champion-map.json")) {
            if (is == null) {
                patchLabel.setText("Patch: Unknown");
                return;
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(is);
            String version = rootNode.get("version").asText();
            patchLabel.setText("Patch: " + version);
        } catch (Exception e) {
            e.printStackTrace();
            patchLabel.setText("Patch: Error");
        }
    }
}
