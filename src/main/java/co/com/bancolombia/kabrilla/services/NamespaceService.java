package co.com.bancolombia.kabrilla.services;

import co.com.bancolombia.kabrilla.Namespace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.util.*;

public class NamespaceService {
    private static final String NAMESPACES_FILE = "namespaces.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KubeService kubeService = new KubeService();
    private static final ObservableList<Namespace> namespacesObservable = FXCollections.observableArrayList();
    private static final NamespaceService INSTANCE = new NamespaceService();

    public static NamespaceService getInstance() {
        return INSTANCE;
    }

    // 🔹 Constructor privado para evitar múltiples instancias
    private NamespaceService() {
        System.out.println("1. al iniciar...............");
        var savedNamespaces = this.loadNamespaces();
        System.out.println("guarda en el array observable");
        namespacesObservable.addAll(savedNamespaces);
    }

    public ObservableList<Namespace> getNamespaces() {
        System.out.println("entra aqui a obtener la lisat observable "+namespacesObservable.hashCode());
        return namespacesObservable;
    }

    public List<Namespace> loadNamespaces() {
        System.out.println("2. cargando todos los ns del archivo"); //porque no hacemos append

        File file = new File(NAMESPACES_FILE);
        if (!file.exists()) return new ArrayList<>();

        try {
            List<Namespace> namespaces = objectMapper.readValue(file, new TypeReference<List<Namespace>>() {});
            return namespaces != null ? namespaces : new ArrayList<>();
        } catch (IOException e) {
            System.out.println("Error: "+e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    public void saveNamespace(Namespace namespace) {
        List<Namespace> namespaces = loadNamespaces();

        if (namespaces == null) {
            namespaces = new ArrayList<>();
        }
        System.out.println("3. guardando en el array local namespaces");
        namespaces.add(namespace);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(NAMESPACES_FILE), namespaces);
            System.out.println("4. Namespace guardado en namespaces.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!namespacesObservable.contains(namespace)) {
            System.out.println("5. guarda en el array observabale: "+namespacesObservable.hashCode());
            namespacesObservable.add(namespace);
        }

    }


    public List<String> getPods(String namespace) {
        boolean entre = false;
        if(entre){
            String kubePath = kubeService.loadKubePath();
            if (kubePath.isEmpty()) return new ArrayList<>();

            List<String> pods = new ArrayList<>();
            try {
                ProcessBuilder builder = new ProcessBuilder(
                        kubePath, "get", "pods", "-n", namespace, "--no-headers", "-o", "custom-columns=:metadata.name"
                );
                builder.redirectErrorStream(true);
                Process process = builder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isBlank()) pods.add(line.trim());
                    }
                }

                if (process.waitFor() != 0) {
                    System.out.println("Error al obtener pods para namespace: " + namespace);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return pods;
        } else {
            try {
                System.out.println("Buscando pods para el ns: "+namespace+ "hilo: "+Thread.currentThread().threadId());

                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return Arrays.asList("Pod-1", "Pod-2");
        }

    }
}

