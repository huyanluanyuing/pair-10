---
paths:
  - "**/*.java"
  - "**/pom.xml"
  - "**/application*.properties"
  - "**/application*.yml"
---
# Java and Spring Boot rules

## Versions

start.spring.io generates Spring Boot 4.1 (Java 21, Maven). AI tools often write Spring Boot 3 code. Verified against Boot 4.1.1:

- Web starter `spring-boot-starter-webmvc`, test starter `spring-boot-starter-webmvc-test`. Not `spring-boot-starter-web`.
- Mock a bean in a slice test with `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`), not `@MockBean`.
- `@WebMvcTest` and `@AutoConfigureMockMvc` are in `org.springframework.boot.webmvc.test.autoconfigure`.
- The web starter alone gives no `RestClient.Builder` bean. Use `RestClient.builder()`.
- A filter annotated `@Component` is included in `@WebMvcTest`, so slice tests must satisfy it (for example, send any header it requires).
- `@WebMvcTest` without a controller class loads every controller in the app, and fails to start (`NoSuchBeanDefinitionException`) as soon as one of them needs a service. Always name the controllers under test: `@WebMvcTest(OrderController.class)`. A test-only nested controller is named there as well as in `@Import`.

## Structure

- Package by feature: one package per feature, plus `common` for what every feature shares.
- Controllers parse and validate input, call one service method and return a record. No calculations, no repository calls.
- Services load data and call calculators. Only services touch repositories and external clients.
- Calculations are plain classes: no Spring annotations, no I/O, no clock reads. "Today" is a parameter.
- Request and response bodies are records. Stored objects are never mutated; build new ones.
- Constructor injection only in production code, never `@Autowired` on fields; test classes may use `@Autowired` fields. No Lombok: records cover data classes.

## Numbers and time

- Exact decimal amounts, such as money, are `BigDecimal`, created from a string or an integer, never from a `double`. Round only when producing output, in one place.
- Compare `BigDecimal` with `compareTo`, not `equals`: `equals` also compares scale, so 2.0 and 2.00 differ.
- "Now" comes from an injected `java.time.Clock` bean, so tests can fix the date. Never `LocalDate.now()` or `Instant.now()` without the clock.

## Errors and external calls

- Throw exceptions. One `@RestControllerAdvice` maps each one to a status and the project's single error JSON shape. No response contains a stack trace.
- Every external HTTP client has a connect timeout and a read timeout (`SimpleClientHttpRequestFactory`, passed to `RestClient.builder().requestFactory(...)`), and sits behind an interface so tests can fake it.
- Data from an external system is translated into our own records at the boundary (an anti-corruption layer). Its field names appear nowhere else.
