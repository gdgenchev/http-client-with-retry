package org.httpclient;

import java.io.Serial;

public class ExternalServiceUnavailableException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExternalServiceUnavailableException(Throwable cause) {
        super(cause);
    }
}
