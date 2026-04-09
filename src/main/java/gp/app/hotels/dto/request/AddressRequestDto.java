package gp.app.hotels.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequestDto(
    @NotBlank(message = "houseNumber is required")
    @Size(max = 50, message = "houseNumber must be at most 50 characters")
    @Pattern(
        regexp = "^\\d{1,5}(?:\\s?[a-zA-Zа-яА-ЯёЁ])?(?:\\s?[\\/\\-]?\\s?\\d{1,3}[a-zA-Zа-яА-ЯёЁ]?)?$",
        message = "houseNumber has invalid format"
    )
    String houseNumber,

    @NotBlank(message = "street is required")
    @Size(max = 255, message = "street must be at most 255 characters")
    String street,

    @NotBlank(message = "city is required")
    @Size(max = 255, message = "city must be at most 255 characters")
    String city,

    @NotBlank(message = "country is required")
    @Size(max = 255, message = "country must be at most 255 characters")
    String country,

    @NotBlank(message = "postCode is required")
    @Size(max = 50, message = "postCode must be at most 50 characters")
    String postCode
) {}
