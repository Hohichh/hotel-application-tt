# Service layer design

В данном случае доменная модель предполагает наличие единственного сервиса - HotelService, который будет агрегировать оба репозитория. 

Для начала следует определить интерфейс, а также реализовать необходимые DTO и интерфейс для mapstruct.

DTO предлагаю разделить по пакетам:
- request dto
- response dto
А также использовать Records 

Рассмотрим DTOшки для каждого энд-поинта а также особенности сервисных методов:

1. GET /hotels 

**Метод** getAllHotels()

**Response DTO**
```java
public record HotelShortResponseDto(
    UUID id,
    String name,
    String description,
    String address,
    String phone
) {}
```
- Мы должны получить список hotels. нужно убедиться что придут проекции. А также что проекции эффективны (open/closed)
- вернуть лист dto 

2. GET /hotels/{id}

**Метод** getHotelById(UUID id)

**Response DTO**
```java
public record HotelFullResponseDto(
    UUID id,
    String name,
    String description,
    String brand,
    AddressResponseDto address,
    ContactsResponseDto contacts,
    ArrivalTimeResponseDto arrivalTime,
    List<String> amenities
) {}

public record AddressResponseDto(
    int houseNumber,
    String street,
    String city,
    String country,
    String postCode
) {}

public record ContactsResponseDto(
    String phone,
    String email
) {}

public record ArrivalTimeResponseDto(
    String checkIn,
    String checkOut
) {}
```
- Мы получаем id отеля. 
- Вернуть дто с отелем, вложенное для корректного json, как в тз. 
- Проверки: что отель сущетсвует - иначе кинь runtime exception пока что. напиши в конструкторе 404.

3. GET /search

**Метод** searchHotels(HotelSearchParams params)

**Request DTO**
```java
public record HotelSearchParams(
    String name,
    String brand,
    String city,
    String country,
    List<String> amenities
) {}
```
- мое предложение: в этот метод передавать не кучу параметров, а отлельную dto с параметрами. Её можно склеить из параметров запроса. 
- На основе этих полей собрать specification который будет использован как параметр к репозиторию. 
- вернуть лист отелей.
- Здесь проверить что приходит короткая проекция.

4. POST /hotels

**Метод** createHotel(HotelCreateRequestDto request)

**Request DTO**
```java
public record HotelCreateRequestDto(
    String name,
    String description,
    String brand,
    AddressRequestDto address,
    ContactsRequestDto contacts,
    ArrivalTimeRequestDto arrivalTime
) {}

public record AddressRequestDto(
    int houseNumber,
    String street,
    String city,
    String country,
    String postCode
) {}

public record ContactsRequestDto(
    String phone,
    String email
) {}

public record ArrivalTimeRequestDto(
    String checkIn,
    String checkOut  // Делаем строки для обработки времени перед сохранением
) {}
```
- Получаем DTO, содержащее описание отеля со вложенными записями. (поле amenities тут отсутствует в соответствии с ТЗ).
- Выполняем маппинг `HotelCreateRequestDto` в JPA сущность `Hotel` (используя MapStruct или маппер).
- Сохраняем сущность `Hotel` через `hotelRepository.save(hotel)`.
- Возвращаем краткую информацию `HotelShortResponseDto` (согласно ТЗ, ответ короткий, как у GET). Маппим свежесохранённую сущность обратно в ответный DTO.
- При сохранении проверяем валидацию.

5. POST /hotels/{id}/amenities

**Метод** addAmenitiesToHotel(UUID hotelId, List<String> amenities)

**Request Body Type**
- Принимает `List<String> amenities` напрямую, отдельная запись (Record) здесь не требуется.

**Логика и проверки**
- Ищем отель по `id` (например, через `hotelRepository.findById(hotelId)`). 
- **Исключения**: Если отель не найден -> выбрасываем `EntityNotFoundException` (с сообщением для 404).
- Делаем запрос к `amenityRepository.findAllByNameIn(amenities)` для поиска уже существующих в БД удобств.
- Выделяем из переданного списка названия удобств, которых еще нет в таблице `amenity`, создаем для них новые объекты `new Amenity(name)` и сохраняем.
- Добавляем объединенный сет (существующие + сохраненные новые) в коллекцию отеля `hotel.getAmenities().addAll(...)`.
- Сохраняем `Hotel` через `hotelRepository.save(...)`. Hibernate самостоятельно добавит записи в таблицу `hotel_amenities`.
- В идеале возвращать статус 200 или 204.

6. GET /histogram/{param}

**Метод** getHistogramByParam(String param)

**Возвращаемое значение**
- Здесь возвращаем `Map<String, Long>`. Spring Boot "из коробки" сериализует мапу в структуру вида:
```json
{
    "Free WiFi": 20,
    "Non-smoking": 5
}
```

**Логика и проверки**
- Делаем `switch-case` по параметру `param` или используем `Map` из функций-обработчиков.
- **Исключения**: Если параметр не равен `brand`, `city`, `country` или `amenities`, выбрасываем кастомное исключение или IllegalArgumentException для ответа "400 Bad Request" с сообщением.
- Вызываем соответствующий метод: например, `hotelRepository.countGroupedByAmenities()` для удобств.
- Преобразуем `List<HistogramResult>` в словарь с помощью `stream().collect(Collectors.toMap(HistogramResult::getGroupName, HistogramResult::getCount))`.