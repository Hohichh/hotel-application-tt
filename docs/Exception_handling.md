 Для отлова исключений на рантайме напишем глобальный хендлер. 
 Можно переопределить ошибку для 404й.
-Проверить могут ли быть ещё исключения бизнеса, на рантайме
использовать специальный тип, (problemdetail) который маппит ошибки в подробные json описания.

Создание надежной и предсказуемой системы обработки исключений — это фундамент качественного API. В Spring Boot существует несколько подходов к решению этой задачи, от локальных до глобальных. 

Ниже представлен подробный обзор стратегий, инструментов и лучших практик для обработки различных категорий ошибок.

---

## 1. Основные стратегии и инструменты Spring Boot

В современной разработке на Spring Boot (особенно начиная с версии 3.0) выделяют три основных подхода.

### Глобальная обработка: `@RestControllerAdvice` (Золотой стандарт)
Это паттерн, основанный на AOP (Aspect-Oriented Programming). Он позволяет перехватывать исключения, выброшенные в любом контроллере, в одном централизованном месте.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("USER_NOT_FOUND", ex.getMessage()));
    }
}
```

### Локальная обработка: `ResponseStatusException`
Подходит для простых приложений или быстрых прототипов, когда не хочется создавать иерархию кастомных классов исключений. Исключение выбрасывается прямо в коде сервиса или контроллера.

```java
if (user == null) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
}
```
*Минус:* Размывает бизнес-логику HTTP-статусами.

### Стандарт RFC 7807: `ProblemDetail` (Spring Boot 3+)
Spring Framework 6 ввел встроенную поддержку стандарта RFC 7807 для ответов об ошибках. Вы можете возвращать объект `ProblemDetail`, который имеет стандартизированные поля (`type`, `title`, `status`, `detail`, `instance`).

```java
@ExceptionHandler(InsufficientFundsException.class)
public ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problemDetail.setTitle("Insufficient Funds");
    problemDetail.setProperty("currentBalance", ex.getBalance());
    return problemDetail;
}
```

---

## 2. Типичные ошибки и как их обрабатывать

Ошибки следует разделять по их природе, так как каждая категория требует своего HTTP-статуса и уровня логирования.

### A. Логические (бизнес-ошибки)
Возникают, когда нарушаются правила бизнес-домена.
* **Примеры:** `UserNotFoundException`, `EmailAlreadyExistsException`, `OrderAlreadyProcessedException`.
* **HTTP Статусы:** `400 Bad Request`, `404 Not Found`, `409 Conflict`, `422 Unprocessable Entity`.
* **Стратегия:** Создайте базовое бизнес-исключение (например, `AppException`) и наследуйте от него остальные. Возвращайте понятный клиенту код ошибки (не путать с HTTP статусом) и сообщение.
* **Логирование:** Уровень `INFO` или `WARN` (это ожидаемые ошибки, стек-трейс писать в логи чаще всего не нужно).

### B. Ошибки валидации входных данных
Возникают при невалидных JSON-телах запросов или параметрах.
* **Примеры:** `MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException`.
* **HTTP Статус:** `400 Bad Request`.
* **Стратегия:** Перехватывать в `@RestControllerAdvice`, извлекать список полей, не прошедших валидацию, и возвращать клиенту структурированный массив ошибок.

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
        String fieldName = ((FieldError) error).getField();
        String errorMessage = error.getDefaultMessage();
        errors.put(fieldName, errorMessage);
    });
    return ResponseEntity.badRequest().body(errors);
}
```

### C. Ошибки подключений и инфраструктуры
Связаны с недоступностью базы данных, сторонних API (через `RestTemplate`, `WebClient`, `Feign`), или брокеров сообщений.
* **Примеры:** `DataAccessResourceFailureException` (упала БД), `ResourceAccessException` (таймаут стороннего API), `FeignException`.
* **HTTP Статусы:** `502 Bad Gateway`, `503 Service Unavailable`, `504 Gateway Timeout`.
* **Стратегия:** Никогда не отдавайте клиенту детали подключения (URL стороннего сервиса или SQL-запрос). Возвращайте обобщенное "Сервис временно недоступен". Внедрите паттерны *Retry* и *Circuit Breaker* (например, через Resilience4j), чтобы приложение не падало каскадно.
* **Логирование:** Уровень `ERROR` со всем стек-трейсом. Системы мониторинга (Prometheus, Grafana) должны реагировать на такие ошибки алертами.

### D. Непредвиденные ошибки Runtime
Самые опасные баги программиста: `NullPointerException`, `IndexOutOfBoundsException`, `IllegalArgumentException` и прочие необработанные исключения.
* **Примеры:** Любые классы, наследующие `RuntimeException`, которые вы не ожидали.
* **HTTP Статус:** `500 Internal Server Error`.
* **Стратегия:** Настройте метод-перехватчик "последней надежды", который будет ловить базовый `Exception.class` или `RuntimeException.class`.
* **Критическое правило:** Запрещено возвращать стек-трейс или техническое описание ошибки `500` на фронтенд. Это огромная дыра в безопасности. Возвращайте: `{"error": "Внутренняя ошибка сервера. Обратитесь в поддержку с ID запроса: XYZ"}`.

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex, WebRequest request) {
    String errorId = UUID.randomUUID().toString();
    log.error("Unknown error occurred. ErrorId: {}", errorId, ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "Произошла непредвиденная ошибка. ID: " + errorId));
}
```

---

## 3. Архитектурные Best Practices

2.  **Не используйте исключения для управления потоком выполнения (Control Flow):** Исключения в Java — тяжелая операция, так как они собирают стек-трейс (метод `fillInStackTrace()`). Если событие происходит часто (например, неверный пароль), лучше возвращать `Result/Either` объект или `Optional`, чем генерировать Exception.
3.  **Переопределение базового `ResponseEntityExceptionHandler`:** Если вы используете `@RestControllerAdvice`, хорошей практикой является наследование вашего обработчика от `ResponseEntityExceptionHandler`. Это даст вам "из коробки" базовую обработку множества внутренних исключений Spring (например, когда не поддерживается HTTP метод — `HttpRequestMethodNotSupportedException`), позволяя просто переопределить нужные методы.
4.  **Словарь ошибок:** Вместо хардкода строковых сообщений в коде, храните их в `messages.properties` или используйте `Enum` с кодами ошибок (например, `ERR_USER_001`). Это упростит локализацию (i18n) и позволит фронтенду привязываться к стабильным кодам, а не к меняющемуся тексту.

