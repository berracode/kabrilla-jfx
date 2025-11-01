package co.com.bancolombia.kabrilla.domain.model;

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
public class KubeConfig {
    private String serverUrl;
    private String clusterName;
    private byte[] caCertBytes;
}
