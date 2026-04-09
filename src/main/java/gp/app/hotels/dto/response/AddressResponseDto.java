package gp.app.hotels.dto.response;

public record AddressResponseDto(
    String houseNumber,
    String street,
    String city,
    String country,
    String postCode
) {}
