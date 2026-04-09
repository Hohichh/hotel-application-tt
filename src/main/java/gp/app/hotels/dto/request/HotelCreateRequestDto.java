package gp.app.hotels.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HotelCreateRequestDto(
    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    String name,

    @Size(max = 2000, message = "description must be at most 2000 characters")
    String description,

    @NotBlank(message = "brand is required")
    @Size(max = 255, message = "brand must be at most 255 characters")
    String brand,

    @NotNull(message = "address is required")
    @Valid
    AddressRequestDto address,

    @NotNull(message = "contacts is required")
    @Valid
    ContactsRequestDto contacts,

    @NotNull(message = "arrivalTime is required")
    @Valid
    ArrivalTimeRequestDto arrivalTime
) {}
