package gp.app.hotels.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import gp.app.hotels.service.HotelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Histogram", description = "Histogram API")
@RequestMapping("/property-view/histogram")
public class HistogramController {

    private final HotelService hotelService;

    public HistogramController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @Operation(summary = "Get histogram", description = "Returns hotel counts grouped by brand, city, country, or amenities")
    @ApiResponse(responseCode = "200", description = "Histogram generated")
    @ApiResponse(responseCode = "400", description = "Invalid parameter")
    @GetMapping("/{param}")
    public ResponseEntity<Map<String, Long>> getHistogramByParam(@PathVariable String param) {
        return ResponseEntity.ok(hotelService.getHistogramByParam(param));
    }
}
