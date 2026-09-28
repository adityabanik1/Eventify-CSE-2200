package com.eventify;

import javafx.scene.control.Alert;

/**
 * Abstract class demonstrating an advanced OOP concept.
 * Provides common utility methods and defines a contract for controllers.
 */
public abstract class BaseController {

    /**
     * Abstract method that concrete controllers must implement.
     * Can be used for custom initialization logic separate from JavaFX's initialize().
     */
    public abstract void setupController();

    /**
     * Common utility method available to all derived controllers.
     */
    protected void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
