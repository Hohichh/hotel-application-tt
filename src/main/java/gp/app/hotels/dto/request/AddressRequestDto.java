package gp.app.hotels.dto.request;

public record AddressRequestDto(
    Integer houseNumber,
    String street,
    String city,
    String country,
    String postCode
) {}
