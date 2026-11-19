package co.com.bancolombia.kabrilla.domain.model.k8s.resources;

/**
 * Recursos de Kubernetes/Istio que se muestran asociados a un deployment.
 */
public enum ResourceKind {
    SERVICE("Service"),
    HPA("HorizontalPodAutoscaler"),
    CONFIG_MAP("ConfigMap"),
    AUTHORIZATION_POLICY("AuthorizationPolicy"),
    REQUEST_AUTHENTICATION("RequestAuthentication"),
    // Istio networking: se asocian vía Services del deployment (VirtualService) y sus spec.gateways (Gateway)
    VIRTUAL_SERVICE("VirtualService"),
    GATEWAY("Gateway");

    private final String k8sKind;

    ResourceKind(String k8sKind) {
        this.k8sKind = k8sKind;
    }

    /**
     * Valor del campo {@code kind} en el manifiesto.
     */
    public String k8sKind() {
        return k8sKind;
    }
}
