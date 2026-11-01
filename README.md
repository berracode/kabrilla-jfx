# Kabrilla

Cliente de escritorio para Kubernetes, multi-cluster, pensado para **ver y filtrar logs de pods rápido**,
sin depender de Grafana. Se conecta directo a los clusters (AWS EKS) con la API de Kubernetes.

> **Estado: pre-alfa.** Se puede navegar clusters → namespaces → deployments → pods y seguir logs en vivo.
> El filtrado de logs y el instalador están en camino: ver [docs/ROADMAP.md](docs/ROADMAP.md).

## Qué hace hoy

- Lee los clusters de tu `~/.kube/config` y muestra los namespaces que registres por cluster.
- Lista deployments y pods de un namespace.
- Sigue los logs de un pod en vivo (como `kubectl logs -f`), con varias pestañas abiertas a la vez.
- Guarda credenciales AWS temporales por cluster (las pegas desde la consola de AWS).

## Requisitos

- Java 21 o superior (no hace falta instalar Maven: el proyecto trae `mvnw`)
- Para clusters reales: AWS CLI (`aws eks get-token`) y un `~/.kube/config` con los clusters EKS
  (`aws eks update-kubeconfig --name <cluster>`)
- Para el modo de prueba: nada más

## Ejecutar

```bash
./mvnw javafx:run                  # clusters reales (EKS)
./mvnw javafx:run -Pmock           # datos simulados de mock-data/, sin AWS ni cluster
./mvnw javafx:run -Pstyles         # con los estilos personalizados (dark theme)
./mvnw javafx:run -Pmock,styles    # se pueden combinar
```

En Windows con PowerShell o CMD usa `mvnw.cmd` en lugar de `./mvnw`.

Desde IntelliJ: ejecuta `co.com.bancolombia.kabrilla.startup.MainApplication` y agrega los argumentos
que necesites en *Program arguments*: `--spring.profiles.active=mock` y/o `--kabrilla.ui.styles-enabled=true`.

### Modos

| Modo | Cómo | Para qué |
|---|---|---|
| **Real** | sin perfil | Usar contra EKS. Lee `~/.kube/config`, `clusters.json` y `credentials.2.json` de la carpeta de ejecución. |
| **Mock** | `-Pmock` | Desarrollar y probar sin acceso al cluster. Todo sale de [mock-data/](mock-data/README.md): deployments = carpetas, pods = archivos `.log`, logs = su contenido (y lo que les agregues aparece en vivo). |
| **Estilos** | `-Pstyles` | Por ahora los estilos propios están **desactivados** y la app usa el tema base de JavaFX. Este perfil los activa sin tocar CSS ni FXML. El valor por defecto está en `src/main/resources/application.properties`. |

## Uso rápido

1. Abre la app. En el panel izquierdo aparecen los clusters de tu kubeconfig.
2. **Crear namespace**: registra el namespace que quieres ver en un cluster.
3. **Agregar credenciales**: elige el cluster y pega el bloque de credenciales temporales de AWS
   (`aws_access_key_id=...`, `aws_secret_access_key=...`, `aws_session_token=...`).
4. Expande el namespace y selecciona un deployment: se abre una pestaña con sus pods y logs.
5. Al cerrar la pestaña se cierra el stream de logs.

## Archivos locales

En modo real la app guarda su estado en la carpeta desde donde se ejecuta. **No los subas a git**
(ya están en `.gitignore`):

| Archivo | Contenido |
|---|---|
| `clusters.json` | namespaces registrados por cluster |
| `credentials.2.json` | credenciales AWS por cluster (**secretos**) |

## Desarrollo

```bash
./mvnw compile     # compilar
./mvnw test        # tests (incluye tests de UI que abren JavaFX)
```

Arquitectura hexagonal + MVVM:

```
src/main/java/co/com/bancolombia/kabrilla/
├── domain/          modelos y puertos (interfaces) — sin Spring ni JavaFX
├── application/     casos de uso (servicios)
├── infrastructure/  adaptadores: k8s (API Kubernetes), fs (archivos), mock (perfil mock)
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
- Nunca subas credenciales, kubeconfigs reales ni logs de clusters reales.
