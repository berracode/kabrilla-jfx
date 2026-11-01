# Kabrilla

Cliente desktop de Kubernetes (JavaFX + Spring Boot) multi-cluster, enfocado en ver y filtrar logs de pods.
Objetivo: reemplazar Grafana para los logs de QA de un equipo de 72 ingenieros. Clusters objetivo: AWS EKS.
Qué sigue y estado de bugs conocidos: [docs/ROADMAP.md](docs/ROADMAP.md) — actualizarlo al terminar una tarea.

## Entorno
- En el PC personal **no hay acceso a EKS**: desarrollar y verificar siempre con el perfil `mock`.
  En el PC del trabajo se prueba contra EKS real (perfil por defecto).
- Windows, Java 21+, Maven wrapper. Para correr la app desde Claude: `timeout 40 ./mvnw -q -o javafx:run -Pmock`
  y revisar los logs (abre una ventana; no se puede interactuar con la UI, así que preferir tests).

## Comandos
```bash
./mvnw compile                 # compilar
./mvnw test                    # tests (JUnit vía BOM de Spring Boot, corren en classpath)
./mvnw javafx:run -Pmock       # app con datos de mock-data/ (sin AWS)
./mvnw javafx:run              # app real: ~/.kube/config + credentials.2.json + aws CLI
./mvnw javafx:run -Pmock,no-styles  # sin tema oscuro (tema base de JavaFX)
```
Desde IntelliJ: ejecutar `MainApplication` con `--spring.profiles.active=mock` y/o `--kabrilla.ui.styles-enabled=false`.
Los perfiles Maven se combinan porque cada uno llena su propia propiedad (`kabrilla.args.*`) de `javafx.args`.

## Arquitectura (hexagonal + MVVM)
`co.com.bancolombia.kabrilla`:
- `domain/model`, `domain/gateway` — modelos y puertos (interfaces). **Sin Spring ni JavaFX.**
- `application/service` — casos de uso. **Sin anotaciones de Spring**; se registran como `@Bean` en `bootstrap/`.
- `infrastructure/adapters` — `k8s/` (API de Kubernetes), `fs/` (archivos JSON), `mock/` (perfil mock),
  `manifest/` (lógica compartida real/mock para recursos: ver abajo).
- `bootstrap/` — cableado Spring: `BeanConfiguration` (servicios y estado de UI),
  `K8sAdaptersConfiguration` (`@Profile("!mock")`), `MockAdaptersConfiguration` (`@Profile("mock")`).
  Un adaptador nuevo va en **ambos** perfiles (real y mock).
- `startup/` — `MainApplication` → `JavaFxApplication` levanta Spring en `init()` y publica `StageReadyEvent`;
  `PrimaryStageInitializer` carga `fxml/main.fxml` con `controllerFactory = context::getBean`.
- `ui/` — `controller/` (FXML), `viewmodel/` (estado observable), `services/` (tareas de fondo).
  - Controladores de `main.fxml` y sus `fx:include` son `@Component` singleton.
  - Pestañas por deployment (`DeploymentTabController`) y sus comandos (Pods, Logs...) se crean a mano con
    `loader.setControllerFactory(...)`, pasando servicios inyectados en `MainTabController`. Hay un set por pestaña.
  - Árbol del sidebar: root → cluster (nombre corto) → namespace → deployment (`SidebarStateViewModel`).

## Trampas conocidas
- **module-info.java**: Spring corre en el classpath (módulo sin nombre). Todo paquete con clases que Spring
  instancia o inspecciona necesita `exports` (y `opens` si usa reflexión privada). Cada paquete con FXML
  controllers necesita `opens ... to javafx.fxml`. Si aparece `IllegalAccessException ... does not export`,
  falta un `exports`.
- **Módulos del JDK**: con `module-info` solo se cargan los módulos del JDK declarados con `requires`. Si aparece
  `NoClassDefFoundError: java/...` (p. ej. `java/sql/Date` del cliente de Kubernetes) falta un `requires`.
  Al agregar o actualizar dependencias, revisar con
  `jdeps --print-module-deps --ignore-missing-deps --multi-release 21 <jars>`. El modo mock NO ejercita el
  cliente de Kubernetes, así que estos errores solo aparecen contra EKS.
- Nombres de cluster: el kubeconfig usa ARN (`arn:aws:eks:...:cluster/<nombre>`); el resto de la app usa
  `<nombre>` (`split("/")[1]`). Credenciales y `clusters.json` se buscan por nombre corto.
- El IDE (VS Code) marca errores falsos de Lombok; la fuente de verdad es `./mvnw compile`.
- Flujo de logs: `LogsAdapter` (hilo lector) → `LogsViewModel.logBuffer` (cola 5000, `put` bloqueante) →
  `UiLogFeederService` (lotes de hasta 1000 cada 150 ms) → `InlineCssTextArea` (máx. 5000 párrafos).
  Cada `startFollowing`/`stop` incrementa una sesión: callbacks de streams anteriores se descartan.
  Al cerrar la pestaña: `MainTabController` → `DeploymentTabController.dispose()` → `LogsContentController.dispose()`.
- Estilos: `ThemeManager` agrega a TODA ventana `css/kabrilla-base.css` (siempre: monoespaciada, resaltado) y
  `css/kabrilla-dark.css` si `kabrilla.ui.styles-enabled=true` (default). Paleta = looked-up colors en `.root` del
  tema (`-kb-*`); reutilizarlas, nunca colores literales. FXML/Java sin `style=` ni `setStyle`: solo clases `kb-*`
  (lista en README, "Tema y estilos"). El área de logs es `StyleClassedTextArea` (resaltado por clase
  `kb-search-match`). `-Pno-styles` para ver el tema base de JavaFX.
- Filtro de logs: `LogsViewModel` guarda un `LogHistory` (50k líneas agrupadas en `LogEntry`: línea + su stack
  trace) y aplica un `LogFilter` inmutable (texto/regex + `LogLevel` mínimo). Habla con la vista por `LogView`
  (`append`/`reset`); `LogsContentController` la implementa sobre RichTextFX y resalta con
  `setStyle(par, from, to, List.of("kb-search-match"))`.
- Logs por rango: `LogsRepository.fetchPodLogs(..., LogRange, maxLines)` → `LogFetchResult`. La API de pod logs no
  tiene `sinceTime` ni "hasta": se pide `sinceSeconds` y `LogRangeCollector` (dominio, compartido real/mock) recorta
  por timestamp. Real: `timestamps=true` y `LogsAdapter.parseTimestamped` lee el prefijo RFC3339 del kubelet.
  `LogsViewModel.loadRange` usa la misma sesión que el stream (cambiar de pod/rango descarta resultados viejos).
  Mock: el archivo del pod se fecha hacia atrás desde el arranque (`history-step-ms`); `<pod>.previous.log` = previous.
  `LogsRepository` ya no es interfaz funcional: en tests usar clase anónima.
- Ojo con builds incrementales: tras cambiar interfaces, verificar con `./mvnw clean test` (Maven puede reusar
  clases de test viejas y dar falsos verdes).
- Recursos (pestañas HPA/ConfigMap/Service/AuthorizationPolicy/RequestAuthentication): una sola vista genérica
  (`ResourcesContentController` + `resources-content.fxml`), carga perezosa al seleccionar la pestaña.
  `ManifestResourceRepository` (abstracta) decide qué recursos pertenecen al deployment y `ManifestMapper` arma el
  resumen + YAML, ambos sobre manifiestos como `Map`. `KubeResourceAdapter` (real, vía `JSON.serialize` del cliente
  → Map; Istio por `CustomObjectsApi` v1 → v1beta1) y `MockKubeResourceAdapter` (YAML de `mock-data/resources`)
  solo obtienen manifiestos. Un recurso nuevo: agregar a `ResourceKind`, a ambos `listManifests` y al mapper.
- Tests de UI: `FxTestSupport` (src/test) arranca el toolkit JavaFX; usar `onFx(...)` para código en el hilo FX.
  Tests solo para lógica crítica (filtro, asociación de recursos, streaming), no uno por clase.

## Convenciones
- Código en inglés (clases, métodos, variables, nombres de tests); comentarios, logs y textos de UI en español.
- Toda forma nueva de ejecutar algo (comando, perfil, propiedad, script) se documenta en README.md.
- En la UI no dejar botones o controles que no hagan nada; nada de datos inventados en vistas (usar mock-data).
- Lombok (`@Slf4j`, `@Getter`, `@Builder`...) como en el resto del código. Inyección por constructor.
- Nunca versionar credenciales ni loguear tokens/headers de autorización. Los archivos locales
  (`credentials*.json`, `clusters.json`) están en `.gitignore`; los datos de prueba van en `mock-data/`.
- **Los commits los hace el usuario**: no ejecutar `git commit`/`push`; dejar los cambios listos y resumirlos.
