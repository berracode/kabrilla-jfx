# Kabrilla

Cliente de escritorio para Kubernetes, multi-cluster, pensado para **ver y filtrar logs de pods rápido**,
sin depender de Grafana. Se conecta directo a los clusters (AWS EKS) con la API de Kubernetes.

> **Estado: beta.** Probada contra EKS: navegación, logs en vivo con filtros, historial por rango y recursos del
> deployment. Próximos pasos en [docs/ROADMAP.md](docs/ROADMAP.md).

## Instalación (Windows, versión portable)

1. Descarga `kabrilla-X.Y.Z-win-portable.zip` de la página **Releases** del repositorio.
2. Descomprímelo donde quieras (p. ej. `C:\Tools\Kabrilla`). No necesita instalar Java: trae su propio runtime.
3. Abre `Kabrilla\Kabrilla.exe`. Se abre también una consola con los logs de la app (útil si algo falla; nunca
   muestra tokens ni credenciales). La versión aparece en el título de la ventana.
   - Si Windows muestra "Windows protegió su PC" (el ejecutable aún no está firmado): *Más información* →
     *Ejecutar de todas formas*.
4. Requisitos para conectarte a EKS: ver [Requisitos](#requisitos) (AWS CLI y `~/.kube/config`).

Tus namespaces registrados y credenciales se guardan en `%USERPROFILE%\.kabrilla\` (ver [Archivos locales](#archivos-locales)):
para actualizar basta con descomprimir la versión nueva y borrar la carpeta anterior.

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

- Para desarrollar: Java 21 o superior (JDK, incluye `jpackage`). No hace falta instalar Maven: el proyecto trae
  `mvnw` / `mvnw.cmd`. La versión portable no requiere Java.
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
| `./mvnw javafx:run -Pno-styles` | Sin el tema oscuro: tema base de JavaFX |
| `./mvnw javafx:run -Pmock,no-styles` | Los perfiles se combinan |
| `./mvnw javafx:run -o -Pmock` | Igual, sin red (`-o` = offline, si ya descargaste las dependencias) |

**Desde IntelliJ:** ejecuta `co.com.bancolombia.kabrilla.startup.MainApplication`. En *Program arguments*
agrega lo que necesites:

| Argumento | Equivale a |
|---|---|
| `--spring.profiles.active=mock` | `-Pmock` |
| `--kabrilla.ui.styles-enabled=false` | `-Pno-styles` |
| `--kabrilla.mock.line-delay-ms=20` | cambiar la velocidad de los logs mock (ver Configuración) |

El *Working directory* debe ser la raíz del proyecto (ahí busca `mock-data/` y los archivos locales).

### Modos

| Modo | Para qué |
|---|---|
| **Real** (sin perfil) | Usar contra EKS. Lee `~/.kube/config`, y `clusters.json` / `credentials.2.json` de la carpeta de ejecución. |
| **Mock** (`-Pmock`) | Desarrollar y probar sin acceso al cluster. Todo sale de [mock-data/](mock-data/README.md): deployments y pods son carpetas y archivos de logs; los recursos (Service, HPA...) son manifiestos YAML. |
| **Tema** (`-Pno-styles` / `-Pstyles`) | Por defecto la app usa el **tema oscuro** de Kabrilla (negro + morados). `-Pno-styles` lo apaga y queda el tema base de JavaFX; `-Pstyles` lo fuerza si cambiaste el default. En ambos casos se conservan la fuente monoespaciada de logs/YAML y el resaltado de búsqueda. Ver "Tema y estilos". |

## Configuración

Propiedades de Spring Boot. Se cambian con `--propiedad=valor` (IntelliJ) o editando los archivos:

| Propiedad | Default | Archivo | Descripción |
|---|---|---|---|
| `kabrilla.ui.styles-enabled` | `true` | `src/main/resources/application.properties` | Tema oscuro on/off (off = tema base de JavaFX) |
| `kabrilla.data-dir` | `${user.home}/.kabrilla` | `src/main/resources/application.properties` | Carpeta de datos local (namespaces y credenciales) |
| `kabrilla.kubeconfig` | (vacío) | — | Kubeconfig a usar. Si está vacío: variable `KUBECONFIG` (primer archivo) y si no `~/.kube/config` |
| `kabrilla.mock.dir` | `mock-data` | `src/main/resources/application-mock.properties` | Carpeta de datos del modo mock |
| `kabrilla.mock.line-delay-ms` | `100` | `src/main/resources/application-mock.properties` | Pausa entre líneas al reproducir logs mock. No bajar de ~20 |
| `kabrilla.mock.history-step-ms` | `2000` | `src/main/resources/application-mock.properties` | Separación en el tiempo entre líneas de un archivo mock para las consultas por **Rango** |

## Uso rápido

1. Abre la app. En el panel izquierdo aparecen los clusters de tu kubeconfig.
2. **Crear namespace**: registra el namespace que quieres ver en un cluster.
3. **Agregar credenciales**: elige el cluster y pega el bloque de credenciales temporales de AWS
   (`aws_access_key_id=...`, `aws_secret_access_key=...`, `aws_session_token=...`).
4. Expande el namespace y selecciona un deployment: se abre una pestaña con sus pods, logs y recursos.
5. En **Logs**:
   - Elige el pod. El stream arranca solo con las últimas 200 líneas.
   - **Container**: con Istio cada pod tiene al menos dos (la app e `istio-proxy`). Se preselecciona el de la
     app: el de la anotación `kubectl.kubernetes.io/default-container` si el pod la tiene, si no el primero que
     no sea un sidecar conocido (`istio-proxy`, `linkerd-proxy`, `aws-xray-daemon`, ...). Cámbialo para ver otro.
   - **Buscar** filtra mientras escribes. Activa **Regex** para expresiones regulares y **Mayús/minús** para
     distinguir mayúsculas. Al lado se ve cuántas líneas coinciden.
   - **Nivel mínimo** WARN muestra WARN y ERROR. Las líneas sin nivel reconocible solo se ven con "Todos".
   - Se puede refiltrar hasta las últimas 50.000 líneas recibidas; en pantalla se muestran las últimas 5.000.
   - **Detener / Reanudar** corta o vuelve a abrir el stream. **Limpiar** borra lo recibido.
   - Las horas que se muestran son las **del pod** (las pone el kubelet), en la zona horaria de tu equipo.
   - **Rango** consulta historial en vez de seguir en vivo: *Últimos 15 min*, *Última hora*, *Últimas 6 h*,
     *Todo lo disponible* o *Personalizado* (desde/hasta, fecha y hora `HH:mm` locales, luego **Consultar**).
     Los filtros de búsqueda y nivel se aplican igual sobre el resultado. **Consultar** repite la consulta; volver
     a *En vivo* reabre el stream.
   - **Container anterior** (solo en modo rango): logs del container que murió antes del último reinicio, útil
     para ver por qué crasheó (p. ej. un `OutOfMemoryError`).
   - Límites del historial de Kubernetes: solo conserva el archivo de log **actual** de cada container (el kubelet
     lo rota por tamaño; en pods con mucho tráfico pueden ser pocos minutos) y nada de pods que ya no existen
     (p. ej. tras un despliegue). Si el rango pedido empieza antes de lo que hay, la app avisa desde cuándo
     tiene datos. Un rango trae como máximo 50.000 líneas; si hay más, se avisa y conviene acotarlo.
6. En **HPA, ConfigMap, Service, AuthorizationPolicy y RequestAuthentication** se muestran solo los recursos
   asociados al deployment (por selector, `scaleTargetRef` o referencias del pod). Se consultan al abrir la
   pestaña; **Recargar** vuelve a consultar.
7. Al cerrar la pestaña del deployment se cierra su stream de logs.

## Problemas comunes

| Síntoma | Causa / solución |
|---|---|
| `NoClassDefFoundError: java/sql/...` u otra clase `java/...` al expandir un namespace | Falta un módulo del JDK en `module-info.java` (`requires java.sql;`). Ver la sección "Trampas conocidas" de [CLAUDE.md](CLAUDE.md). |
| Logs vacíos o `a container name must be specified` | Elige el container correcto en el combo **Container** de la pestaña Logs. |
| `Error K8s (401)` | Credenciales AWS vencidas: vuelve a pegarlas con **Agregar credenciales**. |
| `Error K8s (403)` | Tu rol no tiene permisos RBAC de lectura sobre ese recurso en el namespace. |
| Falta un cluster en el panel | Solo se muestran clusters **EKS** (nombre `arn:aws:eks:...:cluster/<nombre>`) con `certificate-authority-data` válido; los demás se omiten y la consola dice por qué. Regenera con `aws eks update-kubeconfig`. |
| "No se pudo leer ~/.kube/config" | No existe el kubeconfig: `aws eks update-kubeconfig --name <cluster> --region <region>`, o usa `KUBECONFIG` / `--kabrilla.kubeconfig=<ruta>`. |
| "No se pudo ejecutar AWS CLI" / "AWS CLI no devolvió token" | Instala AWS CLI v2 y verifica que `aws` esté en el PATH; si dice `ExpiredToken`, vuelve a pegar las credenciales. |
| Rango con "Kubernetes conserva logs de este container solo desde ..." | Normal: Kubernetes ya rotó o perdió lo anterior. Para más historia hará falta Loki (ver roadmap). |
| `HTTP 400: previous terminated container ... not found` | El container no ha reiniciado: no hay "container anterior". |
| Pestañas de Istio vacías | El cluster no tiene Istio instalado o no hay políticas para ese deployment. |

## Archivos locales

En modo real la app guarda su estado en la carpeta de datos del usuario, `~/.kabrilla/`
(`%USERPROFILE%\.kabrilla\` en Windows; configurable con `kabrilla.data-dir`). Está en tu perfil, así que solo tu
usuario puede leerla y sobrevive a las actualizaciones de la app.

| Archivo | Contenido |
|---|---|
| `clusters.json` | namespaces registrados por cluster |
| `credentials.json` | credenciales AWS temporales por cluster (**secretos**, en texto plano como `~/.aws/credentials`) |

Versiones anteriores guardaban `clusters.json` y `credentials.2.json` en la carpeta desde donde se ejecutaba la app:
en el primer arranque se **copian** a `~/.kabrilla/` (sin sobrescribir nada) y el log lo indica. Después puedes
borrar los archivos viejos. Nunca los subas a git (están en `.gitignore`).

## Desarrollo

| Comando | Qué hace |
|---|---|
| `./mvnw compile` | Compilar |
| `./mvnw test` | Correr los tests (algunos abren JavaFX: necesitan sesión gráfica, no corren en un servidor sin pantalla) |
| `./mvnw test -Dtest=LogFilterTest` | Correr un solo test |
| `./mvnw clean` | Borrar `target/` |
| `./mvnw test -DexcludedGroups=ui` | Tests sin los de UI (`@Tag("ui")`, abren JavaFX); así corren en CI |
| `./mvnw clean package -Pportable -Drevision=0.9.0` | Genera la app portable en `target/dist/Kabrilla/` (jpackage, con JRE incluido). Probarla: `target/dist/Kabrilla/Kabrilla.exe` (agrega `--spring.profiles.active=mock` para datos simulados, desde la raíz del proyecto) |
| `cd mock-data && ./generar-logs.sh <archivo> <líneas>` | Generar logs sintéticos para el modo mock (Git Bash). Ver [mock-data/README.md](mock-data/README.md) |

### Tema y estilos

| Archivo | Qué contiene |
|---|---|
| `src/main/resources/css/kabrilla-dark.css` | El tema oscuro. Arriba está la **paleta** (`-kb-bg`, `-kb-primary`, `-kb-text`...): para cambiar un color se edita solo ahí. Luego, ajustes de controles y las clases `kb-*` |
| `src/main/resources/css/kabrilla-base.css` | Lo que se carga siempre, con o sin tema: fuente monoespaciada (`kb-mono`, área de logs) y resaltado de búsqueda (`kb-search-match`) |
| `ui/theme/ThemeManager.java` | Aplica esas hojas a todas las ventanas (principal, diálogos, alertas) según `kabrilla.ui.styles-enabled` |

Reglas: en FXML y Java **no** se escriben colores ni `style="..."`; se usan clases (`styleClass="kb-title"`,
`kb-primary` para la acción principal, `kb-ghost` para acciones discretas, `kb-muted`, `kb-field-label`, `kb-error`...).
Si hace falta un estilo nuevo, se agrega una clase en `kabrilla-dark.css` usando las variables de la paleta.

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

### Publicar una versión

Versionado semántico con tags `vMAJOR.MINOR.PATCH` (opcionalmente pre-release: `v1.0.0-beta.1`, `v1.0.0-rc.1`).
La versión **solo** se define en el tag: el pom usa `${revision}` (`0.0.0-SNAPSHOT` en local).

```bash
git tag v1.0.0-rc.1
git push origin v1.0.0-rc.1
```

El workflow [.github/workflows/release.yml](.github/workflows/release.yml) (Windows):
1. valida que el tag sea semver,
2. compila, corre los tests (sin los de UI) y genera la app con jpackage (`-Pportable -Drevision=<versión>`),
3. la comprime en `kabrilla-<versión>-win-portable.zip` (~105 MB),
4. crea el Release en GitHub con el ZIP. Notas: el contenido de `docs/releases/<tag>.md` si existe (escríbelo antes
   de crear el tag) más las generadas desde commits/PRs. Los tags con `-` (beta, rc)
   se publican como **pre-release**.

Si el workflow falla, se puede borrar el tag (`git push --delete origin vX.Y.Z` y `git tag -d vX.Y.Z`), corregir
y volver a crearlo.
