package gp.app.hotels.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactsRequestDto(
    @NotBlank(message = "phone is required")
    @Pattern(
        regexp = "^\\+(?:[0-9][\\s-]?){6,14}[0-9]$",
        message = "phone must be in international format (e.g. +375 17 309-80-00)"
    )
    String phone,

    @NotBlank(message = "email is required")
    @Size(max = 255, message = "email must be at most 255 characters")
    @Email(message = "email has invalid format")
    String email
) {}
