package co.com.andesexpress.model;

import java.util.Set;

public final class ZoneClassifier {

    private ZoneClassifier() {
    }

    public static Zone classify(
            Location origin,
            Location destination,
            Set<Integer> neighborsOfOriginDepartment) {

        if (origin.cityId() == destination.cityId()) {
            return Zone.SAME_CITY;
        }

        if (origin.departmentId() == destination.departmentId()) {
            return Zone.SAME_DEPARTMENT;
        }

           if (neighborsOfOriginDepartment.contains(
                destination.departmentId())) {

            return Zone.NEIGHBORING_DEPARTMENT;
        }

        return Zone.REST_OF_COUNTRY;
    }
}