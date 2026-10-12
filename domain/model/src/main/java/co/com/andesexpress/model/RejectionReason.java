package co.com.andesexpress.model;

/** Motivos de rechazo de negocio (no son errores tecnicos). */
public enum RejectionReason {
    ORIGIN_CITY_NOT_FOUND,
    ORIGIN_CITY_NOT_IN_DEPARTMENT,
    DESTINATION_CITY_NOT_FOUND,
    DESTINATION_CITY_NOT_IN_DEPARTMENT
}
