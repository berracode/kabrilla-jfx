package co.com.bancolombia.kabrilla.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class AwsCredentials {
    private String clusterName;
    // Secretos: nunca en toString (y por tanto nunca en logs)
    @ToString.Exclude
    private String accessKey;
    @ToString.Exclude
    private String secretKey;
    @ToString.Exclude
    private String sessionToken;
}
