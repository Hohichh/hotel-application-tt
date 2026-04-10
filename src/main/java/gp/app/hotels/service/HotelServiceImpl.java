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
import gp.app.hotels.exception.HotelNotFoundException;
import gp.app.hotels.exception.InvalidParameterException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
                .orElseThrow(() -> new HotelNotFoundException(id));
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
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        if (newAmenitiesList != null && !newAmenitiesList.isEmpty()) {
            Set<String> normalizedNames = newAmenitiesList.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (normalizedNames.isEmpty()) {
                return;
            }

            List<String> normalizedNamesList = new ArrayList<>(normalizedNames);
            Set<Amenity> existingAmenities = amenityRepository.findAllByNameIn(normalizedNamesList);
            Set<String> existingNames = existingAmenities.stream()
                    .map(Amenity::getName)
                    .collect(Collectors.toSet());

            Set<Amenity> toAdd = new HashSet<>(existingAmenities);
            List<String> missingNames = normalizedNamesList.stream()
                    .filter(name -> !existingNames.contains(name))
                    .collect(Collectors.toList());

            if (!missingNames.isEmpty()) {
                List<Amenity> newAmenities = missingNames.stream()
                        .map(Amenity::new)
                        .collect(Collectors.toList());

                try {
                    toAdd.addAll(amenityRepository.saveAll(newAmenities));
                } catch (DataIntegrityViolationException ex) {
                    toAdd.addAll(amenityRepository.findAllByNameIn(missingNames));
                }
            }

            hotel.getAmenities().addAll(toAdd);
            hotelRepository.save(hotel);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getHistogramByParam(String param) {
        Supplier<List<HistogramResult>> provider = histogramProviders.get(param.toLowerCase());
        
        if (provider == null) {
            throw new InvalidParameterException(param, "Unsupported histogram parameter: " + param);
        }

        List<HistogramResult> results = provider.get();

        return results.stream()
                .filter(r -> r.getGroupName() != null) // исключаем null-ключи (например, когда отель без бренда)
                .collect(Collectors.toMap(
                        HistogramResult::getGroupName,
                        HistogramResult::getCount,
                        (left, right) -> {
                            long leftValue = left == null ? 0L : left;
                            long rightValue = right == null ? 0L : right;
                            return leftValue + rightValue;
                        } // Если вдруг ключи задублируются, суммируем
                ));
    }
}
