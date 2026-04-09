# Hotel Application (Technical Task)

RESTful API for hotel management built with Java 21, Spring Boot, Spring Data JPA, Liquibase, and H2.

## Run

- Prerequisite: Java 21+
- Start app:

```bash
mvn spring-boot:run
```

- Default port: `8092`

## Base API prefix

All endpoints are exposed under `property-view` prefix directly in controller mappings:

- `/property-view/hotels`
- `/property-view/hotels/{id}`
- `/property-view/search`
- `/property-view/hotels` (POST)
- `/property-view/hotels/{id}/amenities` (POST)
- `/property-view/histogram/{param}`

## Key features

- Layered architecture: controller → service → repository.
- DTO-based API contract (`request` and `response` records).
- Dynamic search via JPA Specifications.
- Liquibase-driven schema management.
- Validation + unified `ProblemDetail` error responses.
- OpenAPI/Swagger annotations on controllers.

## Validation and error handling

- Request body validation (`@Valid`) for hotel creation.
- Method-level validation (`@NotEmpty`, `@NotBlank`) for amenities list.
- Global exception handler returns structured `ProblemDetail` with `invalidParams` for both:
  - `MethodArgumentNotValidException`
  - `HandlerMethodValidationException`

## Search behavior

- `name`: case-insensitive `contains` (`LIKE`).
- `brand`, `city`, `country`: case-insensitive exact match.
- `amenities`: exact amenity name match.

## Database and schema

- Main profile uses H2.
- Hibernate DDL mode is `validate` (schema is validated, not auto-mutated).
- Liquibase changelogs are the single source of schema changes.

## Swagger UI

- OpenAPI docs are available via Springdoc UI after app start.

## Requirement compliance (summary)

- Required endpoints: implemented.
- Required prefix `property-view`: implemented.
- Port `8092`: implemented.
- Java 21: configured.
- Technologies (Maven, Spring Boot, JPA, Liquibase, H2): implemented.
- Tests: present (unit + integration).
- Swagger/OpenAPI: present.
