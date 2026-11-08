package com.cedarpos.desktop;

import com.cedarpos.core.CedarPos;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Entry point of the Cedar POS desktop application.
 * Step 2 placeholder: a plain window. The Ledger design comes in step 6.
 */
public class CedarPosApp extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label(CedarPos.PRODUCT_NAME + " " + CedarPos.VERSION);
        stage.setTitle(CedarPos.PRODUCT_NAME);
        stage.setScene(new Scene(new StackPane(label), 800, 500));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
