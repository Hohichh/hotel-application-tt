# Здесь представлен дизайн ER-схемы проекта
---
Исходя из ТЗ и анализа json контракта были выделены следующие сущности и представление UML:

```uml

entity Hotel {
    * id: UUID <<PK>>
    --
    * name: String
    description: String
    brand: String
    --
    // Embedded Address
    * houseNumber: String
    * street: String
    * city: String
    * country: String
    * postCode: String
    --
    // Embedded Contacts
    * phone: String
    * email: String
    --
    // Embedded ArrivalTime
    * checkIn: LocalTime
    checkOut: LocalTime
}

entity Amenity {
    * id: UUID <<PK>>
    --
    * name: String <<Unique>>
}

entity hotel_amenities <<Join Table>> {
    * hotel_id: UUID <<FK>>
    * amenity_id: UUID <<FK>>
}

Hotel "1" *-- "many" hotel_amenities
Amenity "1" *-- "many" hotel_amenities

@enduml
```
Здесь:
- Главная таблица Hotel, содержащая ключевую инфу. Поскольку семантически адрес, контакты и прочее информация имеет отношение композиции с Hotel (т.е. не имеет смысла без неё, жесткая связь), а также для минимизации использования join-таблиц, было принято решение сделать таблицу плоской, что поможет отптимизировать время отклика.
- В коде, для удобства маппинга представим Address, Contacts, ArrivalTime как @Embeddable объекты.
- Поскольку amenities - это явный случай many-to-many, то было принято решение вынести его в отдельную таблицу с промежуточной таблицей hotel_amenities - это позволит эффективно расширять список удобств, не затрагивая структуру отелей.

В спецификацию liquibase нужно добавить индексы на поля, по которым происходит фильтрация - name, brand, city, country, И в таблице Amenities - на столбец name, а также стоит поставить unique констрейнт на name.