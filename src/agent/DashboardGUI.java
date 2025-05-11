package agent;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent Dashboard GUI
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction
 *
 * @author Dustin Ferguson
 * @author Christian Maestas
 * @author Isaac Tapia
 */
public class DashboardGUI {
    
    private VBox root;
    private VBox displayBoxL, displayBoxC, displayBoxR;
    private VBox messageList;
    private final Map<String, AuctionManager> auctionMap = new HashMap<>();
    private ComboBox<String> auctionSelector;
    private TableView<ItemInfo> itemTable;
    private ItemInfo selectedItem;
    
    private final Font titleFont = Font.font(
            "Arial", FontWeight.BOLD, FontPosture.REGULAR, 36);
    private final Font headingFont = Font.font(
            "Arial", FontWeight.BOLD, FontPosture.REGULAR, 26);
    private final Font displayFont = Font.font(
            "Arial", FontWeight.SEMI_BOLD, FontPosture.REGULAR, 18);
    
    private final Agent agent;
    private Label totalBalanceLabel;
    private Label availableBalanceLabel;
    
    
    /**
     * Agent dashboard constructor
     * @param agent the agent that the dashboard
     */
    public DashboardGUI(Agent agent) {
        this.agent = agent;
    }
    
    /**
     * Sets stage for JavaFX GUI
     * @param primaryStage stage for JavaFX GUI
     */
    public void show(Stage primaryStage) {
        primaryStage.setTitle("Agent Dashboard");

        root = new VBox(30);
        root.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, null, Insets.EMPTY)));
        root.setPadding(new Insets(20));

        setupTitleBox();
        setupDisplayBoxes();
        setupAccountBox();
        setupAuctionBox();
        setupMessageBox();

        Scene scene = new Scene(root, 1280, 720);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * Sets up top of display, where title is
     */
    private void setupTitleBox() {
        String titleString = ("Agent Name: " + agent.getAgentName());
        Label titleLabel = new Label(titleString);
        titleLabel.setFont(titleFont);
        titleLabel.setAlignment(Pos.CENTER);

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.getChildren().add(titleLabel);
        root.getChildren().add(titleBox);
    }
    
    /**
     * Sets up 3 main display areas
     */
    private void setupDisplayBoxes() {
        displayBoxL = new VBox(10);
        displayBoxC = new VBox(10);
        displayBoxR = new VBox(10);

        HBox displayBox = new HBox(10);
        VBox.setVgrow(displayBox, Priority.ALWAYS);

        displayBoxL.prefWidthProperty().bind(displayBox.widthProperty().divide(3));
        displayBoxC.prefWidthProperty().bind(displayBox.widthProperty().divide(3));
        displayBoxR.prefWidthProperty().bind(displayBox.widthProperty().divide(3));

        HBox.setHgrow(displayBoxL, Priority.ALWAYS);
        HBox.setHgrow(displayBoxC, Priority.ALWAYS);
        HBox.setHgrow(displayBoxR, Priority.ALWAYS);

        displayBox.getChildren().addAll(displayBoxL, displayBoxC, displayBoxR);
        root.getChildren().add(displayBox);
    }
    
    /**
     * Sets up account info area
     */
    private void setupAccountBox() {
        Label accountHeading = new Label("Account Info");
        accountHeading.setFont(headingFont);
        accountHeading.setUnderline(true);

        HBox headingBox = new HBox(accountHeading);
        headingBox.setAlignment(Pos.CENTER);
        headingBox.setPadding(new Insets(5));

        Label accountNum = new Label("Account# " + agent.getAgentID());
        accountNum.setFont(displayFont);
        accountNum.setUnderline(true);

        totalBalanceLabel = new Label();
        totalBalanceLabel.setFont(displayFont);

        availableBalanceLabel = new Label();
        availableBalanceLabel.setFont(displayFont);

        updateBalanceLabels();

        VBox accountBox = new VBox(5);
        accountBox.setPadding(new Insets(10));
        accountBox.getChildren().addAll(headingBox, accountNum, totalBalanceLabel, availableBalanceLabel);
        accountBox.setBorder(new Border(new BorderStroke(
                Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(15), new BorderWidths(2)
        )));

        displayBoxL.getChildren().add(accountBox);
    }
    
    /**
     * Sets up the auctioning area in GUI
     */
    private void setupAuctionBox() {
        // Heading
        Label auctionHeading = new Label("Auction House Items");
        auctionHeading.setFont(headingFont);
        auctionHeading.setUnderline(true);
        HBox headingBox = new HBox(auctionHeading);
        headingBox.setAlignment(Pos.CENTER);
        headingBox.setPadding(new Insets(5));

        // Dropdown for Auction House selection
        auctionSelector = new ComboBox<>();
        auctionSelector.setPromptText("Select Auction House...");
        auctionSelector.setOnAction(e -> {
            String selected = auctionSelector.getValue();
            if (selected != null && auctionMap.containsKey(selected)) {
                updateItemTable(auctionMap.get(selected));
            }
        });
        HBox selectorBox = new HBox(auctionSelector);
        selectorBox.setAlignment(Pos.CENTER);

        // TableView for item display
        itemTable = new TableView<>();
        itemTable.setPrefHeight(300);
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ItemInfo, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().itemId));

        TableColumn<ItemInfo, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().description));

        TableColumn<ItemInfo, String> minBidCol = new TableColumn<>("Min Bid");
        minBidCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cell.getValue().minBid)));

        TableColumn<ItemInfo, String> currBidCol = new TableColumn<>("Current Bid");
        currBidCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cell.getValue().currBid)));

        itemTable.getColumns().addAll(idCol, descCol, minBidCol, currBidCol);

        itemTable.getSelectionModel().selectedItemProperty().addListener((
                obs, oldItem, newItem) -> {
            selectedItem = newItem;
        });

        // Bid area
        Label bidLabel = new Label("Bid Amount:");
        bidLabel.setFont(displayFont);
        TextField bidField = new TextField();
        bidField.setPrefWidth(100);
        bidField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null
        ));

        Button bidButton = new Button("Bid!");
        bidButton.setFont(headingFont);
        bidButton.setStyle("-fx-border-color: limegreen;" +
                "-fx-border-width: 4;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;");
        bidButton.setOnAction(e -> {
            try {
                int bid = Integer.parseInt(bidField.getText());
                if (selectedItem == null) {
                    System.out.println("No item selected.");
                    return;
                }

                String auctionId = auctionSelector.getValue();
                if (auctionId == null) {
                    System.out.println("No auction house selected.");
                    return;
                }

                AuctionManager manager = auctionMap.get(auctionId);
                if (manager == null) {
                    System.out.println("Invalid auction house.");
                    return;
                }

                manager.getClient().placeBid(Integer.parseInt(selectedItem.itemId), bid);
                bidField.clear();

            } catch (NumberFormatException ex) {
                System.err.println("Invalid bid amount.");
            } catch (IOException ex) {
                System.err.println("Failed to send bid: " + ex.getMessage());
            }
        });

        HBox bidBox = new HBox(20, bidLabel, bidField, bidButton);
        bidBox.setAlignment(Pos.CENTER);
        bidBox.setPadding(new Insets(10));

        // Put it all together
        displayBoxC.setBorder(new Border(new BorderStroke(
                Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(15), new BorderWidths(2)
        )));
        displayBoxC.setSpacing(30);
        displayBoxC.setPadding(new Insets(10));
        displayBoxC.getChildren().addAll(headingBox, selectorBox, itemTable, bidBox);
    }
    
    /**
     * Sets up the message display on agent GUI.
     */
    private void setupMessageBox() {
        Label messageHeading = new Label("Messages");
        messageHeading.setFont(headingFont);
        messageHeading.setUnderline(true);

        HBox headingBox = new HBox(messageHeading);
        headingBox.setAlignment(Pos.CENTER);
        headingBox.setPadding(new Insets(5));

        messageList = new VBox(5);

        ScrollPane messageScrollPane = new ScrollPane(messageList);
        messageScrollPane.setPrefSize(300, 200);
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        messageList.prefWidthProperty().bind(messageScrollPane.widthProperty());

        VBox messageBox = new VBox(10, headingBox, messageScrollPane);
        messageBox.setPadding(new Insets(10));

        displayBoxR.getChildren().add(messageBox);
        displayBoxR.setAlignment(Pos.BOTTOM_CENTER);

        Button shutdownButton = new Button("Exit Agent");
        shutdownButton.setFont(displayFont);
        shutdownButton.setStyle("-fx-border-color: red;" +
                "-fx-border-width: 4;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-background-color: lightcoral;");
        shutdownButton.setOnAction(e -> agent.shutdown());

        VBox buttonBox = new VBox(shutdownButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        displayBoxR.getChildren().add(buttonBox);
    }
    
    /**
     * Shows a message in the message pane of the GUI
     * @param text The message to be displayed on the GUI
     */
    public void displayMessage(String text) {
        Platform.runLater(() -> {
            Label msg = new Label(text);
            msg.setFont(displayFont);
            messageList.getChildren().add(msg);
        });
    }
    
    /**
     * Refreshes the "Total Balance" and "Available Balance" labels in account
     * info area
     */
    public void updateBalanceLabels() {
        int total = agent.getTotalBalance();
        int available = agent.getAvailableBalance();
        totalBalanceLabel.setText("Total Balance: $" + total);
        availableBalanceLabel.setText("Available Balance: $" + available);
    }
    
    /**
     * Adds an auction house to the GUI
     * @param manager The manager that belongs to the auction house to be shown.
     */
    public void addAuctionHouse(AuctionManager manager) {
        String id = manager.getAuctionId();
        auctionMap.put(id, manager);
        auctionSelector.getItems().add(id);
        auctionSelector.getSelectionModel().selectFirst();
    }
    
    /**
     * Removes an auction house that is no longer active.
     * @param auctionId The ID number of the auction house to be removed from the GUI
     */
    public void removeAuctionHouse(String auctionId) {
        Platform.runLater(() -> {
            auctionMap.remove(auctionId);
            auctionSelector.getItems().remove(auctionId);

            if (auctionId.equals(auctionSelector.getValue())) {
                itemTable.getItems().clear();
                auctionSelector.getSelectionModel().clearSelection();
            }

            displayMessage("Auction house " + auctionId + " removed from system.");
        });
    }
    
    /**
     * Updates the list of items that are available for auction for an auction house
     * @param manager The auction house manager that the items belong to.
     */
    private void updateItemTable(AuctionManager manager) {
        itemTable.getItems().setAll(manager.getItems());
    }
    
    /**
     * Forces the item table to re-render if the supplied auction house is
     * currently selected.
     * @param auctionId
     */
    public void refreshAuctionTable(String auctionId) {
        AuctionManager manager = auctionMap.get(auctionId);
        if (manager != null && auctionSelector.getValue() != null &&
                auctionSelector.getValue().equals(auctionId)) {
            updateItemTable(manager);
        }
    }
}
