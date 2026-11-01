package co.com.bancolombia.kabrilla.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.com.bancolombia.kabrilla.ui.viewmodel.credentials.CredentialsItemViewModel;

import org.junit.jupiter.api.Test;

/**
 * Los secretos no deben salir en toString: cualquier log de estos objetos terminaría en la consola de la app.
 */
class AwsCredentialsTest {

    @Test
    void toStringNeverContainsSecrets() {
        String domain = new AwsCredentials("eks-qa", "ASIAEXAMPLEKEY", "secret-value", "session-token").toString();
        String ui = new CredentialsItemViewModel("eks-qa", "ASIAEXAMPLEKEY", "secret-value", "session-token")
                .toString();

        for (String text : new String[]{domain, ui}) {
            assertTrue(text.contains("eks-qa"), text);
            assertFalse(text.contains("ASIAEXAMPLEKEY"), text);
            assertFalse(text.contains("secret-value"), text);
            assertFalse(text.contains("session-token"), text);
        }
    }
}
