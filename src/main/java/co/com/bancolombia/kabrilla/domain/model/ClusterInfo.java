package co.com.bancolombia.kabrilla.domain.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ClusterInfo {
    private List<Cluster> clustersWithNamespace;
    private KubeConfigModel kubeConfigModel;
}
