package co.com.bancolombia.kabrilla.domain.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class KubeConfigModel {
    private Map<String, Object> preferences;
    private String currentContext;
    private List<ClusterWrapper> clusters;
    private List<ContextWrapper> contexts;
    private List<UserWrapper> users;

    @Data
    public static class ClusterWrapper {
        private String name;
        private Cluster cluster;
    }

    @Data
    @Builder
    public static class Cluster {
        private String server;
        private String certificateAuthorityData;
    }

    @Data
    public static class ContextWrapper {
        private String name;
        private Context context;
    }

    @Data
    @Builder
    public static class Context {
        private String cluster;
        private String user;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserWrapper {
        private String name;
        private User user;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User {
        private Exec exec;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Exec {
        private String apiVersion;
        private String command;
        private List<String> args;
    }
}