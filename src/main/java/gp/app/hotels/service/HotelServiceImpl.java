package gp.app.hotels.service;

import gp.app.hotels.dto.request.HotelCreateRequestDto;
import gp.app.hotels.dto.request.HotelSearchParams;
import gp.app.hotels.dto.response.HotelFullResponseDto;
import gp.app.hotels.dto.response.HotelShortResponseDto;
import gp.app.hotels.model.Amenity;
import gp.app.hotels.model.Hotel;
import gp.app.hotels.repository.AmenityRepository;
import gp.app.hotels.repository.HistogramResult;
import gp.app.hotels.repository.HotelRepository;
import gp.app.hotels.repository.HotelShort;
import gp.app.hotels.repository.specification.HotelSpecificationBuilder;
import gp.app.hotels.repository.specification.HotelSpecifications;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final AmenityRepository amenityRepository;
    private final HotelMapper hotelMapper;
    private final Map<String, Supplier<List<HistogramResult>>> histogramProviders;

    public HotelServiceImpl(HotelRepository hotelRepository, AmenityRepository amenityRepository, HotelMapper hotelMapper) {
        this.hotelRepository = hotelRepository;
        this.amenityRepository = amenityRepository;
        this.hotelMapper = hotelMapper;
        this.histogramProviders = Map.of(
                "brand", hotelRepository::countGroupedByBrand,
                "city", hotelRepository::countGroupedByCity,
                "country", hotelRepository::countGroupedByCountry,
                "amenities", hotelRepository::countGroupedByAmenities
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelShortResponseDto> getAllHotels() {
        return hotelRepository.findAllBy().stream()
                .map(hotelMapper::toShortDtoFromProjection)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HotelFullResponseDto getHotelById(UUID id) {
        Hotel hotel = hotelRepository.findWithAmenitiesById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hotel not found with id: " + id));
        return hotelMapper.toFullDto(hotel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelShortResponseDto> searchHotels(HotelSearchParams params) {
        Specification<Hotel> spec = new HotelSpecificationBuilder()
                .withString(params.name(), HotelSpecifications::hasName)
                .withString(params.brand(), HotelSpecifications::hasBrand)
                .withString(params.city(), HotelSpecifications::hasCity)
                .withString(params.country(), HotelSpecifications::hasCountry)
                .withStringList(params.amenities(), HotelSpecifications::hasAmenity)
                .build();

        // Выполняем динамический запрос к БД и преобразовываем результат в интерфейс-проекцию
        List<HotelShort> projections = hotelRepository.findBy(spec, q -> q.as(HotelShort.class).all());

        return projections.stream()
                .map(hotelMapper::toShortDtoFromProjection)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HotelShortResponseDto createHotel(HotelCreateRequestDto request) {
        Hotel hotel = hotelMapper.toEntity(request);
        Hotel savedHotel = hotelRepository.save(hotel);
        return hotelMapper.toShortDto(savedHotel);
    }

    @Override
    @Transactional
    public void addAmenitiesToHotel(UUID hotelId, List<String> newAmenitiesList) {
        Hotel hotel = hotelRepository.findWithAmenitiesById(hotelId)
                .orElseThrow(() -> new EntityNotFoundException("Hotel not found with id: " + hotelId));

        if (newAmenitiesList != null && !newAmenitiesList.isEmpty()) {
            // Находим уже существующие в базе удобства из переданного списка
            Set<Amenity> existingAmenities = amenityRepository.findAllByNameIn(newAmenitiesList);
            Set<String> existingNames = existingAmenities.stream()
                    .map(Amenity::getName)
                    .collect(Collectors.toSet());

            Set<Amenity> toAdd = new HashSet<>(existingAmenities);

            // Те, которых нет в базе, мы создаём
            for (String name : newAmenitiesList) {
                if (!existingNames.contains(name)) {
                    Amenity newAmenity = new Amenity(name);
                    amenityRepository.save(newAmenity);
                    toAdd.add(newAmenity);
                }
            }

            // Добавляем отсутствовавшие у отеля удобства
            hotel.getAmenities().addAll(toAdd);
            hotelRepository.save(hotel);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getHistogramByParam(String param) {
        Supplier<List<HistogramResult>> provider = histogramProviders.get(param.toLowerCase());
        
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported histogram parameter: " + param);
        }

        List<HistogramResult> results = provider.get();

        return results.stream()
                .filter(r -> r.getGroupName() != null) // исключаем null-ключи (например, когда отель без бренда)
                .collect(Collectors.toMap(
                        HistogramResult::getGroupName,
                        HistogramResult::getCount,
                        Long::sum // Если вдруг ключи задублируются, суммируем
                ));
    }
}
