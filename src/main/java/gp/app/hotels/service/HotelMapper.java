package gp.app.hotels.service;

import gp.app.hotels.dto.request.HotelCreateRequestDto;
import gp.app.hotels.dto.response.HotelFullResponseDto;
import gp.app.hotels.dto.response.HotelShortResponseDto;
import gp.app.hotels.model.Amenity;
import gp.app.hotels.model.Hotel;
import gp.app.hotels.repository.HotelShort;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    // 1. Проекция (из базы напрямую) -> Short DTO
    // Тут не нужны правила, имена полей (id, name, description, address, phone) совпадают 1в1
    HotelShortResponseDto toShortDtoFromProjection(HotelShort projection);

    // 2. Полная Entity -> Short DTO (используется при ответе POST /hotels)
    @Mapping(target = "address", source = ".", qualifiedByName = "formatAddress")
    @Mapping(target = "phone", source = "contacts.phone")
    HotelShortResponseDto toShortDto(Hotel hotel);

    // 3. Entity -> Full DTO (для GET /hotels/{id})
    // address/contacts замапятся сами (имеют одинаковую структуру), 
    // amneities превращаем в List<String>, DateTimes форматируем в String
    @Mapping(target = "amenities", source = "amenities", qualifiedByName = "mapAmenitiesToStrings")
    @Mapping(target = "arrivalTime.checkIn", source = "arrivalTime.checkIn", qualifiedByName = "formatTime")
    @Mapping(target = "arrivalTime.checkOut", source = "arrivalTime.checkOut", qualifiedByName = "formatTime")
    HotelFullResponseDto toFullDto(Hotel hotel);

    // 4. CreateRequest DTO -> Entity (для сохранения при POST /hotels)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "amenities", ignore = true)
    @Mapping(target = "arrivalTime.checkIn", source = "arrivalTime.checkIn", qualifiedByName = "parseTime")
    @Mapping(target = "arrivalTime.checkOut", source = "arrivalTime.checkOut", qualifiedByName = "parseTime")
    Hotel toEntity(HotelCreateRequestDto request);

    // Вспомогательные методы (преобразователи типов)

    @Named("formatAddress")
    default String formatAddress(Hotel hotel) {
        if (hotel == null || hotel.getAddress() == null) return null;
        var adr = hotel.getAddress();
        return adr.getHouseNumber() + " " + adr.getStreet() + ", " + 
               adr.getCity() + ", " + adr.getPostCode() + ", " + adr.getCountry();
    }

    @Named("mapAmenitiesToStrings")
    default List<String> mapAmenitiesToStrings(Set<Amenity> amenities) {
        if (amenities == null) return null;
        return amenities.stream().map(Amenity::getName).collect(Collectors.toList());
    }

    @Named("formatTime")
    default String formatTime(LocalTime time) {
        if (time == null) return null;
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Named("parseTime")
    default LocalTime parseTime(String time) {
        if (time == null || time.isEmpty()) return null;
        return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
    }
}
