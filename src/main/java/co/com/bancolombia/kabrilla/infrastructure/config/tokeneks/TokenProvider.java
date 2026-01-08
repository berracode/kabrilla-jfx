package co.com.bancolombia.kabrilla.infrastructure.config.tokeneks;

import java.io.IOException;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;

@FunctionalInterface
public interface TokenProvider {
    String getToken(AwsCredentials creds) throws IOException;
}
