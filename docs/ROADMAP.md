# Roadmap Kabrilla

Objetivo: reemplazar Grafana para ver y filtrar logs de QA (equipo de 72 ingenieros). Grafana se cae y es lento.

Marca `[x]` al completar. Mantener este archivo al día: es la fuente de "qué sigue" entre sesiones.

## Base (hecho)
- [x] Migración a Spring Boot (beans en `bootstrap/`, sin singleton `AppBootstrap`)
- [x] Perfil `mock` con datos en `mock-data/` (deployments, pods y logs desde archivos)
- [x] Credenciales y estado local fuera de git (`.gitignore`)
- [x] Interruptor de estilos: tema oscuro por defecto, `-Pno-styles` = tema base de JavaFX

## Alfa (uso propio + 2–3 compañeros)
- [x] Streaming de logs robusto
  - [x] `LogsViewModel`: `put` desde el hilo lector (contrapresión, sin excepción con la cola llena)
  - [x] `UiLogFeederService`: lotes cada 150 ms (antes 2 s), hasta 1000 líneas; se detiene con `stop()`
  - [x] Cerrar stream + feeder al cerrar la pestaña del deployment
  - [x] Sesiones: líneas/errores de un stream viejo se ignoran; `LogsAdapter` no reporta error al cancelar
  - [x] "Limpiar" ahora limpia el área de texto; hilos del stream real son daemon (la app cierra con streams abiertos)
  - [ ] Probar en EKS real (PC del trabajo): cambio de pod, cerrar pestaña, stream largo
- [x] Búsqueda/filtro de logs: texto o regex, mayús/minús, nivel mínimo, resaltado, stack traces agrupados
- [x] Auto-scroll on/off, limpiar, detener/reanudar; botones sin función eliminados
- [x] Pestañas HPA, ConfigMap, Service, AuthorizationPolicy, RequestAuthentication con datos reales (adaptador
      K8s + mock YAML), vista legible y copiable (antes datos inventados en código)
  - [ ] Probar en EKS real: permisos RBAC, Istio v1 vs v1beta1, clusters sin Istio
- [ ] Navegar entre coincidencias (anterior/siguiente) y copiar selección del área de logs con atajo
- [x] Timestamps reales del pod (`timestamps=true` y parsear) en vez de `LogLine.now`, mostrados en hora local
- [x] Selector de container: default por anotación `default-container` o primer no-sidecar (antes había un
      container escrito a mano y solo funcionaban los logs de un deployment)
  - [ ] Confirmar en EKS con varios deployments e Istio
- [ ] Pods por label selector del deployment (hoy `PodAdapter` filtra por prefijo del nombre)
- [ ] Seguridad: no imprimir token ni headers (`LoggingInterceptor`, `KubeClientFactory`, `JsonCredentialRepository` loguea credenciales con `@ToString`)
- [ ] Errores visibles en UI en vez de `printStackTrace` (NPE de listeners en `MainTabController`/`DeploymentTabController` ya corregidos)
- [ ] `KubeconfigAdapter` tolerante (users sin `exec`, CA inválida o por ruta)
- [ ] Estado local en `~/.kabrilla/` en vez de `user.dir`

## Beta (equipo completo)
- [x] Historial por fecha desde la API de Kubernetes: rangos rápidos, personalizado desde/hasta, todo lo
      disponible y container anterior (`previous`); aviso de cobertura y de recorte (máx. 50.000 líneas).
      La API no tiene `sinceTime` ni "hasta": se usa `sinceSeconds` y se recorta por timestamp (`LogRangeCollector`)
  - [ ] Probar en EKS: cobertura real según rotación del kubelet, `previous` tras un crash
- [ ] **Loki para dev y QA**: historial de días y de pods ya borrados, consultando la API de Loki directamente
      (`/loki/api/v1/query_range` con LogQL) sin pasar por la UI de Grafana. Pendiente averiguar internamente
      URL, autenticación y labels (namespace/pod/container). Encajaría como otro `LogsRepository.fetchPodLogs`
      (mismo `LogRange`/`LogFetchResult`), elegible por ambiente
- [ ] Búsqueda entre pods/deployments por `trace_id` (depende de Loki)
- [ ] Autenticación vía perfiles AWS SSO / `aws eks get-token` sin pegar claves
- [ ] (Baja prioridad) Logs combinados de todos los pods de un deployment (color por pod)
- [x] Logs del container anterior (`previous=true`) para crashes; búsqueda por fecha (ver arriba)
- [ ] (Evaluado, en pausa) Grabar logs en SQLite local: solo como copia paralela opcional, no en el camino en vivo;
      validar antes política de seguridad sobre logs de QA en disco y si 50.000 líneas se quedan cortas
- [ ] Exportar logs a archivo
- [ ] Instalador con `jpackage` (.msi con JRE embebido) + README de instalación para el equipo
- [ ] CI (GitHub Actions) con build + tests
- [x] Rediseño visual: tema oscuro negro + morados con paleta centralizada (`kabrilla-dark.css`), sin estilos en línea
- [ ] Colorear líneas de log por nivel (ERROR/WARN) con clases del tema
- [ ] Evaluar quitar `module-info.java` (complica Spring y tests; jpackage no lo necesita)
