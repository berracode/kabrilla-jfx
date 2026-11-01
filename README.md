# Kabrilla

Cliente de escritorio para Kubernetes, multi-cluster, pensado para **ver y filtrar logs de pods rápido**,
sin depender de Grafana. Se conecta directo a los clusters (AWS EKS) con la API de Kubernetes.

> **Estado: pre-alfa.** Funciona la navegación, los logs en vivo con filtros y la consulta de recursos del
> deployment. El instalador y la autenticación SSO están en camino: ver [docs/ROADMAP.md](docs/ROADMAP.md).

## Qué hace hoy

- Lee los clusters de tu `~/.kube/config` y muestra los namespaces que registres por cluster.
- Lista deployments y pods de un namespace.
- **Logs en vivo** de un pod (como `kubectl logs -f`), varias pestañas a la vez, con:
  - búsqueda por texto o regex que **filtra y resalta** (los stack traces se mantienen junto a su error),
  - filtro por **nivel mínimo** (DEBUG / INFO / WARN / ERROR), detectado en logs de texto y JSON,
  - detener/reanudar, limpiar y auto-scroll.
- Por cada deployment, pestañas con sus **Services, HPA, ConfigMaps, AuthorizationPolicies y
  RequestAuthentications (Istio)**: datos clave legibles, todo copiable, y el YAML completo con "Copiar YAML".
- Guarda credenciales AWS temporales por cluster (las pegas desde la consola de AWS).

## Requisitos

- Java 21 o superior. No hace falta instalar Maven: el proyecto trae `mvnw` / `mvnw.cmd`.
- Para clusters reales:
  - AWS CLI v2 en el `PATH` (la app ejecuta `aws eks get-token` para autenticarse).
  - `~/.kube/config` con los clusters EKS: `aws eks update-kubeconfig --name <cluster> --region <region>`.
  - Permisos RBAC de lectura (`get`, `list`) sobre deployments, pods, pods/log, services, configmaps,
    horizontalpodautoscalers y, si usas Istio, authorizationpolicies y requestauthentications.
- Para el modo de prueba (mock): nada más.

## Ejecutar

Todos los comandos se corren desde la raíz del proyecto. En Windows con PowerShell o CMD usa `mvnw.cmd`
en lugar de `./mvnw` (en Git Bash sirve `./mvnw`).

| Comando | Qué hace |
|---|---|
| `./mvnw javafx:run` | App contra clusters reales (EKS) |
| `./mvnw javafx:run -Pmock` | App con datos simulados de `mock-data/`, sin AWS ni cluster |
| `./mvnw javafx:run -Pstyles` | Con los estilos personalizados (tema oscuro) |
| `./mvnw javafx:run -Pmock,styles` | Los perfiles se combinan |
| `./mvnw javafx:run -o -Pmock` | Igual, sin red (`-o` = offline, si ya descargaste las dependencias) |

**Desde IntelliJ:** ejecuta `co.com.bancolombia.kabrilla.startup.MainApplication`. En *Program arguments*
agrega lo que necesites:

| Argumento | Equivale a |
|---|---|
| `--spring.profiles.active=mock` | `-Pmock` |
| `--kabrilla.ui.styles-enabled=true` | `-Pstyles` |
| `--kabrilla.mock.line-delay-ms=20` | cambiar la velocidad de los logs mock (ver Configuración) |

El *Working directory* debe ser la raíz del proyecto (ahí busca `mock-data/` y los archivos locales).

### Modos

| Modo | Para qué |
|---|---|
| **Real** (sin perfil) | Usar contra EKS. Lee `~/.kube/config`, y `clusters.json` / `credentials.2.json` de la carpeta de ejecución. |
| **Mock** (`-Pmock`) | Desarrollar y probar sin acceso al cluster. Todo sale de [mock-data/](mock-data/README.md): deployments y pods son carpetas y archivos de logs; los recursos (Service, HPA...) son manifiestos YAML. |
| **Estilos** (`-Pstyles`) | Por ahora los estilos propios están **desactivados** y la app usa el tema base de JavaFX. Este perfil los activa sin tocar CSS ni FXML. El área de logs y los bloques YAML conservan siempre su fuente monoespaciada y el resaltado. |

## Configuración

Propiedades de Spring Boot. Se cambian con `--propiedad=valor` (IntelliJ) o editando los archivos:

| Propiedad | Default | Archivo | Descripción |
|---|---|---|---|
| `kabrilla.ui.styles-enabled` | `false` | `src/main/resources/application.properties` | Estilos personalizados on/off |
| `kabrilla.mock.dir` | `mock-data` | `src/main/resources/application-mock.properties` | Carpeta de datos del modo mock |
| `kabrilla.mock.line-delay-ms` | `100` | `src/main/resources/application-mock.properties` | Pausa entre líneas al reproducir logs mock. No bajar de ~20 |

## Uso rápido

1. Abre la app. En el panel izquierdo aparecen los clusters de tu kubeconfig.
2. **Crear namespace**: registra el namespace que quieres ver en un cluster.
3. **Agregar credenciales**: elige el cluster y pega el bloque de credenciales temporales de AWS
   (`aws_access_key_id=...`, `aws_secret_access_key=...`, `aws_session_token=...`).
4. Expande el namespace y selecciona un deployment: se abre una pestaña con sus pods, logs y recursos.
5. En **Logs**:
   - Elige el pod. El stream arranca solo con las últimas 200 líneas.
   - **Buscar** filtra mientras escribes. Activa **Regex** para expresiones regulares y **Mayús/minús** para
     distinguir mayúsculas. Al lado se ve cuántas líneas coinciden.
   - **Nivel mínimo** WARN muestra WARN y ERROR. Las líneas sin nivel reconocible solo se ven con "Todos".
   - Se puede refiltrar hasta las últimas 50.000 líneas recibidas; en pantalla se muestran las últimas 5.000.
   - **Detener / Reanudar** corta o vuelve a abrir el stream. **Limpiar** borra lo recibido.
6. En **HPA, ConfigMap, Service, AuthorizationPolicy y RequestAuthentication** se muestran solo los recursos
   asociados al deployment (por selector, `scaleTargetRef` o referencias del pod). Se consultan al abrir la
   pestaña; **Recargar** vuelve a consultar.
7. Al cerrar la pestaña del deployment se cierra su stream de logs.

## Archivos locales

En modo real la app guarda su estado en la carpeta desde donde se ejecuta. **No los subas a git**
(ya están en `.gitignore`):

| Archivo | Contenido |
|---|---|
| `clusters.json` | namespaces registrados por cluster |
| `credentials.2.json` | credenciales AWS por cluster (**secretos**) |

## Desarrollo

| Comando | Qué hace |
|---|---|
| `./mvnw compile` | Compilar |
| `./mvnw test` | Correr los tests (algunos abren JavaFX: necesitan sesión gráfica, no corren en un servidor sin pantalla) |
| `./mvnw test -Dtest=LogFilterTest` | Correr un solo test |
| `./mvnw clean` | Borrar `target/` |
| `cd mock-data && ./generar-logs.sh <archivo> <líneas>` | Generar logs sintéticos para el modo mock (Git Bash). Ver [mock-data/README.md](mock-data/README.md) |

Arquitectura hexagonal + MVVM:

```
src/main/java/co/com/bancolombia/kabrilla/
├── domain/          modelos y puertos (interfaces) — sin Spring ni JavaFX
├── application/     casos de uso (servicios)
├── infrastructure/  adaptadores: k8s (API Kubernetes), fs (archivos), mock (perfil mock),
│                    manifest (lógica común real/mock para recursos)
├── bootstrap/       cableado de Spring (beans por perfil)
├── startup/         arranque de Spring + JavaFX
└── ui/              controladores FXML, view models, tema
src/main/resources/fxml/   vistas
mock-data/                 datos simulados para -Pmock
docs/ROADMAP.md            qué sigue
```

Más detalle técnico (decisiones y trampas conocidas, como `module-info.java` con Spring) en [CLAUDE.md](CLAUDE.md).

### Contribuir

- Una rama por cambio y PR contra `master`.
- Todo cambio debe poder probarse en modo `-Pmock`: si agregas un adaptador real, agrega su versión mock.
- Código (clases, métodos, variables, tests) en inglés; comentarios, logs y textos de UI en español.
- Nunca subas credenciales, kubeconfigs reales ni logs o manifiestos de clusters reales.
- Si agregas un comando o una forma de ejecutar algo, documéntalo en este README.
