package gp.app.hotels.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ArrivalTimeRequestDto(
    @NotBlank(message = "checkIn is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "checkIn must have HH:mm format")
    String checkIn,

    @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "checkOut must have HH:mm format")
    String checkOut
) {}
