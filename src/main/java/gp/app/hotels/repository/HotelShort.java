package gp.app.hotels.repository;

import org.springframework.beans.factory.annotation.Value;
import java.util.UUID;

public interface HotelShort {
    UUID getId();
    
    String getName();
    
    String getDescription();
    
    // SpEL-выражение для сборки адреса в одну строку
    @Value("#{target.address.houseNumber + ' ' + target.address.street + ', ' + target.address.city + ', ' + target.address.postCode + ', ' + target.address.country}")
    String getAddress();
    
    @Value("#{target.contacts.phone}")
    String getPhone();
}
