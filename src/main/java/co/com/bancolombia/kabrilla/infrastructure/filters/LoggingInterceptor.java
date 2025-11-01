package co.com.bancolombia.kabrilla.infrastructure.filters;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class LoggingInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // Log detalles de la petición
        System.out.println("➡️ REQUEST");
        System.out.println("URL: " + request.url());
        System.out.println("Method: " + request.method());
        System.out.println("Headers:\n" + request.headers());

        // Ejecutar la petición
        Response response = chain.proceed(request);

        // Log detalles de la respuesta
        System.out.println("⬅️ RESPONSE");
        System.out.println("Code: " + response.code());
        System.out.println("Message: " + response.message());
        System.out.println("Headers:\n" + response.headers());

        return response;
    }
}
