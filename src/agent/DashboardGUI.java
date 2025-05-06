package agent;

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

public class DashboardGUI {
    
    private final Agent agent;
    private VBox root;
    private VBox displayBoxL, displayBoxC, displayBoxR;
    private Font titleFont = Font.font("Arial", FontWeight.BOLD,
            FontPosture.REGULAR, 36);
    private Font headingFont = Font.font("Arial", FontWeight.BOLD,
            FontPosture.REGULAR, 26);
    private Font displayFont = Font.font("Arial", FontWeight.SEMI_BOLD,
            FontPosture.REGULAR, 18);
    
    // TODO: Get rid of hard-coding:
    private int totalBalance = 2000;
    private int availableBalance = 1800;
    private int accountNumber = 4568;
    
    
    
    public DashboardGUI(Agent agent) {
        this.agent = agent;
    }
    
    public void show(Stage primaryStage) {
        
        primaryStage.setTitle("Agent Dashboard");
        
        root = new VBox(30);
        root.setBackground(new Background(new BackgroundFill(
                Color.LIGHTGRAY,
                null,
                Insets.EMPTY
        )));
        root.setPadding(new Insets(20));
        
        setupTitleBox();
        setupDisplayBoxes();
        setUpAccountBox();
        setupAuctionBox();
        setupMessageBox();
        
        Scene scene = new Scene(root, 1280, 720);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupTitleBox() {
        
        // Setup label:
        String titleString = ("Agent Name: " + agent.getAgentName() +
                "      (Agent ID: " + agent.getAgentID() + ")");
        Label titleLabel = new Label(titleString);
        titleLabel.setFont(titleFont);
        titleLabel.setAlignment(Pos.CENTER);
        
        // Put it in a box:
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.getChildren().add(titleLabel);
        
        // Put that box in another box:
        root.getChildren().add(titleBox);

    }

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
    
    private void setUpAccountBox() {
        
        // Set up heading:
        Label accountHeading = new Label("Account Info");
        accountHeading.setFont(headingFont);
        accountHeading.setUnderline(true);
        HBox headingBox = new HBox(10);
        headingBox.setAlignment(Pos.CENTER);
        headingBox.setPadding(new Insets(5));
        headingBox.getChildren().add(accountHeading);
        
        // Account number:
        Label accountNum = new Label("Account# " + accountNumber);
        accountNum.setFont(displayFont);
        accountNum.setUnderline(true);
        
        // Total balance:
        String tBalString = ("Total Balance:                 $" + totalBalance);
        Label totBal = new Label(tBalString);
        totBal.setFont(displayFont);
        
        // Available balance:
        String aBalString = ("Available Balance:          $" + availableBalance);
        Label availBal = new Label(aBalString);
        availBal.setFont(displayFont);
        
        // The box:
        VBox accountBox = new VBox(5);
        accountBox.setPadding(new Insets(10));
        accountBox.getChildren().addAll(headingBox, accountNum, totBal,
                availBal);
        BorderStroke borderStroke = new BorderStroke(
                Color.BLACK,
                BorderStrokeStyle.SOLID,
                new CornerRadii(15),
                new BorderWidths(2)
        );
        accountBox.setBorder(new Border(borderStroke));
        
        displayBoxL.getChildren().add(accountBox);
    
    }
    
    private void setupAuctionBox() {
        
        // Set up heading:
        Label auctionHeading = new Label("Auction House Items");
        auctionHeading.setFont(headingFont);
        auctionHeading.setUnderline(true);
        HBox headingBox = new HBox(10);
        headingBox.setAlignment(Pos.CENTER);
        headingBox.setPadding(new Insets(5));
        headingBox.getChildren().add(auctionHeading);
    
        // Set up Auction House dropdown menu:
        ComboBox<String> auctionSelector = new ComboBox<>();
        auctionSelector.setPromptText("Select Auction House...");
        auctionSelector.getItems().addAll("AuctionHouse 1", "AuctionHouse 2",
                "AuctionHouse 3");
        HBox selectorBox = new HBox(auctionSelector);
        selectorBox.setAlignment(Pos.CENTER);
        
        // TODO: handle auction house selection
        
        // Set up items list box:
        javafx.scene.control.ListView<String> listBox = new javafx.scene.control.ListView<>();
        listBox.getItems().addAll("Item 1", "Item 2", "Item 3"); // Example items
        listBox.setPrefSize(300, 300);
        listBox.setStyle("-fx-background-color: black;" +
                "-fx-control-inner-background: black; " +
                "-fx-text-fill: white;");
        
        // TODO: Handle item selection:
        listBox.setOnMouseClicked(e -> {
            String selectedItem = listBox.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                System.out.println("Selected: " + selectedItem);
            }
        });
        
        // Set up bid area:
        Label bidLabel = new Label("Bid Amount:");
        bidLabel.setFont(displayFont);
        TextField bidField = new TextField();
        bidField.setPrefWidth(100);
        bidField.setTextFormatter(new TextFormatter<>(change -> {
            return change.getControlNewText().matches("\\d*") ? change : null;
        }));
        
        // Bid button:
        Button bidButton = new Button("Bid!");
        bidButton.setFont(headingFont);
        bidButton.setOnAction(e -> {
            String bidText = bidField.getText();
            try {
                int bid = Integer.parseInt(bidText);
                // TODO: Do stuff with the bid
                System.out.println("Bid placed: $" + bid);
            } catch (NumberFormatException ex) {
                System.err.println("Invalid bid amount.");
            }
        });
        bidButton.setStyle("-fx-border-color: limegreen;" +
                        "-fx-border-width: 4;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;"
        );
        HBox bidBox = new HBox(20, bidLabel, bidField, bidButton);
        bidBox.setAlignment(Pos.CENTER);
        bidBox.setPadding(new Insets(10));
        
        
        BorderStroke borderStroke = new BorderStroke(
                Color.BLACK,
                BorderStrokeStyle.SOLID,
                new CornerRadii(15),
                new BorderWidths(2)
        );
        displayBoxC.setBorder(new Border (borderStroke));
        displayBoxC.setSpacing(30);
        displayBoxC.setPadding(new Insets(10));
        displayBoxC.getChildren().addAll(headingBox, selectorBox, listBox, bidBox);
        
    }
    
    private void setupMessageBox(){
        
        VBox messageBox = new VBox(10);
        messageBox.setPadding(new Insets(10));
        
        // Set up heading:
        Label messageHeading = new Label("Messages");
        messageHeading.setFont(headingFont);
        messageHeading.setUnderline(true);
        HBox headingBox = new HBox(10);
        headingBox.setAlignment(Pos.CENTER);
        headingBox.setPadding(new Insets(5));
        headingBox.getChildren().add(messageHeading);
        
        
        // Set up message box:
        VBox messageList = new VBox(5);
        messageList.setBackground(new Background(new BackgroundFill(
                Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY
        )));
        ScrollPane messageScrollPane = new ScrollPane(messageList);
        messageScrollPane.setPrefSize(300, 200);
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        messageScrollPane.setBackground(new Background(new BackgroundFill(
                Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY
        )));
        messageList.prefWidthProperty().bind(messageScrollPane.widthProperty());
        messageBox.getChildren().addAll(headingBox, messageScrollPane);
        VBox.setVgrow(messageList, Priority.ALWAYS);
        BorderStroke borderStroke = new BorderStroke(
                Color.BLACK,
                BorderStrokeStyle.SOLID,
                new CornerRadii(15),
                new BorderWidths(2)
        );
        messageBox.setBorder(new Border (borderStroke));
        
        displayBoxR.getChildren().add(messageBox);
        displayBoxR.setAlignment(Pos.BOTTOM_CENTER);
        
        
    }
    
}

