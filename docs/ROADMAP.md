# Roadmap Kabrilla

Objetivo: reemplazar Grafana para ver y filtrar logs de QA (equipo de 72 ingenieros). Grafana se cae y es lento.

Marca `[x]` al completar. Mantener este archivo al día: es la fuente de "qué sigue" entre sesiones.

## Base (hecho)
- [x] Migración a Spring Boot (beans en `bootstrap/`, sin singleton `AppBootstrap`)
- [x] Perfil `mock` con datos en `mock-data/` (deployments, pods y logs desde archivos)
- [x] Credenciales y estado local fuera de git (`.gitignore`)
- [x] Interruptor de estilos (`-Pstyles`); por defecto desactivados, tema base de JavaFX

## Alfa (uso propio + 2–3 compañeros)
- [x] Streaming de logs robusto
  - [x] `LogsViewModel`: `put` desde el hilo lector (contrapresión, sin excepción con la cola llena)
  - [x] `UiLogFeederService`: lotes cada 150 ms (antes 2 s), hasta 1000 líneas; se detiene con `stop()`
  - [x] Cerrar stream + feeder al cerrar la pestaña del deployment
  - [x] Sesiones: líneas/errores de un stream viejo se ignoran; `LogsAdapter` no reporta error al cancelar
  - [x] "Limpiar" ahora limpia el área de texto; hilos del stream real son daemon (la app cierra con streams abiertos)
  - [ ] Probar en EKS real (PC del trabajo): cambio de pod, cerrar pestaña, stream largo
- [ ] Búsqueda/filtro de logs (`txtSearch` existe en la vista pero no hace nada): texto, regex, nivel, resaltado
- [ ] Pausar autoscroll, limpiar, copiar selección
- [ ] Timestamps reales del pod (`timestamps=true` y parsear) en vez de `LogLine.now`
- [ ] Selector de container (quitar el default `movilidad-integration-product-container` de `LogsAdapter`)
- [ ] Pods por label selector del deployment (hoy `PodAdapter` filtra por prefijo del nombre)
- [ ] Seguridad: no imprimir token ni headers (`LoggingInterceptor`, `KubeClientFactory`, `JsonCredentialRepository` loguea credenciales con `@ToString`)
- [ ] Errores visibles en UI en vez de `printStackTrace` (NPE de listeners en `MainTabController`/`DeploymentTabController` ya corregidos)
- [ ] `KubeconfigAdapter` tolerante (users sin `exec`, CA inválida o por ruta)
- [ ] Estado local en `~/.kabrilla/` en vez de `user.dir`

## Beta (equipo completo)
- [ ] Autenticación vía perfiles AWS SSO / `aws eks get-token` sin pegar claves
- [ ] Logs combinados de todos los pods de un deployment (color por pod)
- [ ] Logs del container anterior (`previous=true`) para crashes; `sinceTime`
- [ ] Exportar logs a archivo
- [ ] Instalador con `jpackage` (.msi con JRE embebido) + README de instalación para el equipo
- [ ] CI (GitHub Actions) con build + tests
- [ ] Rediseño visual y volver a activar estilos por defecto (`kabrilla.ui.styles-enabled=true`)
- [ ] Evaluar quitar `module-info.java` (complica Spring y tests; jpackage no lo necesita)
