package co.com.bancolombia.kabrilla.infrastructure.filters;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Traza de llamadas a la API de Kubernetes en nivel DEBUG.
 * Nunca registra headers: llevan el token Bearer de EKS.
 */
@Slf4j
public class LoggingInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long start = System.nanoTime();
        Response response = chain.proceed(request);
        if (log.isDebugEnabled()) {
            // Solo ruta (sin query): puede incluir parámetros que no aportan al diagnóstico
            log.debug("K8s {} {} -> {} ({} ms)", request.method(), request.url().encodedPath(), response.code(),
                      (System.nanoTime() - start) / 1_000_000);
        }
        return response;
    }
}
