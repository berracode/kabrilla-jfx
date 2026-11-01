package co.com.bancolombia.kabrilla.infrastructure.config.k8s;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import co.com.bancolombia.kabrilla.domain.gateway.KubeconfigRepository;
import co.com.bancolombia.kabrilla.domain.model.KubeConfigModel;

import io.kubernetes.client.util.KubeConfig;

public class KubeconfigAdapter implements KubeconfigRepository {

    private final String kubeConfigPath;

    public KubeconfigAdapter() {
        this(System.getProperty("user.home") + "/.kube/config");
    }

    public KubeconfigAdapter(String kubeConfigPath) {
        this.kubeConfigPath = kubeConfigPath;
    }


    @Override
    public KubeConfigModel findAllKubeconfig() {
        FileReader reader = null;
        KubeConfig config;
        try {
            reader = new FileReader(kubeConfigPath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        config = KubeConfig.loadKubeConfig(reader);

        var kubeConfigModel = new KubeConfigModel();
        kubeConfigModel.setCurrentContext(config.getCurrentContext());
        var clusters = new ArrayList<KubeConfigModel.ClusterWrapper>();

        for (Object clusterObj : config.getClusters()) {
            var clusterMap = (LinkedHashMap<String, Object>) clusterObj;
            var clusterData = (LinkedHashMap<String, Object>) clusterMap.get("cluster");
            var server = (String) clusterData.get("server");
            var caBase64 = (String) clusterData.get("certificate-authority-data");

            var cluster = KubeConfigModel.Cluster.builder().server(server).certificateAuthorityData(caBase64).build();
            var clusterWrapper = new KubeConfigModel.ClusterWrapper();
            clusterWrapper.setName((String) clusterMap.get("name"));
            clusterWrapper.setCluster(cluster);
            clusters.add(clusterWrapper);
        }

        var contexts = new ArrayList<KubeConfigModel.ContextWrapper>();
        for (Object contextObj : config.getContexts()) {
            var contextMap = (LinkedHashMap<String, Object>) contextObj;
            var contextData = (LinkedHashMap<String, Object>) contextMap.get("context");
            var contextName = (String) contextMap.get("name");
            var cluster = (String) contextData.get("cluster");
            var user = (String) contextData.get("user");

            var context = KubeConfigModel.Context.builder().cluster(cluster).user(user).build();
            var contextWrapper = new KubeConfigModel.ContextWrapper();
            contextWrapper.setContext(context);
            contextWrapper.setName(contextName);
            contexts.add(contextWrapper);
        }

        var users = new ArrayList<KubeConfigModel.UserWrapper>();
        for (Object userObj : config.getUsers()) {
            var userMap = (LinkedHashMap<String, Object>) userObj;
            var userData = (LinkedHashMap<String, Object>) userMap.get("user");
            var exec = (LinkedHashMap<String, Object>) userData.get("exec");
            if (exec == null) {
                continue; // usuario con token/certificado en vez de exec: no aplica a EKS
            }
            var apiVersion = (String) exec.get("apiVersion");
            var command = (String) exec.get("command");
            var args = (ArrayList<String>) exec.get("args");
            var userName = (String) userMap.get("name");

            var user = new KubeConfigModel.User(new KubeConfigModel.Exec(apiVersion, command, args));
            var userWrapper = new KubeConfigModel.UserWrapper(userName, user);
            users.add(userWrapper);
        }

        kubeConfigModel.setClusters(clusters);
        kubeConfigModel.setContexts(contexts);
        kubeConfigModel.setUsers(users);
        return kubeConfigModel;
    }
}
