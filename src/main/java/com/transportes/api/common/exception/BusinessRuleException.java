package com.transportes.api.common.exception;

/**
 * Se lanza cuando la petición es válida pero choca con una regla de negocio
 * (por ejemplo, un cambio de estado que no está permitido).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
