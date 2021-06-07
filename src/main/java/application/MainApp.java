package application;

import application.appconfig.ApplicationContext;
import controller.MainController;
import ga.GA;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import writer.ResultWriter;

public class MainApp extends Application {

    public static Stage primaryStage;
    public static Scene primaryScene;

    @Override
    public void start(Stage primaryStage) throws Exception {
        MainApp.primaryStage = primaryStage;
        String resultsFile = ApplicationContext.getProperties().getProperty("data.resultsfile");
        ResultWriter resultWriter = new ResultWriter(resultsFile);
        GA ga = new GA(resultWriter);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/scene.fxml"));
        Parent root = loader.load();
        MainController mainCtrl = loader.getController();
        mainCtrl.init(ga, primaryStage);
        primaryScene = new Scene(root);
        primaryStage.setTitle("JavaFX and Gradle");
        primaryStage.setScene(primaryScene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
