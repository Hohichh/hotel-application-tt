package gp.app.hotels.dto.request;

import java.util.List;

public record HotelSearchParams(
    String name,
    String brand,
    String city,
    String country,
    List<String> amenities
) {}
