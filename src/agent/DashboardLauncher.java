package agent;

import javafx.application.Application;
import javafx.stage.Stage;

public class DashboardLauncher extends Application {
    
    private static Agent agent;
    
    public static void launchGUI(Agent inputAgent) {
        agent = inputAgent;
        Application.launch(DashboardLauncher.class);
    }

    @Override
    public void start(Stage primaryStage) {
        DashboardGUI gui = new DashboardGUI(agent);
        agent.setOnBalanceUpdate(gui::updateBalanceLabels);
        agent.setOnMessage(gui::displayMessage);
        agent.setOnAuctionConnected(manager -> {
            gui.addAuctionHouse(manager);
            manager.setOnItemUpdate(() ->
                    gui.refreshAuctionTable(manager.getAuctionId()));
        });
        agent.setOnAuctionRemoved(gui::removeAuctionHouse);

        gui.show(primaryStage);
    }
}
