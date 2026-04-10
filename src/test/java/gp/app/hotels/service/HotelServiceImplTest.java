package gp.app.hotels.service;

import gp.app.hotels.dto.request.AddressRequestDto;
import gp.app.hotels.dto.request.ArrivalTimeRequestDto;
import gp.app.hotels.dto.request.ContactsRequestDto;
import gp.app.hotels.dto.request.HotelCreateRequestDto;
import gp.app.hotels.dto.request.HotelSearchParams;
import gp.app.hotels.dto.response.AddressResponseDto;
import gp.app.hotels.dto.response.ArrivalTimeResponseDto;
import gp.app.hotels.dto.response.ContactsResponseDto;
import gp.app.hotels.dto.response.HotelFullResponseDto;
import gp.app.hotels.dto.response.HotelShortResponseDto;
import gp.app.hotels.exception.HotelNotFoundException;
import gp.app.hotels.exception.InvalidParameterException;
import gp.app.hotels.model.Address;
import gp.app.hotels.model.Amenity;
import gp.app.hotels.model.Hotel;
import gp.app.hotels.repository.AmenityRepository;
import gp.app.hotels.repository.HistogramResult;
import gp.app.hotels.repository.HotelRepository;
import gp.app.hotels.repository.HotelShort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelServiceImpl Unit Tests")
class HotelServiceImplTest {

    
    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private HotelMapper hotelMapper;

    @Captor
    private ArgumentCaptor<List<Amenity>> amenitiesCaptor;

    @InjectMocks
    private HotelServiceImpl hotelService;

    // ─────────────────────────────────────────────────────────────
    // Test Fixtures — set of pre-conditions and testing objects
    // ─────────────────────────────────────────────────────────────

    static final UUID HOTEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    static Hotel aHotel() {
        Hotel hotel = new Hotel();
        hotel.setId(HOTEL_ID);
        hotel.setName("Grand Palace");
        hotel.setDescription("Luxury hotel in city center");
        hotel.setBrand("PalaceGroup");
        hotel.setAddress(new Address("10", "Baker Street", "London", "UK", "NW1 6XE"));
        return hotel;
    }

    static HotelShort aHotelShortProjection() {
        return new HotelShort() {
            @Override public UUID getId()          { return HOTEL_ID; }
            @Override public String getName()      { return "Grand Palace"; }
            @Override public String getDescription() { return "Luxury hotel in city center"; }
            @Override public String getAddress()   { return "10 Baker Street, London, NW1 6XE, UK"; }
            @Override public String getPhone()     { return "+447000000001"; }
        };
    }

    static HotelShortResponseDto aShortDto() {
        return new HotelShortResponseDto(
                HOTEL_ID,
                "Grand Palace",
                "Luxury hotel in city center",
                "10 Baker Street, London, NW1 6XE, UK",
                "+447000000001"
        );
    }

    static HotelFullResponseDto aFullDto() {
        return new HotelFullResponseDto(
                HOTEL_ID,
                "Grand Palace",
                "Luxury hotel in city center",
                "PalaceGroup",
                new AddressResponseDto("10", "Baker Street", "London", "UK", "NW1 6XE"),
                new ContactsResponseDto("+447000000001", "info@palace.com"),
                new ArrivalTimeResponseDto("14:00", "12:00"),
                List.of("WiFi", "Pool")
        );
    }

    static HotelCreateRequestDto aCreateRequest() {
        return new HotelCreateRequestDto(
                "Grand Palace",
                "Luxury hotel in city center",
                "PalaceGroup",
                new AddressRequestDto("10", "Baker Street", "London", "UK", "NW1 6XE"),
                new ContactsRequestDto("+447000000001", "info@palace.com"),
                new ArrivalTimeRequestDto("14:00", "12:00")
        );
    }

    static HistogramResult aHistogramResult(String groupName, Long count) {
        return new HistogramResult() {
            @Override public String getGroupName() { return groupName; }
            @Override public Long getCount()       { return count; }
        };
    }

    static Amenity anAmenity(String name) {
        return new Amenity(name);
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllHotels()")
    class GetAllHotels {

        @Test
        @DisplayName("should return mapped DTOs when hotels exist in repository")
        void should_return_mapped_dtos_when_hotels_exist() {
            // Given
            HotelShort projection  = aHotelShortProjection();
            HotelShortResponseDto expectedDto = aShortDto();
            when(hotelRepository.findAllBy()).thenReturn(List.of(projection));
            when(hotelMapper.toShortDtoFromProjection(projection)).thenReturn(expectedDto);

            // When
            List<HotelShortResponseDto> result = hotelService.getAllHotels();

            // Then
            assertThat(result).hasSize(1).containsExactly(expectedDto);
            verify(hotelRepository).findAllBy();
            verify(hotelMapper).toShortDtoFromProjection(projection);
        }

        @Test
        @DisplayName("should return empty list when repository has no hotels")
        void should_return_empty_list_when_no_hotels() {
            // Given
            when(hotelRepository.findAllBy()).thenReturn(Collections.emptyList());

            // When
            List<HotelShortResponseDto> result = hotelService.getAllHotels();

            // Then
            assertThat(result).isEmpty();
            verify(hotelMapper, never()).toShortDtoFromProjection(any());
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHotelById(UUID)")
    class GetHotelById {

        @Test
        @DisplayName("should return full DTO when hotel exists")
        void should_return_full_dto_when_hotel_found() {
            // Given
            Hotel hotel = aHotel();
            HotelFullResponseDto expectedDto = aFullDto();
            when(hotelRepository.findWithAmenitiesById(HOTEL_ID)).thenReturn(Optional.of(hotel));
            when(hotelMapper.toFullDto(hotel)).thenReturn(expectedDto);

            // When
            HotelFullResponseDto result = hotelService.getHotelById(HOTEL_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(hotelRepository).findWithAmenitiesById(HOTEL_ID);
            verify(hotelMapper).toFullDto(hotel);
        }

        @Test
        @DisplayName("should throw HotelNotFoundException when hotel does not exist")
        void should_throw_HotelNotFoundException_when_hotel_not_found() {
            // Given
            when(hotelRepository.findWithAmenitiesById(HOTEL_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> hotelService.getHotelById(HOTEL_ID))
                    .isInstanceOf(HotelNotFoundException.class)
                    .hasMessageContaining(HOTEL_ID.toString());

            verify(hotelMapper, never()).toFullDto(any());
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchHotels(HotelSearchParams)")
    class SearchHotels {

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("should delegate to repository and return mapped results")
        void should_call_repository_with_spec_and_return_mapped_results() {
            // Given 
            HotelSearchParams params = new HotelSearchParams("Grand", "PalaceGroup", "London", "UK", List.of("WiFi"));
            HotelShort projection   = aHotelShortProjection();
            HotelShortResponseDto expectedDto = aShortDto();

            when(hotelRepository.findBy(any(Specification.class), any())).thenReturn(List.of(projection));
            when(hotelMapper.toShortDtoFromProjection(projection)).thenReturn(expectedDto);

            // When
            List<HotelShortResponseDto> result = hotelService.searchHotels(params);

            // Then
            assertThat(result).hasSize(1).containsExactly(expectedDto);
            verify(hotelRepository).findBy(any(Specification.class), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("should return empty list when no hotels match search criteria")
        void should_return_empty_list_when_no_match() {
            // Given
            HotelSearchParams params = new HotelSearchParams("NoSuch", null, null, null, null);
            when(hotelRepository.findBy(any(Specification.class), any())).thenReturn(Collections.emptyList());

            // When
            List<HotelShortResponseDto> result = hotelService.searchHotels(params);

            // Then
            assertThat(result).isEmpty();
            verify(hotelMapper, never()).toShortDtoFromProjection(any());
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createHotel(HotelCreateRequestDto)")
    class CreateHotel {

        @Test
        @DisplayName("should save entity and return short DTO")
        void should_save_entity_and_return_short_dto() {
            // Given
            HotelCreateRequestDto request   = aCreateRequest();
            Hotel hotelEntity  = aHotel();
            Hotel savedHotel   = aHotel();
            HotelShortResponseDto expectedDto = aShortDto();

            when(hotelMapper.toEntity(request)).thenReturn(hotelEntity);
            when(hotelRepository.save(hotelEntity)).thenReturn(savedHotel);
            when(hotelMapper.toShortDto(savedHotel)).thenReturn(expectedDto);

            // When
            HotelShortResponseDto result = hotelService.createHotel(request);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(hotelMapper).toEntity(request);
            verify(hotelRepository).save(hotelEntity);
            verify(hotelMapper).toShortDto(savedHotel);
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addAmenitiesToHotel(UUID, List<String>)")
    class AddAmenitiesToHotel {

        @Test
        @DisplayName("should throw HotelNotFoundException when hotel does not exist")
        void should_throw_when_hotel_not_found() {
            // Given
            when(hotelRepository.findWithAmenitiesById(HOTEL_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> hotelService.addAmenitiesToHotel(HOTEL_ID, List.of("WiFi")))
                    .isInstanceOf(HotelNotFoundException.class)
                    .hasMessageContaining(HOTEL_ID.toString());

            verify(amenityRepository, never()).findAllByNameIn(anyList());
        }

        @Test
        @DisplayName("should do nothing when amenities list is null")
        void should_do_nothing_when_list_is_null() {
            // Given
            when(hotelRepository.findWithAmenitiesById(HOTEL_ID)).thenReturn(Optional.of(aHotel()));

            // When
            hotelService.addAmenitiesToHotel(HOTEL_ID, null);

            // Then
            verify(amenityRepository, never()).findAllByNameIn(anyList());
            verify(hotelRepository, never()).save(any());
        }

        @Test
        @DisplayName("should do nothing when amenities list is empty")
        void should_do_nothing_when_list_is_empty() {
            // Given
            when(hotelRepository.findWithAmenitiesById(HOTEL_ID)).thenReturn(Optional.of(aHotel()));

            // When
            hotelService.addAmenitiesToHotel(HOTEL_ID, Collections.emptyList());

            // Then
            verify(amenityRepository, never()).findAllByNameIn(anyList());
            verify(hotelRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create all new amenities when none exist in DB")
        void should_create_all_amenities_when_none_exist_in_db() {
            // Given
            Hotel hotel = aHotel();
            List<String> newNames = List.of("WiFi", "Pool");
            when(hotelRepository.findWithAmenitiesById(HOTEL_ID)).thenReturn(Optional.of(hotel));
            when(amenityRepository.findAllByNameIn(newNames)).thenReturn(Collections.emptySet());
            when(amenityRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            hotelService.addAmenitiesToHotel(HOTEL_ID, newNames);

            // Then
            verify(amenityRepository).saveAll(anyList());
            verify(hotelRepository).save(hotel);
            assertThat(hotel.getAmenities()).hasSize(2);
        }

        @Test
        @DisplayName("should not save amenities that already exist in DB")
        void should_not_save_existing_amenities() {
            // Given
            Hotel hotel             = aHotel();
            Amenity existingAmenity = anAmenity("WiFi");
            List<String> names      = List.of("WiFi");
            when(hotelRepository.findWithAmenitiesById(HOTEL_ID)).thenReturn(Optional.of(hotel));
            when(amenityRepository.findAllByNameIn(names)).thenReturn(Set.of(existingAmenity));

            // When
            hotelService.addAmenitiesToHotel(HOTEL_ID, names);

            // Then
            verify(amenityRepository, never()).saveAll(anyList());
            verify(hotelRepository).save(hotel);
            assertThat(hotel.getAmenities()).contains(existingAmenity);
        }

        @Test
        @DisplayName("should save only missing amenities when list is mixed (some exist, some new)")
        void should_create_only_missing_amenities_in_mixed_case() {
            // Given
            Hotel hotel              = aHotel();
            Amenity existingAmenity  = anAmenity("WiFi");
            List<String> names       = List.of("WiFi", "Parking"); // WiFi — exists, Parking - isn't
            when(hotelRepository.findWithAmenitiesById(HOTEL_ID)).thenReturn(Optional.of(hotel));
            when(amenityRepository.findAllByNameIn(names)).thenReturn(Set.of(existingAmenity));
            when(amenityRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            hotelService.addAmenitiesToHotel(HOTEL_ID, names);

            // Then
            verify(amenityRepository).saveAll(amenitiesCaptor.capture());
            assertThat(amenitiesCaptor.getValue()).hasSize(1);
            assertThat(amenitiesCaptor.getValue().getFirst().getName()).isEqualTo("Parking");

            verify(hotelRepository).save(hotel);
            assertThat(hotel.getAmenities()).hasSize(2);
        }

        @Test
        @DisplayName("should fallback to re-read when concurrent amenity insert causes integrity violation")
        void should_fallback_to_reread_on_integrity_violation() {
            // Given
            Hotel hotel = aHotel();
            List<String> names = List.of("Spa");
            Amenity persistedSpa = anAmenity("Spa");

            when(hotelRepository.findWithAmenitiesById(HOTEL_ID)).thenReturn(Optional.of(hotel));
            when(amenityRepository.findAllByNameIn(names))
                    .thenReturn(Collections.emptySet())
                    .thenReturn(Set.of(persistedSpa));
            when(amenityRepository.saveAll(anyList())).thenThrow(new DataIntegrityViolationException("duplicate"));

            // When
            hotelService.addAmenitiesToHotel(HOTEL_ID, names);

            // Then
            verify(amenityRepository).saveAll(anyList());
            verify(amenityRepository, times(2)).findAllByNameIn(anyList());
            verify(hotelRepository).save(hotel);
            assertThat(hotel.getAmenities()).contains(persistedSpa);
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistogramByParam(String)")
    class GetHistogramByParam {

        @Test
        @DisplayName("should throw InvalidParameterException for unsupported parameter")
        void should_throw_InvalidParameterException_when_param_unsupported() {
            // Given
            String unsupported = "unknown_param";

            // When / Then
            assertThatThrownBy(() -> hotelService.getHistogramByParam(unsupported))
                    .isInstanceOf(InvalidParameterException.class)
                    .hasMessageContaining(unsupported);
        }

        @Test
        @DisplayName("should return grouped map for valid 'brand' parameter")
        void should_return_map_for_valid_brand_param() {
            // Given
            when(hotelRepository.countGroupedByBrand())
                    .thenReturn(List.of(
                            aHistogramResult("PalaceGroup", 3L),
                            aHistogramResult("StarInn", 1L)
                    ));

            // When
            Map<String, Long> result = hotelService.getHistogramByParam("brand");

            // Then
            assertThat(result)
                    .hasSize(2)
                    .containsEntry("PalaceGroup", 3L)
                    .containsEntry("StarInn", 1L);
        }

        @Test
        @DisplayName("should accept param case-insensitively (e.g., 'CITY')")
        void should_accept_param_case_insensitively() {
            // Given
            when(hotelRepository.countGroupedByCity())
                    .thenReturn(List.of(aHistogramResult("London", 5L)));

            // When
            Map<String, Long> result = hotelService.getHistogramByParam("CITY");

            // Then
            assertThat(result).containsEntry("London", 5L);
        }

        @Test
        @DisplayName("should filter out results with null groupName")
        void should_filter_out_null_group_names() {
            // Given 
            when(hotelRepository.countGroupedByBrand())
                    .thenReturn(List.of(
                            aHistogramResult(null, 2L),
                            aHistogramResult("StarInn", 4L)
                    ));

            // When
            Map<String, Long> result = hotelService.getHistogramByParam("brand");

            // Then 
            assertThat(result)
                    .hasSize(1)
                    .containsEntry("StarInn", 4L)
                    .doesNotContainKey(null);
        }

        @Test
        @DisplayName("should support all valid parameters: brand, city, country, amenities")
        void should_support_all_valid_params() {
            // Given
            when(hotelRepository.countGroupedByBrand()).thenReturn(Collections.emptyList());
            when(hotelRepository.countGroupedByCity()).thenReturn(Collections.emptyList());
            when(hotelRepository.countGroupedByCountry()).thenReturn(Collections.emptyList());
            when(hotelRepository.countGroupedByAmenities()).thenReturn(Collections.emptyList());

            // When / Then
            for (String param : List.of("brand", "city", "country", "amenities")) {
                assertThat(hotelService.getHistogramByParam(param)).isEmpty();
            }
        }
    }
}
