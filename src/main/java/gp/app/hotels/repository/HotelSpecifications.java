package gp.app.hotels.repository;

import gp.app.hotels.model.Amenity;
import gp.app.hotels.model.Hotel;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class HotelSpecifications {

    public static Specification<Hotel> hasName(String name) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(name)) {
                return null;
            }
            return cb.equal(root.get("name"), name);
        };
    }

    public static Specification<Hotel> hasBrand(String brand) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(brand)) {
                return null;
            }
            return cb.equal(root.get("brand"), brand);
        };
    }

    public static Specification<Hotel> hasCity(String city) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(city)) {
                return null;
            }
            return cb.equal(root.get("address").get("city"), city);
        };
    }

    public static Specification<Hotel> hasCountry(String country) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(country)) {
                return null;
            }
            return cb.equal(root.get("address").get("country"), country);
        };
    }

    public static Specification<Hotel> hasAmenity(String amenityName) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(amenityName)) {
                return null;
            }
            query.distinct(true); // важный момент, избежать дублирования
            Join<Hotel, Amenity> amenitiesJoin = root.join("amenities");
            return cb.equal(amenitiesJoin.get("name"), amenityName);
        };
    }
}
