package co.com.bancolombia.kabrilla.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private SplitPane debugSplitPane;


    @FXML
    public void initialize() {
        mainSplitPane.setDividerPosition(0, 250.0 / mainSplitPane.getWidth());

        mainSplitPane.widthProperty().addListener((obs, oldW, newW) -> {
            mainSplitPane.setDividerPosition(0, 250.0 / newW.doubleValue());
        });

        debugSplitPane.setDividerPosition(0, 1.0 - (250.0 / debugSplitPane.getHeight()));
        debugSplitPane.heightProperty().addListener((obs, oldH, newH) -> {
            debugSplitPane.setDividerPosition(0, 1.0 - (250.0 / newH.doubleValue()));
        });
    }
}
