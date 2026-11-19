package co.com.bancolombia.kabrilla.ui.services;

/**
 * Texto corto de un error para la UI. El detalle completo (traza) siempre va al log con {@code log.error}.
 */
public final class ErrorMessages {

    private ErrorMessages() {
    }

    /**
     * Mensaje de la excepción y, si es distinta, el de su causa raíz: el envoltorio dice qué se intentaba
     * ("Error listando deployments en ns=x") y la raíz por qué falló ("ExpiredToken: ...").
     */
    public static String describe(Throwable error) {
        if (error == null) {
            return "Error desconocido";
        }
        String top = messageOf(error);
        Throwable root = rootCause(error);
        if (root == error) {
            return top;
        }
        String rootMessage = messageOf(root);
        return top.contains(rootMessage) ? top : top + " → " + rootMessage;
    }

    public static Throwable rootCause(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private static String messageOf(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
