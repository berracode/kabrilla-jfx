package co.com.bancolombia.kabrilla.controller.sidebar;

import co.com.bancolombia.kabrilla.Namespace;
import co.com.bancolombia.kabrilla.services.NamespaceService;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SideBarController {
    @FXML
    private TreeView<String> namespaceTreeView;

    private final NamespaceService namespaceService = NamespaceService.getInstance();
    private final TreeItem<String> rootItem = new TreeItem<>("Namespaces");

    public void initialize() {
        // 🔹 Mostrar el root inmediatamente con el mensaje "Cargando..."
        TreeItem<String> loadingItem = new TreeItem<>("Cargando...");
        rootItem.getChildren().add(loadingItem);
        rootItem.setExpanded(true);
        namespaceTreeView.setShowRoot(false);
        namespaceTreeView.setRoot(rootItem);


        // Escuchar cambios en la lista observable
        namespaceService.getNamespaces().addListener((ListChangeListener<Namespace>) change -> {
            System.out.println("==========En el escuchador.... algo cambió 1");

            while (change.next()) {
                System.out.println("==========En el escuchador.... algo cambió 2");

                if (change.wasAdded()) {
                    System.out.println("==========En el escuchador.... algo cambió 3");
                    Platform.runLater(() -> rootItem.getChildren().remove(loadingItem)); // ❗ Eliminar "Cargando..." antes de agregar

                    for (Namespace namespace : change.getAddedSubList()) {
                        System.out.println("==========Se agrego un nuevo desde el home: " + namespace.getName());

                        CompletableFuture.supplyAsync(() -> {
                                            System.out.println("Se consultan pods en otro hilo: " + namespace.getName());
                                            return namespaceService.getPods(namespace.getName());
                                        }
                                )
                                .thenAccept(pods -> Platform.runLater(() -> {
                                    namespace.setPods(pods);
                                    TreeItem<String> namespaceItem = new TreeItem<>(namespace.getName());

                                    for (String pod : pods) {
                                        namespaceItem.getChildren().add(new TreeItem<>(pod));
                                    }

                                    rootItem.getChildren().add(namespaceItem);
                                }));
                    }
                }
            }
        });


        loadNamespacesAsync4();
    }

    private void loadNamespacesAsync4() {
        System.out.println("Cargando namespaces desde el archivo...");

        CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return namespaceService.getNamespaces();
        })
                .thenAccept(namespaces -> Platform.runLater(() -> {
                    rootItem.getChildren().clear(); // quitar el treeItem cargando

                    if (namespaces.isEmpty()) {
                        rootItem.getChildren().add(new TreeItem<>("No hay namespaces"));
                        return;
                    }

                    for (Namespace namespace : namespaces) {
                        TreeItem<String> namespaceItem = new TreeItem<>(namespace.getName());
                        rootItem.getChildren().add(namespaceItem);

                        // 🔹 Iniciar la carga de pods en otro hilo (sin bloquear)
                        CompletableFuture.supplyAsync(() -> namespaceService.getPods(namespace.getName()))
                                .thenAccept(pods -> Platform.runLater(() -> {
                                    namespace.setPods(pods);
                                    for (String pod : pods) {
                                        namespaceItem.getChildren().add(new TreeItem<>(pod));
                                    }
                                }));
                    }
                }));
    }


    private void loadNamespacesAsync3() {
        System.out.println("Cargando namespaces desde el archivo...");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        CompletableFuture.supplyAsync(namespaceService::getNamespaces)
                .thenAccept(namespaces -> Platform.runLater(() -> {
                    rootItem.getChildren().clear();

                    for (Namespace namespace : namespaces) {
                        TreeItem<String> namespaceItem = new TreeItem<>(namespace.getName());
                        rootItem.getChildren().add(namespaceItem);

                        // 🔹 Cargar pods en otro hilo sin bloquear la UI
                        CompletableFuture.supplyAsync(() -> namespaceService.getPods(namespace.getName()))
                                .thenAccept(pods -> Platform.runLater(() -> {
                                    namespace.setPods(pods);
                                    for (String pod : pods) {
                                        namespaceItem.getChildren().add(new TreeItem<>(pod));
                                    }
                                }));
                    }
                }));
    }


    private void loadNamespacesAsync2() {
        System.out.println("Cargando desde sidebar...");

        CompletableFuture.supplyAsync(this::getNamespaces).thenAccept(namespaces ->
                Platform.runLater(() -> populateTreeView(namespaces))
        );
    }

    private List<Namespace> getNamespaces() {
        System.out.println("Obteniendo desde memoria----");
        List<Namespace> namespaces = namespaceService.getNamespaces();
        System.out.println("ya con los namespaces: "+namespaces.size()+" hash: "+namespaces.hashCode());


        for (Namespace ns : namespaces) {
            ns.setPods(namespaceService.getPods(ns.getName()));
        }
        return namespaces;
    }

    private void loadNamespacesAsync() {
        System.out.println("cargando desde sidebar, debe leer archivo a ver que hay");

        CompletableFuture.supplyAsync(this::getNamespaces).thenAccept(namespaces ->
                Platform.runLater(() -> populateTreeView(namespaces))
        );
    }

    private void populateTreeView(List<Namespace> namespaces) {
        System.out.println("poblando root treeitem");
        //Platform.runLater(() -> rootItem.getChildren().clear()); // 🔥 Limpiar todo antes de poblar

        System.out.println("eliminando elementos del root "+rootItem.getChildren().size());
        System.out.println("eliminando elementos del root "+rootItem.getChildren().get(0).getValue());
        rootItem.getChildren().clear();

        for (Namespace namespace : namespaces) {
            TreeItem<String> namespaceItem = new TreeItem<>(namespace.getName());

            for (String pod : namespace.getPods()) {
                namespaceItem.getChildren().add(new TreeItem<>(pod));
            }

            rootItem.getChildren().add(namespaceItem);
        }
        System.out.println("fin de la poblada ............");

    }
}
