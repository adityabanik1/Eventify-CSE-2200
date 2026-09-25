package com.eventify;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HomeController {

    @FXML
    private VBox root;

    @FXML
    private Label statusLabel;

    @FXML
    public void onSelectBitFest() {
        App.selectMainEventAndLogin("BitFest", root, statusLabel);
    }

    @FXML
    public void onSelectCalibration() {
        App.selectMainEventAndLogin("Calibration", root, statusLabel);
    }

    @FXML
    public void onSelectIgnition() {
        App.selectMainEventAndLogin("Ignition", root, statusLabel);
    }
}
