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
```
Desde IntelliJ: ejecutar `MainApplication` con `--spring.profiles.active=mock`.

## Arquitectura (hexagonal + MVVM)
`co.com.bancolombia.kabrilla`:
- `domain/model`, `domain/gateway` — modelos y puertos (interfaces). **Sin Spring ni JavaFX.**
- `application/service` — casos de uso. **Sin anotaciones de Spring**; se registran como `@Bean` en `bootstrap/`.
- `infrastructure/adapters` — `k8s/` (API de Kubernetes), `fs/` (archivos JSON), `mock/` (perfil mock).
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
- Nombres de cluster: el kubeconfig usa ARN (`arn:aws:eks:...:cluster/<nombre>`); el resto de la app usa
  `<nombre>` (`split("/")[1]`). Credenciales y `clusters.json` se buscan por nombre corto.
- El IDE (VS Code) marca errores falsos de Lombok; la fuente de verdad es `./mvnw compile`.
- Flujo de logs: `LogsAdapter` (hilo lector) → `LogsViewModel.logBuffer` (cola 5000) → `UiLogFeederService`
  (lotes) → `InlineCssTextArea` (máx. 5000 párrafos). Ver bugs pendientes en el roadmap.

## Convenciones
- Código, comentarios, logs y mensajes de UI en español.
- Lombok (`@Slf4j`, `@Getter`, `@Builder`...) como en el resto del código. Inyección por constructor.
- Nunca versionar credenciales ni loguear tokens/headers de autorización. Los archivos locales
  (`credentials*.json`, `clusters.json`) están en `.gitignore`; los datos de prueba van en `mock-data/`.
- **Los commits los hace el usuario**: no ejecutar `git commit`/`push`; dejar los cambios listos y resumirlos.
