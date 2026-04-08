package gp.app.hotels.dto.response;

import java.util.List;
import java.util.UUID;

public record HotelFullResponseDto(
    UUID id,
    String name,
    String description,
    String brand,
    AddressResponseDto address,
    ContactsResponseDto contacts,
    ArrivalTimeResponseDto arrivalTime,
    List<String> amenities
) {}
