package com.eventify;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class AuthController {

    @FXML
    private VBox root;

    @FXML
    private TextField loginEmail;

    @FXML
    private PasswordField loginPassword;

    @FXML
    private TextField registerName;

    @FXML
    private TextField registerEmail;

    @FXML
    private PasswordField registerPassword;

    @FXML
    private PasswordField confirmPassword;

    @FXML
    private Label statusLabel;

    @FXML
    private void onLogin() {
        String email = loginEmail.getText();
        String password = loginPassword.getText();

        App.run(
                root,
                statusLabel,
                () -> Database.login(email, password),
                App::showMain
        );
    }

    @FXML
    private void onRegister() {
        String name = registerName.getText();
        String email = registerEmail.getText();
        String password = registerPassword.getText();
        String confirmation = confirmPassword.getText();

        if (!password.equals(confirmation)) {
            Forms.info("The passwords do not match.");
            return;
        }

        App.run(
                root,
                statusLabel,
                () -> {
                    Database.createParticipant(name, email, password);
                    return null;
                },
                ignored -> {
                    registerName.clear();
                    registerEmail.clear();
                    registerPassword.clear();
                    confirmPassword.clear();

                    loginEmail.setText(email);
                    loginPassword.clear();

                    statusLabel.setText(
                            "Account created. Open Sign in to continue."
                    );

                    Forms.info(
                            "Participant account created successfully.\n"
                                    + "Open the Sign in tab and log in."
                    );
                }
        );
    }
}
