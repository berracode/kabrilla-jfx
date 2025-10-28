package co.com.bancolombia.kabrilla.domain.gateway;

import java.util.Optional;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;

public interface CredentialRepository {

    void save(AwsCredentials credentials);

    Optional<AwsCredentials> findByClusterName(String clusterName);

}
