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

    @FXML
    private javafx.scene.control.TextField searchField;

    @FXML
    private VBox bitfestCard;

    @FXML
    private VBox calibrationCard;

    @FXML
    private VBox ignitionCard;

    @FXML
    public void initialize() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                String query = newValue.toLowerCase().trim();
                
                boolean showBitFest = query.isEmpty() || "bitfest".contains(query) || "kuet cse".contains(query);
                boolean showCalibration = query.isEmpty() || "calibration".contains(query) || "mechatronics".contains(query);
                boolean showIgnition = query.isEmpty() || "ignition".contains(query) || "mechanical".contains(query);

                bitfestCard.setVisible(showBitFest);
                bitfestCard.setManaged(showBitFest);

                calibrationCard.setVisible(showCalibration);
                calibrationCard.setManaged(showCalibration);

                ignitionCard.setVisible(showIgnition);
                ignitionCard.setManaged(showIgnition);
            });
        }
    }
}
