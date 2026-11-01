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
└── logs/
    └── <cluster>/<namespace>/<deployment>/<pod>.log|.txt
```

- **Deployment** = cada carpeta dentro de `logs/<cluster>/<namespace>/`.
- **Pod** = cada archivo `.log` o `.txt` dentro de la carpeta del deployment (nombre sin extensión).
- **Logs** = el contenido del archivo. Se reproduce línea a línea (`kabrilla.mock.line-delay-ms`, 100 ms por defecto)
  y después queda "siguiendo" el archivo: lo que agregues aparece en la app, como `kubectl logs -f`.

`<cluster>` es el nombre corto (lo que va después de `cluster/` en el kubeconfig) y debe coincidir en los 4 lugares:
`kubeconfig.yaml`, `clusters.json`, `credentials.json` y la carpeta `logs/<cluster>`.

## Casos incluidos

| Cluster / namespace | Qué prueba |
|---|---|
| `mock-qa / ventas-qa` | `orders-api` con 2 pods (texto, stack traces), `catalog-api` con logs JSON |
| `mock-qa / pagos-qa` | `payments-api` termina con un `OutOfMemoryError`; `payments-worker` usa `.txt` |
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

Para agregar un namespace: crea la carpeta en `logs/<cluster>/` y regístralo en `clusters.json`
(o con el botón "crear namespace" de la app, que en modo mock escribe en este `clusters.json`).
