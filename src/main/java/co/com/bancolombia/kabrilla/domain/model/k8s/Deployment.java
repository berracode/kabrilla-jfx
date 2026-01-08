package co.com.bancolombia.kabrilla.domain.model.k8s;

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
public class Deployment {
    private String clusterName;
    private String namespace;
    private String name;
}
