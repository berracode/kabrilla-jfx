package co.com.bancolombia.kabrilla.services;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class KubeService {
    private static final String SETTINGS_FILE = "settings.yml";

    public String loadKubePath() {
        try {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                Yaml yaml = new Yaml();
                try (FileReader reader = new FileReader(file)) {
                    Map<String, Object> data = yaml.load(reader);
                    if (data != null && data.get("kubePath") != null) {
                        return data.get("kubePath").toString();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void saveKubePath(String kubePath) {
        if (!kubePath.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("kubePath", kubePath);

            try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                options.setPrettyFlow(true);

                Yaml yaml = new Yaml(options);
                yaml.dump(data, writer);
                System.out.println("KubePath guardado en formato limpio en settings.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

