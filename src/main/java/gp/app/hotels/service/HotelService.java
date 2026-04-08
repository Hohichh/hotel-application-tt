package gp.app.hotels.service;

import gp.app.hotels.dto.request.HotelCreateRequestDto;
import gp.app.hotels.dto.request.HotelSearchParams;
import gp.app.hotels.dto.response.HotelFullResponseDto;
import gp.app.hotels.dto.response.HotelShortResponseDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HotelService {

    List<HotelShortResponseDto> getAllHotels();

    HotelFullResponseDto getHotelById(UUID id);

    List<HotelShortResponseDto> searchHotels(HotelSearchParams params);

    HotelShortResponseDto createHotel(HotelCreateRequestDto request);

    void addAmenitiesToHotel(UUID hotelId, List<String> amenities);

    Map<String, Long> getHistogramByParam(String param);
}
