package co.com.bancolombia.kabrilla.ui.viewmodel.state.base;

public class AppContext {
    private final AppState appState = new AppState();

    public AppState getAppState() {
        return appState;
    }
}
