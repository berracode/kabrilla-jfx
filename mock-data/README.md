# mock-data — datos simulados para el perfil `mock`

Permite usar Kabrilla sin AWS ni cluster. Todo es falso y se puede versionar.

```bash
./mvnw javafx:run -Pmock
```

## Estructura

```
mock-data/
├── kubeconfig.yaml      # reemplaza ~/.kube/config (clusters con nombre ARN .../cluster/<nombre>)
├── clusters.json        # namespaces registrados por cluster (lo que muestra el sidebar)
├── credentials.json     # credenciales AWS falsas (la UI exige que existan por cluster)
├── generar-logs.sh      # genera archivos de logs sintéticos
├── logs/
│   └── <cluster>/<namespace>/<deployment>/<pod>.log|.txt
└── resources/
    └── <cluster>/<namespace>/*.yaml   # manifiestos: Deployment, Service, HPA, ConfigMap, políticas Istio
```

- **Deployment** = cada carpeta dentro de `logs/<cluster>/<namespace>/`.
- **Pod** = cada archivo `.log` o `.txt` dentro de la carpeta del deployment (nombre sin extensión). Cada pod
  simula Istio con dos containers: `<deployment>` (preseleccionado) e `istio-proxy`. El mock muestra el mismo
  archivo para ambos containers; sirve para probar el selector, no el contenido.
- **Logs en vivo** = el contenido del archivo. Se reproduce línea a línea (`kabrilla.mock.line-delay-ms`, 100 ms por
  defecto) y después queda "siguiendo" el archivo: lo que agregues aparece en la app, como `kubectl logs -f`.
- **Logs por rango** (selector *Rango*) = el archivo hace de "lo que conserva Kubernetes": la última línea tiene la
  hora en que arrancó la app y cada línea anterior `kabrilla.mock.history-step-ms` (2 s) antes. Un archivo de ~450
  líneas cubre ~15 min, así que *Última hora* muestra el aviso de cobertura incompleta.
- **Container anterior** = `<pod>.previous.log` junto al archivo del pod (no aparece como pod aparte). Incluido:
  `payments-api-66f8d9c7b-aa01b.previous.log`, que termina en `OutOfMemoryError`. Sin ese archivo, la consulta
  falla como en Kubernetes ("previous terminated container ... not found").

- **Recursos** (pestañas HPA, ConfigMap, Service, AuthorizationPolicy, RequestAuthentication) = manifiestos YAML
  en `resources/<cluster>/<namespace>/`, en el mismo formato de `kubectl get <recurso> -o yaml` (un archivo puede
  traer varios documentos separados por `---`). La asociación con el deployment usa la misma lógica que contra
  EKS, así que **cada deployment necesita su manifiesto `Deployment`** (labels del pod template y ConfigMaps
  referenciados):
  - Service: su `spec.selector` coincide con los labels del pod template.
  - HPA: `spec.scaleTargetRef` apunta al deployment (no necesita el manifiesto Deployment).
  - ConfigMap: referenciado en `volumes`, `envFrom` o `env.valueFrom` del pod template.
  - AuthorizationPolicy / RequestAuthentication: `spec.selector.matchLabels` coincide, o no tiene selector
    (aplica a todo el namespace).

  Para copiar recursos reales al mock (en el PC con acceso, **quitando datos sensibles**):
  `kubectl get deploy,svc,hpa,cm -n <ns> -o yaml > resources/<cluster>/<ns>/real.yaml`

`<cluster>` es el nombre corto (lo que va después de `cluster/` en el kubeconfig) y debe coincidir en los 4 lugares:
`kubeconfig.yaml`, `clusters.json`, `credentials.json` y la carpeta `logs/<cluster>`.

## Casos incluidos

| Cluster / namespace | Qué prueba |
|---|---|
| `mock-qa / ventas-qa` | `orders-api` con 2 pods (texto, stack traces) y todos los recursos (HPA con CPU y memoria, 3 ConfigMaps uno con archivo YAML, políticas Istio); `catalog-api` con logs JSON, Service LoadBalancer y sin HPA. `namespace.yaml` trae recursos que **no** deben aparecer (Service y ConfigMap de otros) y una política para todo el namespace |
| `mock-qa / pagos-qa` | `payments-api` termina con un `OutOfMemoryError`; `payments-worker` usa `.txt` y su HPA escala por métrica externa |
| `mock-qa / vacio-qa` | namespace sin deployments |
| `mock-dev / ventas-dev` | segundo cluster |

## Recetas

```bash
cd mock-data
# Archivo grande para probar volumen
./generar-logs.sh logs/mock-qa/ventas-qa/orders-api/orders-api-7d9f8b6c4-big01.log 50000

# Tráfico en vivo sobre un pod mientras la app está abierta
while true; do ./generar-logs.sh /dev/stdout 1 >> logs/mock-qa/ventas-qa/orders-api/orders-api-7d9f8b6c4-k2x9p.log; sleep 1; done
```

Para agregar un namespace: crea la carpeta en `logs/<cluster>/` (y en `resources/<cluster>/` si quieres recursos)
y regístralo en `clusters.json`
(o con el botón "crear namespace" de la app, que en modo mock escribe en este `clusters.json`).
