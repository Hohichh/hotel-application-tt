package gp.app.hotels.controller;

import gp.app.hotels.service.HotelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/histogram")
public class HistogramController {

    private final HotelService hotelService;

    public HistogramController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/{param}")
    public ResponseEntity<Map<String, Long>> getHistogramByParam(@PathVariable String param) {
        return ResponseEntity.ok(hotelService.getHistogramByParam(param));
    }
}
