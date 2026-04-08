package gp.app.hotels.dto.response;

import java.util.UUID;

public record HotelShortResponseDto(
    UUID id,
    String name,
    String description,
    String address,
    String phone
) {}
