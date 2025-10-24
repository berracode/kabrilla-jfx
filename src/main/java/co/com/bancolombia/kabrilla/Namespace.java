package co.com.bancolombia.kabrilla;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Namespace {
    private String name;
    private List<String> pods;

    public Namespace() {}

    public Namespace(String name, List<String> pods) {
        this.name = name;
        this.pods = pods;
    }

    public String getName() {
        return name;
    }

    public List<String> getPods() {
        return pods;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPods(List<String> pods) {
        this.pods = pods;
    }
}

