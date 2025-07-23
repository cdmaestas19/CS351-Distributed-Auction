package auctionhouse;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * GUI for visualizing the state of the Auction House.
 * Displays items for sale, connected agents, and supports controlled shutdown.
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction.
 *
 * @author Isaac Tapia
 * @author Christian Maestas
 */
public class AuctionHouseGUI {

    private final AuctionHouse house;
    private Label headerLabel;
    private TableView<AuctionItem> itemTable;
    private VBox agentListPane;

    /**
     * Constructs a GUI bound to the given AuctionHouse instance.
     *
     * @param house the auction house to display
     */
    public AuctionHouseGUI(AuctionHouse house) {
        this.house = house;
    }

    /**
     * Initializes and shows the window with layout and event bindings.
     *
     * @param primaryStage the main stage
     */
    public void show(Stage primaryStage) {
        primaryStage.setTitle("AuctionHouse");

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(createHeader());
        mainLayout.setCenter(createItemTable());
        mainLayout.setRight(createAgentPane());
        mainLayout.setBottom(createShutdownBox());

        Scene scene = new Scene(mainLayout, 900, 600);
        primaryStage.setScene(scene);

        // Prevent exit if active auctions exist
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            handleShutdown();
        });

        primaryStage.show();

        updateUI();
    }

    /**
     * Creates the top header displaying auction house ID and port.
     */
    private HBox createHeader() {
        headerLabel = new Label("AuctionHouse ID: <loading> | Port: " + house.getPort());
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox headerBox = new HBox(headerLabel);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(10));
        return headerBox;
    }

    /**
     * Constructs the item table view showing ID, description, bids, and status.
     */
    private TableView<AuctionItem> createItemTable() {
        itemTable = new TableView<>();
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AuctionItem, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("itemId"));

        TableColumn<AuctionItem, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<AuctionItem, Integer> minBidCol = new TableColumn<>("MinBid");
        minBidCol.setCellValueFactory(new PropertyValueFactory<>("minimumBid"));

        TableColumn<AuctionItem, Integer> bidCol = new TableColumn<>("Bid");
        bidCol.setCellValueFactory(new PropertyValueFactory<>("currentBid"));

        TableColumn<AuctionItem, String> statusCol = new TableColumn<>("Status");

        // Dynamically bind status label based on item state
        statusCol.setCellValueFactory(cellData -> {
            AuctionItem item = cellData.getValue();
            return Bindings.createStringBinding(() -> {
                if (item.isSold()) return "SOLD";
                if (item.isActive()) return "ACTIVE";
                return "WAITING";
            });
        });

        itemTable.getColumns().addAll(List.of(idCol, descCol, minBidCol, bidCol, statusCol));
        return itemTable;
    }

    /**
     * Builds the side pane listing connected agents.
     */
    private ScrollPane createAgentPane() {
        agentListPane = new VBox(5);
        agentListPane.setPadding(new Insets(10));
        agentListPane.setStyle("-fx-background-color: #f9f9f9;");

        Label agentTitle = new Label("Agents Connected");
        agentTitle.setStyle("-fx-font-weight: bold; -fx-underline: true;");
        agentListPane.getChildren().add(agentTitle);

        ScrollPane scrollPane = new ScrollPane(agentListPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(180);
        return scrollPane;
    }

    /**
     * Creates the shutdown button and attaches its event handler.
     */
    private HBox createShutdownBox() {
        Button shutdownButton = new Button("Shutdown");
        shutdownButton.setOnAction(e -> handleShutdown());

        HBox box = new HBox(shutdownButton);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        return box;
    }

    /**
     * Attempts to shut down the auction house if no active auctions exist.
     * Displays a warning if shutdown is blocked.
     */
    private void handleShutdown() {
        if (house.hasActiveAuctions()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Shutdown Blocked");
            alert.setHeaderText(null);
            alert.setContentText("Unable to shut down: active auctions in progress.");
            alert.showAndWait();
        } else {
            house.shutdown();
            Platform.exit();
        }
    }

    /**
     * Refreshes the entire GUI: header, item table, and agent list.
     */
    public void updateUI() {
        Platform.runLater(() -> {
            headerLabel.setText("ID: " + house.getAccountId() + " | Port: " + house.getPort());
            itemTable.getItems().setAll(house.getItemManager().getAllItems());

            agentListPane.getChildren().removeIf(n -> n instanceof Label &&
                    ((Label) n).getText().startsWith("Agent"));

            for (Integer id : house.getAgentIds()) {
                agentListPane.getChildren().add(new Label("Agent " + id));
            }
        });
    }
}
