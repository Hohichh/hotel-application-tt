package gp.app.hotels.controller;

import gp.app.hotels.dto.request.AddressRequestDto;
import gp.app.hotels.dto.request.ArrivalTimeRequestDto;
import gp.app.hotels.dto.request.ContactsRequestDto;
import gp.app.hotels.dto.request.HotelCreateRequestDto;
import gp.app.hotels.model.Address;
import gp.app.hotels.model.Amenity;
import gp.app.hotels.model.ArrivalTime;
import gp.app.hotels.model.Contacts;
import gp.app.hotels.model.Hotel;
import gp.app.hotels.repository.AmenityRepository;
import gp.app.hotels.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end интеграционный тест API отелей.
 * Поднимает полный Spring-контекст с H2 in-memory базой.
 * Схема создаётся через Liquibase из основного changelog.
 * Каждый тест работает в транзакции, которая откатывается после — БД изолирована.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Hotel API Integration Tests (E2E, H2)")
class HotelControllerIntegrationTest {

    private static final String BASE_URL = "/property-view";

    @Autowired
    private AmenityRepository amenityRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private MockMvc mockMvc;

    // ─────────────────────────────────────────────────────────────
    // Pre-condition: данные в базе перед каждым тестом
    // ─────────────────────────────────────────────────────────────

    private UUID grandPalaceId;

    @BeforeEach
    void setUp() {
        // Amenities
        Amenity wifi    = amenityRepository.save(new Amenity("WiFi"));
        Amenity pool    = amenityRepository.save(new Amenity("Pool"));
        Amenity parking = amenityRepository.save(new Amenity("Parking"));

        // Hotel 1 — Grand Palace (London, PalaceGroup)
        Hotel grandPalace = new Hotel();
        grandPalace.setName("Grand Palace");
        grandPalace.setDescription("Luxury hotel in city center");
        grandPalace.setBrand("PalaceGroup");
        grandPalace.setAddress(new Address("10", "Baker Street", "London", "UK", "NW1 6XE"));
        grandPalace.setContacts(new Contacts("+447000000001", "palace@example.com"));
        grandPalace.setArrivalTime(new ArrivalTime(LocalTime.of(14, 0), LocalTime.of(12, 0)));
        grandPalace.setAmenities(new HashSet<>(Set.of(wifi, pool)));
        grandPalaceId = hotelRepository.save(grandPalace).getId();

        // Hotel 2 — Star Inn (Manchester, StarBrand) — с amenity Parking
        Hotel starInn = new Hotel();
        starInn.setName("Star Inn");
        starInn.setDescription("Budget hotel near station");
        starInn.setBrand("StarBrand");
        starInn.setAddress(new Address("5", "Oxford Road", "Manchester", "UK", "M1 5QA"));
        starInn.setContacts(new Contacts("+447000000002", "star@example.com"));
        starInn.setArrivalTime(new ArrivalTime(LocalTime.of(15, 0), LocalTime.of(11, 0)));
        starInn.setAmenities(new HashSet<>(Set.of(parking)));
        hotelRepository.save(starInn);
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /hotels")
    class GetAllHotels {

        @Test
        @DisplayName("should return all hotels with short info")
        void should_return_all_hotels() throws Exception {
            mockMvc.perform(get(BASE_URL + "/hotels"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", hasItem("Grand Palace")))
                    .andExpect(jsonPath("$[*].name", hasItem("Star Inn")));
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /hotels/{id}")
    class GetHotelById {

        @Test
        @DisplayName("should return full hotel details when hotel exists")
        void should_return_hotel_by_id() throws Exception {
            mockMvc.perform(get(BASE_URL + "/hotels/{id}", grandPalaceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(grandPalaceId.toString())))
                    .andExpect(jsonPath("$.name", is("Grand Palace")))
                    .andExpect(jsonPath("$.brand", is("PalaceGroup")))
                    .andExpect(jsonPath("$.address.city", is("London")))
                    .andExpect(jsonPath("$.amenities", hasItem("WiFi")))
                    .andExpect(jsonPath("$.amenities", hasItem("Pool")));
        }

        @Test
        @DisplayName("should return 404 with ProblemDetail when hotel not found")
        void should_return_404_when_hotel_not_found() throws Exception {
            UUID randomId = UUID.randomUUID();
            mockMvc.perform(get(BASE_URL + "/hotels/{id}", randomId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title", is("Hotel Not Found")))
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(randomId.toString())));
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /search")
    class SearchHotels {

        @Test
        @DisplayName("should filter hotels by brand")
        void should_filter_by_brand() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search").param("brand", "PalaceGroup"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Grand Palace")));
        }

        @Test
        @DisplayName("should filter hotels by brand case-insensitively")
        void should_filter_by_brand_case_insensitively() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search").param("brand", "palacegroup"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Grand Palace")));
        }

        @Test
        @DisplayName("should filter hotels by city")
        void should_filter_by_city() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search").param("city", "Manchester"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Star Inn")));
        }

        @Test
        @DisplayName("should filter hotels by name using contains and ignore case")
        void should_filter_by_name_with_contains_ignore_case() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search").param("name", "grand"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Grand Palace")));
        }

        @Test
        @DisplayName("should filter hotels by amenity")
        void should_filter_by_amenity() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search").param("amenities", "Pool"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Grand Palace")));
        }

        @Test
        @DisplayName("should return all hotels when no search params provided")
        void should_return_all_when_no_params() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("should return empty list when no hotels match criteria")
        void should_return_empty_when_no_match() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search").param("brand", "NonExistentBrand"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /hotels")
    class CreateHotel {

        @Test
        @DisplayName("should create hotel and return 201 with short DTO")
        void should_create_hotel() throws Exception {
            HotelCreateRequestDto request = new HotelCreateRequestDto(
                    "New Hotel",
                    "Brand new hotel",
                    "NewBrand",
                    new AddressRequestDto("1A", "Main St", "Bristol", "UK", "BS1 1AA"),
                    new ContactsRequestDto("+447999999999", "new@hotel.com"),
                    new ArrivalTimeRequestDto("15:00", "11:00")
            );
            String requestJson = """
                    {
                      "name": "%s",
                      "description": "%s",
                      "brand": "%s",
                      "address": {
                        "houseNumber": "%s",
                        "street": "%s",
                        "city": "%s",
                        "country": "%s",
                        "postCode": "%s"
                      },
                      "contacts": {
                        "phone": "%s",
                        "email": "%s"
                      },
                      "arrivalTime": {
                        "checkIn": "%s",
                        "checkOut": "%s"
                      }
                    }
                    """.formatted(
                    request.name(),
                    request.description(),
                    request.brand(),
                    request.address().houseNumber(),
                    request.address().street(),
                    request.address().city(),
                    request.address().country(),
                    request.address().postCode(),
                    request.contacts().phone(),
                    request.contacts().email(),
                    request.arrivalTime().checkIn(),
                    request.arrivalTime().checkOut()
            );

            mockMvc.perform(post(BASE_URL + "/hotels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("New Hotel")));
        }

        @Test
        @DisplayName("should return 400 with validation error when payload is invalid")
        void should_return_400_on_invalid_payload() throws Exception {
            String invalidRequestJson = """
                    {
                      "name": "",
                      "description": "Invalid hotel",
                      "brand": "",
                      "address": {
                        "houseNumber": "@#$",
                        "street": "",
                        "city": "",
                        "country": "",
                        "postCode": ""
                      },
                      "contacts": {
                        "phone": "abc",
                        "email": "wrong-email"
                      },
                      "arrivalTime": {
                        "checkIn": "25:00",
                        "checkOut": "99:99"
                      }
                    }
                    """;

            mockMvc.perform(post(BASE_URL + "/hotels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Validation Error")))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.invalidParams.name", notNullValue()))
                    .andExpect(jsonPath("$.invalidParams.brand", notNullValue()))
                    .andExpect(jsonPath("$.invalidParams", notNullValue()));
        }

        @Test
        @DisplayName("should return 400 with malformed JSON problem detail")
        void should_return_400_on_malformed_json() throws Exception {
            String malformedJson = "{\"name\":\"Broken Hotel\",";

            mockMvc.perform(post(BASE_URL + "/hotels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Malformed JSON")))
                    .andExpect(jsonPath("$.status", is(400)));
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /hotels/{id}/amenities")
    class AddAmenitiesToHotel {

        @Test
        @DisplayName("should add new amenity to hotel and return 201")
        void should_add_new_amenity() throws Exception {
            mockMvc.perform(post(BASE_URL + "/hotels/{id}/amenities", grandPalaceId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"Gym\"]"))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return 404 when hotel not found")
        void should_return_404_when_hotel_not_found() throws Exception {
            mockMvc.perform(post(BASE_URL + "/hotels/{id}/amenities", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"Spa\"]"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title", is("Hotel Not Found")));
        }

        @Test
        @DisplayName("should return 400 when amenities list is empty")
        void should_return_400_when_amenities_list_is_empty() throws Exception {
            mockMvc.perform(post(BASE_URL + "/hotels/{id}/amenities", grandPalaceId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Validation Error")))
                    .andExpect(jsonPath("$.invalidParams", notNullValue()));
        }

        @Test
        @DisplayName("should return 400 when amenities contains blank value")
        void should_return_400_when_amenity_name_is_blank() throws Exception {
            mockMvc.perform(post(BASE_URL + "/hotels/{id}/amenities", grandPalaceId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"  \"]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Validation Error")))
                    .andExpect(jsonPath("$.invalidParams", notNullValue()));
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /histogram/{param}")
    class GetHistogram {

        @Test
        @DisplayName("should return histogram grouped by brand")
        void should_return_histogram_by_brand() throws Exception {
            mockMvc.perform(get(BASE_URL + "/histogram/brand"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.PalaceGroup", is(1)))
                    .andExpect(jsonPath("$.StarBrand", is(1)));
        }

        @Test
        @DisplayName("should return histogram grouped by city")
        void should_return_histogram_by_city() throws Exception {
            mockMvc.perform(get(BASE_URL + "/histogram/city"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.London", is(1)))
                    .andExpect(jsonPath("$.Manchester", is(1)));
        }

        @Test
        @DisplayName("should return histogram grouped by amenities")
        void should_return_histogram_by_amenities() throws Exception {
            mockMvc.perform(get(BASE_URL + "/histogram/amenities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.WiFi", is(1)))
                    .andExpect(jsonPath("$.Pool", is(1)))
                    .andExpect(jsonPath("$.Parking", is(1)));
        }

        @Test
        @DisplayName("should return 400 with ProblemDetail for unsupported parameter")
        void should_return_400_for_invalid_param() throws Exception {
            mockMvc.perform(get(BASE_URL + "/histogram/unknown"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Invalid Parameter")))
                    .andExpect(jsonPath("$.status", is(400)));
        }
    }
}
