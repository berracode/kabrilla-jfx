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
public class AwsCredentials {
    private String clusterName;
    private String accessKey;
    private String secretKey;
    private String sessionToken;
}
