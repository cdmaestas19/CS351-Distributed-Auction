package agent;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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
    private Font headingFont = Font.font("Arial", FontWeight.SEMI_BOLD,
            FontPosture.REGULAR, 20);
    private Font displayFont = Font.font("Arial", FontWeight.NORMAL,
            FontPosture.REGULAR, 16);
    
    
    
    public DashboardGUI(Agent agent) {
        this.agent = agent;
    }
    
    public void show(Stage primaryStage) {
        
        primaryStage.setTitle("Agent Dashboard");
        
        root = new VBox(10);
        root.setBackground(new Background(new BackgroundFill(
                Color.LIGHTGRAY,
                null,
                Insets.EMPTY
        )));
        root.setPadding(new Insets(10));
        
        setupTitleBox();
        setupDisplayBoxes();
        
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
        displayBoxL.setBackground(new Background(new BackgroundFill(
                Color.BLUE,
                null,
                Insets.EMPTY
        )));
        displayBoxC = new VBox(10);
        displayBoxC.setBackground(new Background(new BackgroundFill(
                Color.GREEN,
                null,
                Insets.EMPTY
        )));
        displayBoxR = new VBox(10);
        displayBoxR.setBackground(new Background(new BackgroundFill(
                Color.YELLOW,
                null,
                Insets.EMPTY
        )));
        
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
        
        Label accountHeading = new Label("Account Info");
        accountHeading.setFont(headingFont);
        
        VBox accountBox = new VBox(20);
        accountBox.setPadding(new Insets(10));
        
        
    
    }

}

