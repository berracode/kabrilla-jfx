# Roadmap Kabrilla

Objetivo: reemplazar Grafana para ver y filtrar logs de QA (equipo de 72 ingenieros). Grafana se cae y es lento.

Marca `[x]` al completar. Mantener este archivo al día: es la fuente de "qué sigue" entre sesiones.

## Base (hecho)
- [x] Migración a Spring Boot (beans en `bootstrap/`, sin singleton `AppBootstrap`)
- [x] Perfil `mock` con datos en `mock-data/` (deployments, pods y logs desde archivos)
- [x] Credenciales y estado local fuera de git (`.gitignore`)

## Alfa (uso propio + 2–3 compañeros)
- [ ] Streaming de logs robusto
  - [ ] `LogsViewModel`: usar `offer` desde el hilo lector (hoy `add` en `Platform.runLater` lanza excepción con la cola llena)
  - [ ] `UiLogFeederService`: bajar `Thread.sleep(2000)` (~100–250 ms) y detenerlo al parar el stream
  - [ ] Cerrar stream + feeder al cerrar la pestaña (`LogsContentController.dispose()` nunca se llama)
  - [ ] El cierre de un stream viejo no debe marcar error ni `following=false` del stream nuevo (`LogsAdapter` llama `onError` al cancelar)
- [ ] Búsqueda/filtro de logs (`txtSearch` existe en la vista pero no hace nada): texto, regex, nivel, resaltado
- [ ] Pausar autoscroll, limpiar, copiar selección
- [ ] Timestamps reales del pod (`timestamps=true` y parsear) en vez de `LogLine.now`
- [ ] Selector de container (quitar el default `movilidad-integration-product-container` de `LogsAdapter`)
- [ ] Pods por label selector del deployment (hoy `PodAdapter` filtra por prefijo del nombre)
- [ ] Seguridad: no imprimir token ni headers (`LoggingInterceptor`, `KubeClientFactory`, `JsonCredentialRepository` loguea credenciales con `@ToString`)
- [ ] Errores visibles en UI en vez de `printStackTrace`; NPE en listeners (`MainTabController`, `DeploymentTabController`)
- [ ] `KubeconfigAdapter` tolerante (users sin `exec`, CA inválida o por ruta)
- [ ] Estado local en `~/.kabrilla/` en vez de `user.dir`

## Beta (equipo completo)
- [ ] Autenticación vía perfiles AWS SSO / `aws eks get-token` sin pegar claves
- [ ] Logs combinados de todos los pods de un deployment (color por pod)
- [ ] Logs del container anterior (`previous=true`) para crashes; `sinceTime`
- [ ] Exportar logs a archivo
- [ ] Instalador con `jpackage` (.msi con JRE embebido) + README de instalación para el equipo
- [ ] CI (GitHub Actions) con build + tests
- [ ] Evaluar quitar `module-info.java` (complica Spring y tests; jpackage no lo necesita)
