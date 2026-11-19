package co.com.bancolombia.kabrilla.ui.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class ErrorMessagesTest {

    @Test
    void showsWhatFailedAndWhy() {
        var error = new RuntimeException("Error listando deployments en ns=pagos-qa",
                                         new IOException("AWS CLI no devolvió token: ExpiredToken"));

        assertEquals("Error listando deployments en ns=pagos-qa → AWS CLI no devolvió token: ExpiredToken",
                     ErrorMessages.describe(error));
    }

    @Test
    void doesNotRepeatCauseAlreadyInMessage() {
        var cause = new IOException("HTTP 401");
        var error = new RuntimeException(cause); // mensaje = cause.toString()

        assertEquals("java.io.IOException: HTTP 401", ErrorMessages.describe(error));
    }

    @Test
    void usesClassNameWhenThereIsNoMessage() {
        assertEquals("NullPointerException", ErrorMessages.describe(new NullPointerException()));
    }
}
