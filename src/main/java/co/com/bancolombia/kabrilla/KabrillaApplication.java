package co.com.bancolombia.kabrilla;

import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

import io.kubernetes.client.util.KubeConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class KabrillaApplication extends Application {

    // Define las dimensiones mínimas como constantes
    private static final double MIN_WIDTH = 1280.0;
    private static final double MIN_HEIGHT = 720.0;

    public static void main(String[] args) throws IOException {
        String kubeConfigPath = System.getProperty("user.home") + "/.kube/config";

        FileReader reader = new FileReader(kubeConfigPath);
        KubeConfig config = KubeConfig.loadKubeConfig(reader);


        System.out.println("Contexto actual: " + config.getCurrentContext());
        System.out.println("Clusters disponibles: " + config.getClusters().size());

        launch();
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 720);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);

        primaryStage.setWidth(MIN_WIDTH);
        primaryStage.setHeight(MIN_HEIGHT);

        // Agregar CSS
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());

        primaryStage.setTitle("Kabrilla kubernetes Dashboard");
        primaryStage.setScene(scene);
        primaryStage.getIcons().addAll(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/kabrilla-icon-16.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/kabrilla-icon-32.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/kabrilla-icon-64.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/kabrilla-icon-128.png")))
        );

        primaryStage.show();
    }

    //@Override
    public void start2(Stage stage) throws IOException {

        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.25); // Sidebar ocupa 25%

        TreeItem<String> rootItem = new TreeItem<>("Home");
        rootItem.setExpanded(true);

        TreeItem<String> clusterDev = new TreeItem<>("cluster-name-dev");
        TreeItem<String> nsDev = new TreeItem<>("namespace-xyz-dev");
        TreeItem<String> nsDev2 = new TreeItem<>("namespace-xyz-2-dev");
        nsDev2.getChildren().addAll(
                new TreeItem<>("my-app-1-deployment"),
                new TreeItem<>("my-app-2-deployment")
        );
        nsDev.getChildren().add(nsDev2);
        clusterDev.getChildren().add(nsDev);

        TreeItem<String> clusterQA = new TreeItem<>("cluster-name-qa");

        rootItem.getChildren().addAll(clusterDev, clusterQA);

        TreeView<String> treeView = new TreeView<>(rootItem);
        VBox sidebar = new VBox(new Label("Create cluster Other"), treeView);
        sidebar.setPrefWidth(200);

        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("my-app-1-deploy");
        Tab tab2 = new Tab("my-app-2-deploy");

// Contenido dentro de cada Tab
        HBox innerTabs = new HBox();
        innerTabs.getChildren().addAll(
                new Button("pods"),
                new Button("Logs"),
                new Button("Config map"),
                new Button("RequestPolicy"),
                new Button("Service")
        );

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll("Combined logs", "pod-1", "pod-2", "pod-3", "pod-4");
        comboBox.getSelectionModel().selectFirst();

        TextArea logsArea = new TextArea("pod-1: [INFO] Mi log super largo...");
        logsArea.setWrapText(true);

        VBox tabContent = new VBox(innerTabs, comboBox, logsArea);
        tab1.setContent(tabContent);
        tab2.setContent(new Label("Contenido para my-app-2-deploy"));

        tabPane.getTabs().addAll(tab1, tab2);

        splitPane.getItems().addAll(sidebar, tabPane);
        Scene scene = new Scene(splitPane, 1000, 600);
        stage.setScene(scene);
        stage.show();

       /* FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("home-view.fxml"));
        AnchorPane homeLayout = homeLoader.load();

        FXMLLoader sidebarLoader = new FXMLLoader(getClass().getResource("sidebar/sidebar.fxml"));
        VBox sidebarLayout = sidebarLoader.load();

        HBox rootLayout = new HBox();
        rootLayout.getChildren().addAll(sidebarLayout, homeLayout);

        Scene scene = new Scene(rootLayout, 1080, 720);
        stage.setTitle("Kabrilla - Kubernetes Manager");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();*/



/* pantalla grande - no fullscreen (modo cine)
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
*/
        // stage.show();
    }

    private TreeItem<String> addChild(String child, TreeItem<String> parent) {
        var treItem = new TreeItem<String>(child);
        treItem.setExpanded(true);
        parent.getChildren().add(treItem);
        return treItem;
    }
}