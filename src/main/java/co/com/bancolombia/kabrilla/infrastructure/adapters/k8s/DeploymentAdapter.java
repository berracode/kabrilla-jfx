package co.com.bancolombia.kabrilla.infrastructure.adapters.k8s;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.DeploymentRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.Deployment;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.infrastructure.adapters.EksTokenProvider;
import co.com.bancolombia.kabrilla.infrastructure.adapters.OkHttpClientFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentAdapter implements DeploymentRepository {

    @Override
    public List<Deployment> findAll(KubeConfig kubeConfig, AwsCredentials credentials, String namespace) {
        try {
            List<Deployment> deployments = new ArrayList<>();
            var token = EksTokenProvider.getEksToken(credentials);
            log.info("TOKEN: " + token);
            ApiClient client = new ApiClient();
            client.setBasePath(kubeConfig.getServerUrl());
            //((ApiKeyAuth) client.getAuthentication("BearerToken")).setApiKey(token);
            client.setApiKey(token);
            client.setApiKeyPrefix("Bearer");


            client.setVerifyingSsl(true); //si lo pongo true debo descomentar la linea de abajo
            //client.setHttpClient(createSecureClient(decodedCa));
            client.setHttpClient(OkHttpClientFactory.createSecureClient(kubeConfig.getCaCertBytes()));
            Configuration.setDefaultApiClient(client);

            //aqui puedo usar el client

// Crear instancia de la API de Deployments
            AppsV1Api appsV1Api = new AppsV1Api();

            // Namespace objetivo

            try {
                V1DeploymentList deploymentList = appsV1Api.listNamespacedDeployment(namespace).execute();

                for (V1Deployment deployment : deploymentList.getItems()) {
                    var deploymentItem = Deployment.builder().clusterName(kubeConfig.getClusterName())
                            .namespace(namespace).name(deployment.getMetadata().getName()).build();
                    deployments.add(deploymentItem);
                    log.info("📦 Deployment: {}" + deploymentItem.getName());
                }

                return deployments;


            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
