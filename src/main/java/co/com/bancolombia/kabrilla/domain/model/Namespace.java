package co.com.bancolombia.kabrilla.domain.model;

import java.util.regex.Pattern;

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
public class Namespace {

    // Regla de Kubernetes para nombres de namespace (DNS-1123 label)
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?");
    private static final int MAX_LENGTH = 63;

    private String name;

    /**
     * Motivo por el que el nombre no es válido en Kubernetes, o {@code null} si es válido.
     */
    public static String validateName(String name) {
        if (name == null || name.isBlank()) {
            return "Escribe el nombre del namespace.";
        }
        if (name.length() > MAX_LENGTH) {
            return "El nombre no puede tener más de " + MAX_LENGTH + " caracteres.";
        }
        if (!VALID_NAME.matcher(name).matches()) {
            return "\"" + name + "\" no es un namespace válido: solo minúsculas, números y guiones (-), "
                    + "y debe empezar y terminar con letra o número.";
        }
        return null;
    }
}
