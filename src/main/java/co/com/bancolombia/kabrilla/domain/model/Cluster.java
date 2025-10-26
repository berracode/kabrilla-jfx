package co.com.bancolombia.kabrilla.domain.model;

import java.util.ArrayList;
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
public class Cluster {
    private String name;
    private List<Namespace> namespaces = new ArrayList<>();

    public Cluster(String name) {
        this.name = name;
    }

    public void addNamespace(Namespace ns) {
        if (namespaces != null && !namespaces.contains(ns)) {
            namespaces.add(ns);
        }
    }

}
