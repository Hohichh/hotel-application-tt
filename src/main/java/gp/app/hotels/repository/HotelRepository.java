package gp.app.hotels.repository;

import gp.app.hotels.model.Hotel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID>, JpaSpecificationExecutor<Hotel> {

    // Для GET /hotels/{id} извлекаем Hotel с инициализированными amenities
    @EntityGraph(attributePaths = {"amenities"})
    Optional<Hotel> findWithAmenitiesById(UUID id);

    // --- Методы для работы эндпоинта /histogram/{param} ---

    @Query("SELECT h.brand AS groupName, COUNT(h) AS count FROM Hotel h GROUP BY h.brand")
    List<HistogramResult> countGroupedByBrand();

    @Query("SELECT h.address.city AS groupName, COUNT(h) AS count FROM Hotel h GROUP BY h.address.city")
    List<HistogramResult> countGroupedByCity();

    @Query("SELECT h.address.country AS groupName, COUNT(h) AS count FROM Hotel h GROUP BY h.address.country")
    List<HistogramResult> countGroupedByCountry();

    // Особая группировка для amenities из-за many-to-many связи
    @Query("SELECT a.name AS groupName, COUNT(h) AS count FROM Hotel h JOIN h.amenities a GROUP BY a.name")
    List<HistogramResult> countGroupedByAmenities();
}
