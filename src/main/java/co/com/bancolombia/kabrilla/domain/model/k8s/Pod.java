package co.com.bancolombia.kabrilla.domain.model.k8s;

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
public class Pod {
    private String name;
    private String status;
    private int restarts;
    private String age;
    private String node;
    // Containers de la app (sin initContainers), en el orden del spec
    @Builder.Default
    private List<String> containers = List.of();
    // Container cuyos logs se muestran por defecto (ver ContainerSelector)
    private String defaultContainer;
}
