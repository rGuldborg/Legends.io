package org.example.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.example.ThemeManager;
import org.example.model.ChampionSummary;
import org.example.model.RecommendationContext;
import org.example.model.Role;
import org.example.model.SlotSelection;
import org.example.service.MockStatsService;
import org.example.service.RiotStatsService;
import org.example.service.StatsService;
import org.example.service.lcu.ChampSelectSnapshot;
import org.example.service.lcu.LeagueClientChampSelectWatcher;
import org.example.util.ChampionIconResolver;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class GameController {
    private static final int SLOT_COUNT = 5;
    private static final int RECOMMENDATION_LIMIT = 100;
    private static final Role[] ROLE_ORDER = {Role.TOP, Role.JUNGLE, Role.MID, Role.BOTTOM, Role.SUPPORT};

    @FXML private ImageView allyBan1, allyBan2, allyBan3, allyBan4, allyBan5;
    @FXML private ImageView enemyBan1, enemyBan2, enemyBan3, enemyBan4, enemyBan5;
    @FXML private ImageView allyPick1, allyPick2, allyPick3, allyPick4, allyPick5;
    @FXML private ImageView enemyPick1, enemyPick2, enemyPick3, enemyPick4, enemyPick5;
    @FXML private HBox allyRoleRow1, allyRoleRow2, allyRoleRow3, allyRoleRow4, allyRoleRow5;
    @FXML private HBox enemyRoleRow1, enemyRoleRow2, enemyRoleRow3, enemyRoleRow4, enemyRoleRow5;
    @FXML private TableView<ChampionSummary> recommendedTable;
    @FXML private TableColumn<ChampionSummary, String> championCol;
    @FXML private TableColumn<ChampionSummary, String> opCol;
    @FXML private TableColumn<ChampionSummary, String> synCol;
    @FXML private TableColumn<ChampionSummary, String> coCol;
    @FXML private TableColumn<ChampionSummary, Number> scoreCol;
    @FXML private Label selectionStatusLabel;
    @FXML private RadioButton allyFirstPickRadio;
    @FXML private RadioButton enemyFirstPickRadio;
    @FXML private ToggleGroup firstPickGroup;
    @FXML private Button finishBansButton;
    @FXML private CheckBox mirrorClientToggle;

    private final List<String> allyBans = createSlotList();
    private final List<String> enemyBans = createSlotList();
    private final List<String> allyPicks = createSlotList();
    private final List<String> enemyPicks = createSlotList();
    private final List<Role> allyPickRoles = new ArrayList<>(List.of(Role.TOP, Role.JUNGLE, Role.MID, Role.BOTTOM, Role.SUPPORT));
    private final List<Role> enemyPickRoles = new ArrayList<>(List.of(Role.TOP, Role.JUNGLE, Role.MID, Role.BOTTOM, Role.SUPPORT));
    private final List<Slot> slots = new ArrayList<>();
    private final DropShadow activeEffect = new DropShadow(18, Color.web("#73c0ff"));
    private final Map<ThemeManager.Theme, Map<Role, Image>> roleIconCache = new EnumMap<>(ThemeManager.Theme.class);
    private DraftPhase draftPhase = DraftPhase.SELECT_FIRST_PICK;
    private Side firstPickSide;
    private List<Slot> banOrder = List.of();
    private List<Slot> pickOrder = List.of();
    private int banIndex;
    private int pickIndex;
    private boolean liveMirrorRequested;

    private StatsService statsService;
    private ObservableList<ChampionSummary> tableData;
    private Slot activeSlot;
    private final Consumer<ThemeManager.Theme> themeListener = theme -> Platform.runLater(this::refreshRoleIcons);
    private final LeagueClientChampSelectWatcher clientWatcher = new LeagueClientChampSelectWatcher();
    private final Consumer<ChampSelectSnapshot> clientSnapshotConsumer =
            snapshot -> Platform.runLater(() -> applyClientSnapshot(snapshot));

    private enum SlotType {
        ALLY_BAN(true), ENEMY_BAN(false), ALLY_PICK(true), ENEMY_PICK(false);

        private final boolean allySide;

        SlotType(boolean allySide) {
            this.allySide = allySide;
        }

        boolean isAlly() {
            return allySide;
        }
    }

    private enum Side {
        ALLY, ENEMY
    }

    private enum DraftPhase {
        SELECT_FIRST_PICK,
        BAN_PHASE,
        PICK_PHASE,
        COMPLETE,
        LIVE_MIRROR
    }

    private static class RoleChip {
        final Role role;
        final StackPane container;
        final ImageView iconView;

        RoleChip(Role role, StackPane container, ImageView iconView) {
            this.role = role;
            this.container = container;
            this.iconView = iconView;
        }
    }

    private static class Slot {
        final SlotType type;
        final int index;
        final ImageView pickView;
        final HBox roleRow;
        final List<RoleChip> roleChips = new ArrayList<>();

        Slot(SlotType type, int index, ImageView pickView, HBox roleRow) {
            this.type = type;
            this.index = index;
            this.pickView = pickView;
            this.roleRow = roleRow;
        }
    }

    @FXML
    public void initialize() {
        System.out.println("[GameController] Loaded game-view!");
        ThemeManager.addThemeChangeListener(themeListener);
        statsService = initStatsService();
        configureTable();
        configureSlots();
        configureFirstPickControls();
        resetBoardToInitial();
        refreshRecommendations();
        recommendedTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                assignSelectedChampion();
            }
        });
    }

    private void configureTable() {
        tableData = FXCollections.observableArrayList();
        recommendedTable.setItems(tableData);
        recommendedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        championCol.setCellValueFactory(data -> Bindings.createStringBinding(data.getValue()::name));
        opCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> data.getValue().opTier().label()));
        synCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> data.getValue().synTier().label()));
        coCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> data.getValue().coTier().label()));
        scoreCol.setCellValueFactory(data -> Bindings.createDoubleBinding(data.getValue()::score));

        championCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView iconView = new ImageView();
            private final Text nameText = new Text();
            private final HBox box = new HBox(8, iconView, nameText);
            {
                iconView.setFitWidth(28);
                iconView.setFitHeight(28);
                iconView.setPreserveRatio(true);
                HBox.setHgrow(nameText, Priority.ALWAYS);
                nameText.getStyleClass().add("recommendation-name");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                ChampionSummary row = getTableView().getItems().get(getIndex());
                iconView.setImage(row.icon());
                nameText.setText(row.name());
                setGraphic(box);
                setText(null);
            }
        });

        scoreCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(String.format("%.1f", item.doubleValue()));
            }
        });

        scoreCol.setSortType(TableColumn.SortType.DESCENDING);
    }

    private void configureSlots() {
        slots.add(new Slot(SlotType.ALLY_BAN, 0, allyBan1, null));
        slots.add(new Slot(SlotType.ALLY_BAN, 1, allyBan2, null));
        slots.add(new Slot(SlotType.ALLY_BAN, 2, allyBan3, null));
        slots.add(new Slot(SlotType.ALLY_BAN, 3, allyBan4, null));
        slots.add(new Slot(SlotType.ALLY_BAN, 4, allyBan5, null));

        slots.add(new Slot(SlotType.ENEMY_BAN, 0, enemyBan1, null));
        slots.add(new Slot(SlotType.ENEMY_BAN, 1, enemyBan2, null));
        slots.add(new Slot(SlotType.ENEMY_BAN, 2, enemyBan3, null));
        slots.add(new Slot(SlotType.ENEMY_BAN, 3, enemyBan4, null));
        slots.add(new Slot(SlotType.ENEMY_BAN, 4, enemyBan5, null));

        slots.add(new Slot(SlotType.ALLY_PICK, 0, allyPick1, allyRoleRow1));
        slots.add(new Slot(SlotType.ALLY_PICK, 1, allyPick2, allyRoleRow2));
        slots.add(new Slot(SlotType.ALLY_PICK, 2, allyPick3, allyRoleRow3));
        slots.add(new Slot(SlotType.ALLY_PICK, 3, allyPick4, allyRoleRow4));
        slots.add(new Slot(SlotType.ALLY_PICK, 4, allyPick5, allyRoleRow5));

        slots.add(new Slot(SlotType.ENEMY_PICK, 0, enemyPick1, enemyRoleRow1));
        slots.add(new Slot(SlotType.ENEMY_PICK, 1, enemyPick2, enemyRoleRow2));
        slots.add(new Slot(SlotType.ENEMY_PICK, 2, enemyPick3, enemyRoleRow3));
        slots.add(new Slot(SlotType.ENEMY_PICK, 3, enemyPick4, enemyRoleRow4));
        slots.add(new Slot(SlotType.ENEMY_PICK, 4, enemyPick5, enemyRoleRow5));

        slots.forEach(slot -> {
            if (slot.pickView != null) {
                slot.pickView.setCursor(Cursor.HAND);
                slot.pickView.setOnMouseClicked(event -> handleSlotClick(slot, event));
            }
            if (slot.roleRow != null) {
                buildRoleChips(slot);
            }
        });
    }

    private void configureFirstPickControls() {
        if (firstPickGroup != null) {
            firstPickGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                if (draftPhase == DraftPhase.LIVE_MIRROR) {
                    if (newToggle != null) {
                        firstPickGroup.selectToggle(oldToggle);
                    }
                    updateStatus("Disable live mirroring to control the draft manually.");
                    return;
                }
                Side selectedSide = null;
                if (newToggle == allyFirstPickRadio) {
                    selectedSide = Side.ALLY;
                } else if (newToggle == enemyFirstPickRadio) {
                    selectedSide = Side.ENEMY;
                }
                handleFirstPickSelection(selectedSide);
            });
        }
        if (finishBansButton != null) {
            finishBansButton.setDisable(true);
        }
        if (mirrorClientToggle != null) {
            mirrorClientToggle.selectedProperty().addListener((obs, oldValue, selected) -> toggleLiveMirror(selected));
        }
    }

    private void handleFirstPickSelection(Side side) {
        if (liveMirrorRequested) {
            updateStatus("Live mirroring is active. Disable it to control the draft.");
            if (firstPickGroup != null) {
                firstPickGroup.selectToggle(null);
            }
            return;
        }
        if (side == null) {
            resetBoardToInitial();
            return;
        }
        firstPickSide = side;
        restartDraftForSide(side);
    }

    private void resetBoardToInitial() {
        clearAllSlotsAndEffects();
        firstPickSide = null;
        banOrder = List.of();
        pickOrder = List.of();
        banIndex = 0;
        pickIndex = 0;
        draftPhase = liveMirrorRequested ? DraftPhase.LIVE_MIRROR : DraftPhase.SELECT_FIRST_PICK;
        if (finishBansButton != null) {
            finishBansButton.setDisable(true);
        }
        if (firstPickGroup != null) {
            firstPickGroup.selectToggle(null);
        }
        if (!liveMirrorRequested) {
            updateStatus("Choose who has first pick to start drafting.");
        }
        refreshRecommendations();
    }

    private void restartDraftForSide(Side side) {
        clearAllSlotsAndEffects();
        banOrder = buildBanOrder(side);
        pickOrder = buildPickOrder(side);
        banIndex = 0;
        pickIndex = 0;
        draftPhase = DraftPhase.BAN_PHASE;
        if (finishBansButton != null) {
            finishBansButton.setDisable(true);
        }
        if (!banOrder.isEmpty()) {
            Slot slot = banOrder.get(0);
            setActiveSlot(slot);
            updateStatus("Ban Phase: " + describeSlot(slot) + " is up.");
        }
        refreshRecommendations();
    }

    private void clearAllSlotsAndEffects() {
        clearList(allyBans);
        clearList(enemyBans);
        clearList(allyPicks);
        clearList(enemyPicks);
        slots.forEach(slot -> updateIcon(slot.pickView, null));
        slots.forEach(this::updateRoleHighlight);
        slots.forEach(slot -> {
            if (slot.pickView != null) {
                slot.pickView.setEffect(null);
            }
        });
        activeSlot = null;
    }

    private void toggleLiveMirror(boolean enable) {
        if (enable == liveMirrorRequested) {
            return;
        }
        liveMirrorRequested = enable;
        if (enable) {
            draftPhase = DraftPhase.LIVE_MIRROR;
            clearAllSlotsAndEffects();
             if (firstPickGroup != null) {
                 firstPickGroup.selectToggle(null);
             }
             if (finishBansButton != null) {
                 finishBansButton.setDisable(true);
             }
            clientWatcher.addListener(clientSnapshotConsumer);
            clientWatcher.start();
            updateStatus("Mirroring the live League client. Waiting for champ select...");
            refreshRecommendations();
        } else {
            clientWatcher.removeListener(clientSnapshotConsumer);
            clientWatcher.stop();
            draftPhase = DraftPhase.SELECT_FIRST_PICK;
            resetBoardToInitial();
        }
    }

    private void applyClientSnapshot(ChampSelectSnapshot snapshot) {
        if (!liveMirrorRequested) {
            return;
        }
        if (!snapshot.inChampSelect()) {
            draftPhase = DraftPhase.LIVE_MIRROR;
            clearAllSlotsAndEffects();
            updateStatus(snapshot.statusText());
            refreshRecommendations();
            return;
        }
        draftPhase = DraftPhase.LIVE_MIRROR;
        if (finishBansButton != null) {
            finishBansButton.setDisable(true);
        }
        copySnapshotList(snapshot.allyBans(), allyBans);
        copySnapshotList(snapshot.enemyBans(), enemyBans);
        copySnapshotList(snapshot.allyPicks(), allyPicks);
        copySnapshotList(snapshot.enemyPicks(), enemyPicks);
        slots.forEach(slot -> updateIcon(slot.pickView, valueForSlot(slot)));
        slots.forEach(this::updateRoleHighlight);
        setActiveSlot(null);
        updateStatus(snapshot.statusText());
        refreshRecommendations();
    }

    private void copySnapshotList(List<String> source, List<String> target) {
        for (int i = 0; i < target.size(); i++) {
            String value = (source != null && i < source.size()) ? normalizeChampionName(source.get(i)) : null;
            target.set(i, value);
        }
    }

    private String normalizeChampionName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    private List<Slot> buildBanOrder(Side side) {
        List<Slot> order = new ArrayList<>();
        SlotType first = side == Side.ALLY ? SlotType.ALLY_BAN : SlotType.ENEMY_BAN;
        SlotType second = side == Side.ALLY ? SlotType.ENEMY_BAN : SlotType.ALLY_BAN;
        for (int i = 0; i < SLOT_COUNT; i++) {
            order.add(findSlot(first, i));
            order.add(findSlot(second, i));
        }
        return order;
    }

    private List<Slot> buildPickOrder(Side side) {
        List<Slot> order = new ArrayList<>();
        if (side == Side.ALLY) {
            order.add(findSlot(SlotType.ALLY_PICK, 0));
            order.add(findSlot(SlotType.ENEMY_PICK, 0));
            order.add(findSlot(SlotType.ENEMY_PICK, 1));
            order.add(findSlot(SlotType.ALLY_PICK, 1));
            order.add(findSlot(SlotType.ALLY_PICK, 2));
            order.add(findSlot(SlotType.ENEMY_PICK, 2));
            order.add(findSlot(SlotType.ENEMY_PICK, 3));
            order.add(findSlot(SlotType.ALLY_PICK, 3));
            order.add(findSlot(SlotType.ALLY_PICK, 4));
            order.add(findSlot(SlotType.ENEMY_PICK, 4));
        } else {
            order.add(findSlot(SlotType.ENEMY_PICK, 0));
            order.add(findSlot(SlotType.ALLY_PICK, 0));
            order.add(findSlot(SlotType.ALLY_PICK, 1));
            order.add(findSlot(SlotType.ENEMY_PICK, 1));
            order.add(findSlot(SlotType.ENEMY_PICK, 2));
            order.add(findSlot(SlotType.ALLY_PICK, 2));
            order.add(findSlot(SlotType.ALLY_PICK, 3));
            order.add(findSlot(SlotType.ENEMY_PICK, 3));
            order.add(findSlot(SlotType.ENEMY_PICK, 4));
            order.add(findSlot(SlotType.ALLY_PICK, 4));
        }
        return order;
    }

    private Slot findSlot(SlotType type, int index) {
        return slots.stream()
                .filter(slot -> slot.type == type && slot.index == index)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing slot " + type + " #" + index));
    }

    private void buildRoleChips(Slot slot) {
        slot.roleRow.getChildren().clear();
        boolean allySide = slot.type.isAlly();
        for (Role role : ROLE_ORDER) {
            StackPane chip = new StackPane();
            chip.setPrefSize(28, 28);
            chip.setMinSize(28, 28);
            chip.setMaxSize(28, 28);
            chip.getStyleClass().addAll("role-chip", allySide ? "ally" : "enemy");
            ImageView icon = new ImageView(roleIcon(role));
            icon.setFitWidth(18);
            icon.setFitHeight(18);
            icon.setPreserveRatio(true);
            chip.getChildren().add(icon);
            chip.setOnMouseClicked(e -> {
                selectRole(slot, role);
            });
            HBox.setMargin(chip, new Insets(0, 3, 0, 0));
            slot.roleRow.getChildren().add(chip);
            slot.roleChips.add(new RoleChip(role, chip, icon));
            Tooltip.install(chip, new Tooltip(role.label()));
        }
        updateRoleHighlight(slot);
    }

    private void handleSlotClick(Slot slot, MouseEvent event) {
        if (draftPhase == DraftPhase.LIVE_MIRROR) {
            updateStatus("Viewing live client. Disable mirroring to edit manually.");
            return;
        }
        if (draftPhase == DraftPhase.SELECT_FIRST_PICK) {
            updateStatus("Choose who has first pick before interacting with slots.");
            return;
        }
        if (activeSlot == null || slot != activeSlot) {
            if (activeSlot == null) {
                updateStatus("No slot is currently active. Follow the draft prompts above.");
            } else {
                updateStatus(describeSlot(activeSlot) + " is currently active.");
            }
            return;
        }
        if (event.getButton() == MouseButton.SECONDARY || event.getClickCount() == 2) {
            clearSlot(slot);
        }
    }

    private void selectRole(Slot slot, Role role) {
        List<Role> roles = rolesForSlot(slot);
        if (roles == null) return;
        roles.set(slot.index, role);
        updateRoleHighlight(slot);
        updateStatus("Set " + describeSlot(slot) + " role to " + role.label() + ".");
        refreshRecommendations();
    }

    private void assignSelectedChampion() {
        ChampionSummary selection = recommendedTable.getSelectionModel().getSelectedItem();
        if (selection == null) {
            updateStatus("Double-click a champion row to assign it.");
            return;
        }
        if (draftPhase == DraftPhase.LIVE_MIRROR) {
            updateStatus("Live mirror is active. Disable it to edit selections.");
            return;
        }
        if (activeSlot == null) {
            updateStatus("Select a slot first.");
            return;
        }
        placeChampion(activeSlot, selection.name());
    }

    private void placeChampion(Slot slot, String champion) {
        switch (slot.type) {
            case ALLY_BAN -> allyBans.set(slot.index, champion);
            case ENEMY_BAN -> enemyBans.set(slot.index, champion);
            case ALLY_PICK -> allyPicks.set(slot.index, champion);
            case ENEMY_PICK -> enemyPicks.set(slot.index, champion);
        }
        updateIcon(slot.pickView, champion);
        updateStatus("Placed " + champion + " into " + describeSlot(slot) + ".");
        refreshRecommendations();
        handleSlotCommit(slot);
    }

    private void clearSlot(Slot slot) {
        clearSlot(slot, false);
    }

    private void clearSlot(Slot slot, boolean force) {
        if (slot == null) return;
        if (!force && !canModifySlot(slot)) {
            updateStatus("Cannot modify " + describeSlot(slot) + " right now.");
            return;
        }
        switch (slot.type) {
            case ALLY_BAN -> allyBans.set(slot.index, null);
            case ENEMY_BAN -> enemyBans.set(slot.index, null);
            case ALLY_PICK -> allyPicks.set(slot.index, null);
            case ENEMY_PICK -> enemyPicks.set(slot.index, null);
        }
        updateIcon(slot.pickView, null);
        if (!force) {
            updateStatus("Cleared " + describeSlot(slot) + ".");
            refreshRecommendations();
        }
    }

    private boolean canModifySlot(Slot slot) {
        if (slot == null) return false;
        if (draftPhase == DraftPhase.LIVE_MIRROR) return false;
        return slot == activeSlot;
    }

    private void handleSlotCommit(Slot slot) {
        if (draftPhase == DraftPhase.BAN_PHASE && isBanSlot(slot)) {
            banIndex++;
            if (banIndex < banOrder.size()) {
                Slot next = banOrder.get(banIndex);
                setActiveSlot(next);
                updateStatus("Ban Phase: " + describeSlot(next) + " is up.");
            } else {
                setActiveSlot(null);
                if (finishBansButton != null) {
                    finishBansButton.setDisable(false);
                }
                updateStatus("All bans locked. Click Finish Bans to move on.");
            }
        } else if (draftPhase == DraftPhase.PICK_PHASE && isPickSlot(slot)) {
            pickIndex++;
            if (pickIndex < pickOrder.size()) {
                Slot next = pickOrder.get(pickIndex);
                setActiveSlot(next);
                updateStatus("Pick Phase: " + describeSlot(next) + " is up.");
            } else {
                setActiveSlot(null);
                draftPhase = DraftPhase.COMPLETE;
                updateStatus("Draft complete.");
            }
        }
    }

    private boolean isBanSlot(Slot slot) {
        return slot.type == SlotType.ALLY_BAN || slot.type == SlotType.ENEMY_BAN;
    }

    private boolean isPickSlot(Slot slot) {
        return slot.type == SlotType.ALLY_PICK || slot.type == SlotType.ENEMY_PICK;
    }

    private void setActiveSlot(Slot slot) {
        activeSlot = slot;
        slots.forEach(s -> {
            if (s.pickView != null) {
                s.pickView.setEffect(s == activeSlot ? activeEffect : null);
            }
        });
    }

    private String describeSlot(Slot slot) {
        String prefix = switch (slot.type) {
            case ALLY_BAN -> "Ally Ban";
            case ENEMY_BAN -> "Enemy Ban";
            case ALLY_PICK -> "Ally Pick";
            case ENEMY_PICK -> "Enemy Pick";
        };
        if (slot.type == SlotType.ALLY_PICK || slot.type == SlotType.ENEMY_PICK) {
            Role role = rolesForSlot(slot).get(slot.index);
            return prefix + " #" + (slot.index + 1) + " (" + role.label() + ")";
        }
        return prefix + " #" + (slot.index + 1);
    }

    private void refreshRecommendations() {
        List<SlotSelection> allySelections = buildSelections(allyPicks, allyPickRoles);
        List<SlotSelection> enemySelections = buildSelections(enemyPicks, enemyPickRoles);
        List<String> bans = mergeLists(allyBans, enemyBans);
        Role targetRole = activeSlot != null && (activeSlot.type == SlotType.ALLY_PICK || activeSlot.type == SlotType.ENEMY_PICK)
                ? rolesForSlot(activeSlot).get(activeSlot.index)
                : Role.UNKNOWN;

        RecommendationContext context = new RecommendationContext(
                allySelections,
                enemySelections,
                bans,
                targetRole,
                RECOMMENDATION_LIMIT
        );

        List<ChampionSummary> summaries = statsService.fetchRecommended(context);
        tableData.setAll(summaries);
        recommendedTable.getSortOrder().setAll(scoreCol);
        recommendedTable.sort();
    }

    @FXML
    private void onClearSelections() {
        if (draftPhase == DraftPhase.LIVE_MIRROR) {
            updateStatus("Disable live mirroring before clearing selections.");
            return;
        }
        resetBoardToInitial();
    }

    @FXML
    private void onFinishBans() {
        if (draftPhase != DraftPhase.BAN_PHASE) {
            updateStatus("Ban phase is already complete.");
            return;
        }
        if (banIndex < banOrder.size()) {
            updateStatus("Complete all ban slots before finishing.");
            return;
        }
        draftPhase = DraftPhase.PICK_PHASE;
        pickIndex = 0;
        if (!pickOrder.isEmpty()) {
            Slot slot = pickOrder.get(0);
            setActiveSlot(slot);
            updateStatus("Pick Phase: " + describeSlot(slot) + " is up.");
        }
        if (finishBansButton != null) {
            finishBansButton.setDisable(true);
        }
    }

    private List<SlotSelection> buildSelections(List<String> champions, List<Role> roles) {
        List<SlotSelection> selections = new ArrayList<>();
        for (int i = 0; i < champions.size(); i++) {
            String champion = champions.get(i);
            if (champion == null || champion.isBlank()) continue;
            Role role = roles.get(i);
            selections.add(new SlotSelection(champion, role));
        }
        return selections;
    }

    private List<String> mergeLists(List<String> first, List<String> second) {
        Set<String> merged = new LinkedHashSet<>();
        if (first != null) {
            first.stream().filter(name -> name != null && !name.isBlank()).forEach(merged::add);
        }
        if (second != null) {
            second.stream().filter(name -> name != null && !name.isBlank()).forEach(merged::add);
        }
        return merged.isEmpty() ? List.of() : List.copyOf(merged);
    }

    private void updateStatus(String message) {
        if (selectionStatusLabel != null) {
            selectionStatusLabel.setText(message);
        }
    }

    private StatsService initStatsService() {
        String apiKey = System.getenv("RIOT_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            String platformTag = System.getenv().getOrDefault("RIOT_PLATFORM", "EUROPE_WEST");
            return new RiotStatsService(apiKey, platformTag);
        }
        System.out.println("[GameController] Missing RIOT_API_KEY, using mock data.");
        return new MockStatsService();
    }

    private String valueForSlot(Slot slot) {
        return switch (slot.type) {
            case ALLY_BAN -> allyBans.get(slot.index);
            case ENEMY_BAN -> enemyBans.get(slot.index);
            case ALLY_PICK -> allyPicks.get(slot.index);
            case ENEMY_PICK -> enemyPicks.get(slot.index);
        };
    }

    private void updateRoleHighlight(Slot slot) {
        if (slot.roleRow == null || slot.roleChips.isEmpty()) return;
        List<Role> roles = rolesForSlot(slot);
        if (roles == null) return;
        Role selected = roles.get(slot.index);
        for (RoleChip chip : slot.roleChips) {
            chip.container.getStyleClass().remove("role-chip-active");
            if (chip.role == selected) {
                chip.container.getStyleClass().add("role-chip-active");
            }
        }
    }

    private void refreshRoleIcons() {
        roleIconCache.clear();
        for (Slot slot : slots) {
            for (RoleChip chip : slot.roleChips) {
                if (chip.iconView != null) {
                    chip.iconView.setImage(roleIcon(chip.role));
                }
            }
        }
    }

    private Image roleIcon(Role role) {
        ThemeManager.Theme theme = ThemeManager.currentTheme();
        Map<Role, Image> cache = roleIconCache.computeIfAbsent(theme, t -> new EnumMap<>(Role.class));
        return cache.computeIfAbsent(role, r -> {
            String folder = theme == ThemeManager.Theme.DARK ? "dark" : "light";
            String path = "/org/example/images/roles/" + folder + "/" + r.iconFile();
            try {
                return new Image(getClass().getResourceAsStream(path));
            } catch (Exception ex) {
                return ChampionIconResolver.placeholder();
            }
        });
    }

    private void updateIcon(ImageView view, String champion) {
        if (view == null) return;
        view.setImage(champion == null ? ChampionIconResolver.placeholder() : ChampionIconResolver.load(champion));
    }

    private List<Role> rolesForSlot(Slot slot) {
        return switch (slot.type) {
            case ALLY_PICK -> allyPickRoles;
            case ENEMY_PICK -> enemyPickRoles;
            default -> null;
        };
    }

    private static List<String> createSlotList() {
        return new ArrayList<>(java.util.Collections.nCopies(SLOT_COUNT, null));
    }

    private void clearList(List<String> list) {
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            list.set(i, null);
        }
    }
}
