package gp.app.hotels.repository;

import gp.app.hotels.model.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
    
    // Найти все существующие удобства по списку имен
    Set<Amenity> findAllByNameIn(List<String> names);
    
    Optional<Amenity> findByName(String name);
}
