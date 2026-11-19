package co.com.bancolombia.kabrilla.infrastructure.config.k8s;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;

import io.kubernetes.client.openapi.ApiClient;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

class KubeClientFactoryTest {

    private static final KubeConfig CLUSTER = KubeConfig.builder()
            .clusterName("arn:aws:eks:us-east-1:123:cluster/qa")
            .serverUrl("https://qa.eks.example")
            .caCertBytes(new byte[]{1, 2, 3})
            .build();

    // Access key de las credenciales con las que se pidió cada token
    private final List<String> tokensIssuedFor = new ArrayList<>();
    private final MutableClock clock = new MutableClock();
    private int nextHttpCode = 200;

    private final KubeClientFactory factory = new KubeClientFactory(
            creds -> {
                if ("BAD".equals(creds.getAccessKey())) {
                    throw new IOException("AWS CLI no devolvió token: ExpiredToken");
                }
                tokensIssuedFor.add(creds.getAccessKey());
                return "token-" + tokensIssuedFor.size();
            },
            ca -> new OkHttpClient.Builder()
                    .addInterceptor(chain -> new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(nextHttpCode)
                            .message("fake")
                            .body(ResponseBody.create("", null))
                            .build())
                    .build(),
            clock);

    @Test
    void reusesClientAndTokenWhileCredentialsAndTokenAreValid() {
        ApiClient first = factory.forCluster(CLUSTER, creds("AKIA1"));
        ApiClient second = factory.forCluster(CLUSTER, creds("AKIA1"));

        assertSame(first, second);
        assertEquals(List.of("AKIA1"), tokensIssuedFor);
    }

    @Test
    void renewedCredentialsGetNewTokenImmediately() {
        ApiClient expired = factory.forCluster(CLUSTER, creds("AKIA-OLD"));

        ApiClient renewed = factory.forCluster(CLUSTER, creds("AKIA-NEW"));

        assertNotSame(expired, renewed);
        assertEquals(List.of("AKIA-OLD", "AKIA-NEW"), tokensIssuedFor);
    }

    @Test
    void unauthorizedResponseForcesNewTokenOnNextCall() throws IOException {
        ApiClient client = factory.forCluster(CLUSTER, creds("AKIA1"));
        nextHttpCode = 401;
        call(client);

        nextHttpCode = 200;
        ApiClient again = factory.forCluster(CLUSTER, creds("AKIA1"));

        assertSame(client, again);
        assertEquals(List.of("AKIA1", "AKIA1"), tokensIssuedFor);
    }

    @Test
    void successfulResponseKeepsToken() throws IOException {
        ApiClient client = factory.forCluster(CLUSTER, creds("AKIA1"));
        call(client);

        factory.forCluster(CLUSTER, creds("AKIA1"));

        assertEquals(List.of("AKIA1"), tokensIssuedFor);
    }

    @Test
    void renewsTokenWhenTtlExpires() {
        factory.forCluster(CLUSTER, creds("AKIA1"));
        clock.advance(KubeClientFactory.TOKEN_TTL.plusSeconds(1));

        factory.forCluster(CLUSTER, creds("AKIA1"));

        assertEquals(List.of("AKIA1", "AKIA1"), tokensIssuedFor);
    }

    @Test
    void failedTokenDoesNotBlockLaterCredentials() {
        var error = assertThrows(IllegalStateException.class, () -> factory.forCluster(CLUSTER, creds("BAD")));
        assertEquals(true, error.getMessage().contains("ExpiredToken"), error.getMessage());

        factory.forCluster(CLUSTER, creds("AKIA-NEW"));

        assertEquals(List.of("AKIA-NEW"), tokensIssuedFor);
    }

    @Test
    void fingerprintDetectsAnyCredentialChangeWithoutExposingSecrets() {
        String base = KubeClientFactory.fingerprint(creds("AKIA1"));
        AwsCredentials otherSession = creds("AKIA1").toBuilder().sessionToken("otro").build();

        assertEquals(base, KubeClientFactory.fingerprint(creds("AKIA1")));
        assertEquals(false, base.equals(KubeClientFactory.fingerprint(otherSession)));
        assertEquals(false, base.contains("AKIA1"));
    }

    private static AwsCredentials creds(String accessKey) {
        return AwsCredentials.builder()
                .clusterName("qa")
                .accessKey(accessKey)
                .secretKey("secret")
                .sessionToken("session")
                .build();
    }

    private static void call(ApiClient client) throws IOException {
        client.getHttpClient().newCall(new Request.Builder().url("https://qa.eks.example/api").build())
                .execute().close();
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-19T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
