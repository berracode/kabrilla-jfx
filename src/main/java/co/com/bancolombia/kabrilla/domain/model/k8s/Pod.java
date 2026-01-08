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
public class Pod {
    private String name;
    private String status;
    private int restarts;
    private String age;
    private String node;
}
