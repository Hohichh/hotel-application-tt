package gp.app.hotels.dto.request;

public record HotelCreateRequestDto(
    String name,
    String description,
    String brand,
    AddressRequestDto address,
    ContactsRequestDto contacts,
    ArrivalTimeRequestDto arrivalTime
) {}
