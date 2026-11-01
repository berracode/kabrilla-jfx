#!/usr/bin/env bash
# Genera un archivo de logs sintético para el perfil mock (corre en Git Bash).
# Uso:   ./generar-logs.sh <archivo-destino> <lineas> [app] [text|json] [seed] [crash 0|1]
# Ej.:   ./generar-logs.sh logs/mock-qa/ventas-qa/orders-api/orders-api-7d9f8b6c4-big01.log 50000
# Para simular tráfico en vivo, con la app abierta:
#        while true; do ./generar-logs.sh /dev/stdout 1 >> logs/mock-qa/ventas-qa/orders-api/<pod>.log; sleep 1; done
set -euo pipefail
dest="$1"; n="$2"; app="${3:-orders}"; style="${4:-text}"; seed="${5:-$RANDOM}"; crash="${6:-0}"
[ "$dest" != "/dev/stdout" ] && mkdir -p "$(dirname "$dest")"
awk -v seed="$seed" -v n="$n" -v app="$app" -v style="$style" -v crash="$crash" '
BEGIN {
  srand(seed);
  split("GET /api/v1/orders|POST /api/v1/orders|GET /api/v1/orders/{id}|GET /actuator/health|PUT /api/v1/orders/{id}/status", paths, "|");
  split("OrderController|OrderService|InventoryClient|KafkaProducer|HikariPool", classes, "|");
  t = 1761900000; # base epoch
  for (i = 1; i <= n; i++) {
    t += int(rand() * 3);
    ts = strftime("%Y-%m-%dT%H:%M:%S", t, 1) sprintf(".%03dZ", int(rand()*1000));
    r = rand(); lvl = (r < 0.80) ? "INFO" : (r < 0.93) ? "WARN" : "ERROR";
    trace = sprintf("%08x", int(rand()*4294967295));
    cls = classes[int(rand()*5)+1]; p = paths[int(rand()*5)+1]; ms = int(rand()*900)+5;
    if (lvl == "INFO")  msg = sprintf("%s -> 200 en %d ms", p, ms);
    if (lvl == "WARN")  msg = sprintf("Latencia alta en %s: %d ms (umbral 500 ms)", p, ms+500);
    if (lvl == "ERROR") msg = sprintf("Fallo procesando %s: timeout llamando a %s-svc", p, tolower(cls));
    if (style == "json") {
      printf("{\"timestamp\":\"%s\",\"level\":\"%s\",\"logger\":\"co.com.%s.%s\",\"trace_id\":\"%s\",\"message\":\"%s\"}\n", ts, lvl, app, cls, trace, msg);
    } else {
      printf("%s %-5s [%s,%s] --- [nio-8080-exec-%d] c.c.%s.%s : %s\n", ts, lvl, app, trace, int(rand()*9)+1, app, cls, msg);
      if (lvl == "ERROR" && rand() < 0.5) {
        print "java.net.SocketTimeoutException: Read timed out";
        printf("\tat co.com.%s.client.%s.call(%s.java:%d)\n", app, cls, cls, int(rand()*200)+10);
        printf("\tat co.com.%s.service.OrderService.process(OrderService.java:%d)\n", app, int(rand()*200)+10);
        print "\tat java.base/java.lang.Thread.run(Thread.java:1583)";
      }
    }
  }
  if (crash) {
    printf("%s ERROR [%s,deadbeef] --- [main] o.s.boot.SpringApplication : Application run failed\n", ts, app);
    print "java.lang.OutOfMemoryError: Java heap space";
    print "\tat java.base/java.util.Arrays.copyOf(Arrays.java:3541)";
  }
}
' > "$dest"
