package co.com.bancolombia.kabrilla.ui.viewmodel.cluster;

import static co.com.bancolombia.kabrilla.ui.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.domain.gateway.ClusterRepository;
import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import co.com.bancolombia.kabrilla.ui.FxTestSupport;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.AppState;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterStateViewModel;

import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * El árbol del sidebar refleja renombrar/quitar namespaces y pide cerrar sus pestañas.
 */
@Tag("ui")
class SidebarNamespacesTest {

    private final List<Cluster> saved = new ArrayList<>();
    private final ClusterNamespaceStateViewModel state = new ClusterNamespaceStateViewModel();
    private final List<SidebarStateViewModel.NamespaceRef> closeRequests = new ArrayList<>();
    private SidebarStateViewModel sidebar;
    private TreeItem<String> clusterItem;

    @BeforeAll
    static void startFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    private void setUp() {
        Cluster qa = new Cluster("qa");
        qa.getNamespaces().add(new Namespace("ventas-qa"));
        qa.getNamespaces().add(new Namespace("pagso-qa"));
        saved.add(qa);

        ClusterRepository repository = new ClusterRepository() {
            @Override
            public List<Cluster> findAll() {
                return new ArrayList<>(saved);
            }

            @Override
            public void saveAll(List<Cluster> clusters) {
                saved.clear();
                saved.addAll(clusters);
            }
        };
        sidebar = new SidebarStateViewModel(new AppState(), state, new ClusterService(null, repository), null);
        sidebar.closeNamespaceTabsRequestProperty().addListener((obs, old, ref) -> {
            if (ref != null) {
                closeRequests.add(ref);
                sidebar.clearCloseNamespaceTabsRequest();
            }
        });
        sidebar.bindTreeViewToState(sidebar.getRoot());
        state.getObservableClusters().add(new ClusterStateViewModel(qa));
        clusterItem = sidebar.getRoot().getChildren().getLast();
    }

    @Test
    void renameUpdatesTreeInPlaceAndClosesOldTabs() throws Exception {
        onFx(() -> {
            setUp();
            TreeItem<String> untouched = clusterItem.getChildren().get(0);
            TreeItem<String> typo = clusterItem.getChildren().get(1);
            assertTrue(sidebar.isNamespaceItem(typo));

            sidebar.renameNamespace(typo, "  pagos-qa ");

            assertEquals(List.of("ventas-qa", "pagos-qa"), values(clusterItem));
            assertSame(untouched, clusterItem.getChildren().get(0), "el namespace sin cambios conserva su nodo");
            assertEquals(List.of(new SidebarStateViewModel.NamespaceRef("qa", "pagso-qa")), closeRequests);
        });
    }

    @Test
    void invalidRenameKeepsTree() throws Exception {
        onFx(() -> {
            setUp();
            TreeItem<String> typo = clusterItem.getChildren().get(1);

            assertThrows(IllegalArgumentException.class, () -> sidebar.renameNamespace(typo, "ventas-qa"));

            assertEquals(List.of("ventas-qa", "pagso-qa"), values(clusterItem));
            assertTrue(closeRequests.isEmpty());
        });
    }

    @Test
    void removingLastNamespaceShowsEmptyMessage() throws Exception {
        onFx(() -> {
            setUp();
            sidebar.removeNamespace(clusterItem.getChildren().get(1));
            sidebar.removeNamespace(clusterItem.getChildren().get(0));

            assertEquals(List.of("No hay namespaces registrados"), values(clusterItem));
            assertFalse(sidebar.isNamespaceItem(clusterItem.getChildren().getFirst()));
            assertEquals(2, closeRequests.size());
            assertTrue(saved.getFirst().getNamespaces().isEmpty());
        });
    }

    private static List<String> values(TreeItem<String> parent) {
        return parent.getChildren().stream().map(TreeItem::getValue).toList();
    }
}
