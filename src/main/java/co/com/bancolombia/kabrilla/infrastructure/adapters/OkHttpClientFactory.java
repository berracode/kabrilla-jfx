package co.com.bancolombia.kabrilla.infrastructure.adapters;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import co.com.bancolombia.kabrilla.infrastructure.filters.LoggingInterceptor;

import okhttp3.OkHttpClient;

public class OkHttpClientFactory {

    public static OkHttpClient createSecureClient(byte[] caBytes) throws Exception {
        return createSecureClientInternal(caBytes);
    }

    private static OkHttpClient createSecureClientInternal(byte[] caBytes) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate caCert = cf.generateCertificate(new ByteArrayInputStream(caBytes));

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        return new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .addInterceptor(new LoggingInterceptor())
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                .build();
    }
}
