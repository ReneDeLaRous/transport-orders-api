package com.transportes.api.order;

public enum OrderStatus {
    CREATED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED;

    /**
     * Flujo permitido:
     * CREATED -> IN_TRANSIT | CANCELLED
     * IN_TRANSIT -> DELIVERED | CANCELLED
     * DELIVERED y CANCELLED son estados finales.
     */
    public boolean canMoveTo(OrderStatus target) {
        switch (this) {
            case CREATED:
                return target == IN_TRANSIT || target == CANCELLED;
            case IN_TRANSIT:
                return target == DELIVERED || target == CANCELLED;
            default:
                return false;
        }
    }
}
