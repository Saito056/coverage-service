package co.com.andesexpress.model;

public record Location(int departmentId, int cityId) {

    public Location {
        if (departmentId <= 0 || cityId <= 0) {
            throw new IllegalArgumentException(
                    "Los IDs deben ser positivos"
            );
        }
    }
}