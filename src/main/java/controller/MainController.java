package controller;

import application.appconfig.ApplicationContext;
import ga.GA;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;

public class MainController {

    private Stage stage;
    private GA ga;

    @FXML
    private TextField classPathTextField;
    @FXML
    private TextField minNrTextField;
    @FXML
    private TextField maxNrTextField;
    @FXML
    private TextField strLengthTextField;
    @FXML
    private TextField solLengthTextField;

    @FXML
    private RadioButton small1;
    @FXML
    private RadioButton big2;
    @FXML
    private RadioButton numbers3;
    @FXML
    private RadioButton others4;

    @FXML
    private CheckBox firstCheckBox;
    @FXML
    private Label stateLabel;
    @FXML
    private Label resultLabel;

    @FXML
    public void initialize() {

    }

    public void init(GA ga, Stage stage) {
        this.ga = ga;
        this.stage = stage;
    }

    @FXML
    private void stateGenerating() {
        resultLabel.setVisible(false);
        stateLabel.setTextFill(Paint.valueOf("Orange"));
        stateLabel.setText("State: Generating...");
    }

    @FXML
    private void stateFailed(Exception exception) {
        resultLabel.setVisible(false);
        stateLabel.setTextFill(Paint.valueOf("Red"));
        stateLabel.setText("State: An error has occured! " + exception.getMessage());
        System.out.println(Arrays.toString(exception.getStackTrace()));
    }

    @FXML
    private void stateDone() {
        stateLabel.setTextFill(Paint.valueOf("Green"));
        stateLabel.setText("State: Done!");
        double result = this.ga.getBestFitness() * 100;
        DecimalFormat df = new DecimalFormat("#.##");
        if (result == 100.0)
            resultLabel.setTextFill(Paint.valueOf("Green"));
        if (result < 100.0)
            resultLabel.setTextFill(Paint.valueOf("Yellow"));
        if (result <= 75.0)
            resultLabel.setTextFill(Paint.valueOf("Red"));
        resultLabel.setText("Result: The solution has a coverage of " + Double.valueOf(df.format(result)) + "%");
        resultLabel.setVisible(true);
    }

    @FXML
    public void handleFileChoice() {
        FileChooser fileChooser = new FileChooser();
        File selected = fileChooser.showOpenDialog(stage);
        classPathTextField.setText(selected.getAbsolutePath());
    }

    @FXML
    public void handleGenerate() {
        int minNr, maxNr, strLength, solLength;

        try {
            minNr = Integer.parseInt(minNrTextField.getText());
            maxNr = Integer.parseInt(maxNrTextField.getText());
            if (minNr <= maxNr) {
                this.ga.setMinNr(minNr);
                this.ga.setMaxNr(maxNr);
            }
        } catch (NumberFormatException ignored) {
            this.ga.setMinNr(Integer.parseInt(ApplicationContext.getProperties().getProperty("data.default.minNumericalValue")));
            this.ga.setMaxNr(Integer.parseInt(ApplicationContext.getProperties().getProperty("data.default.maxNumericalValue")));
        }

        try {
            strLength = Integer.parseInt(strLengthTextField.getText());
            if (strLength > 0)
                this.ga.setMaxStringLength(strLength);
        } catch (NumberFormatException ignored) {
            this.ga.setMaxStringLength(Integer.parseInt(ApplicationContext.getProperties().getProperty("data.default.maxStringLength")));
        }

        try {
            solLength = Integer.parseInt(solLengthTextField.getText());
            if (solLength > 1)
                this.ga.setMaxSuiteLength(solLength);
        } catch (NumberFormatException e) {
            this.ga.setMaxSuiteLength(Integer.parseInt(ApplicationContext.getProperties().getProperty("data.default.maxSuiteLength")));
        }

        ToggleGroup toggleGroup = small1.getToggleGroup();
        Toggle selected = toggleGroup.getSelectedToggle();
        if (selected == small1)
            this.ga.setStringType(1);
        if (selected == big2)
            this.ga.setStringType(2);
        if (selected == numbers3)
            this.ga.setStringType(3);
        if (selected == others4)
            this.ga.setStringType(4);

        this.ga.setOnlyFirst(firstCheckBox.isSelected());

        Thread thread = new Thread(() -> {
            Platform.runLater(this::stateGenerating);
            try {
                ga.start(classPathTextField.getText());
                Platform.runLater(this::stateDone);
            } catch (Exception exception) {
                Platform.runLater(() -> stateFailed(exception));
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
