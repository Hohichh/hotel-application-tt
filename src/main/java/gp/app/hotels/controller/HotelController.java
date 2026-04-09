package gp.app.hotels.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import gp.app.hotels.dto.request.HotelCreateRequestDto;
import gp.app.hotels.dto.request.HotelSearchParams;
import gp.app.hotels.dto.response.HotelFullResponseDto;
import gp.app.hotels.dto.response.HotelShortResponseDto;
import gp.app.hotels.service.HotelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@Tag(name = "Hotels", description = "Hotel API")
@RequestMapping("/property-view")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @Operation(summary = "Get all hotels", description = "Returns short information for all hotels")
    @ApiResponse(responseCode = "200", description = "Hotels retrieved")
    @GetMapping("/hotels")
    public ResponseEntity<List<HotelShortResponseDto>> getAllHotels() {
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    @Operation(summary = "Get hotel by id", description = "Returns full information for a specific hotel")
    @ApiResponse(responseCode = "200", description = "Hotel retrieved")
    @ApiResponse(responseCode = "404", description = "Hotel not found")
    @GetMapping("/hotels/{id}")
    public ResponseEntity<HotelFullResponseDto> getHotelById(@PathVariable UUID id) {
        return ResponseEntity.ok(hotelService.getHotelById(id));
    }

    @Operation(summary = "Search hotels", description = "Searches hotels by name, brand, city, country and amenities")
    @ApiResponse(responseCode = "200", description = "Search completed")
    @GetMapping("/search")
    public ResponseEntity<List<HotelShortResponseDto>> searchHotels(HotelSearchParams params) {
        return ResponseEntity.ok(hotelService.searchHotels(params));
    }

    @Operation(summary = "Create hotel", description = "Creates a new hotel and returns short information")
    @ApiResponse(responseCode = "201", description = "Hotel created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @PostMapping("/hotels")
    public ResponseEntity<HotelShortResponseDto> createHotel(@Valid @RequestBody HotelCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelService.createHotel(request));
    }

    @Operation(summary = "Add amenities", description = "Adds amenities to an existing hotel")
    @ApiResponse(responseCode = "201", description = "Amenities added")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Hotel not found")
    @PostMapping("/hotels/{id}/amenities")
    public ResponseEntity<Void> addAmenitiesToHotel(
            @PathVariable UUID id,
            @NotEmpty(message = "amenities list must not be empty")
            @Valid @RequestBody List<@NotBlank(message = "amenity name must not be blank") String> amenities
    ) {
        hotelService.addAmenitiesToHotel(id, amenities);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
