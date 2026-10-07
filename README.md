# be-interview-prep

Spring Boot 3 backend exercises: five small APIs built one branch and one PR at a time.

## Stack

Java 21, Spring Boot 3.5, Maven (wrapper included), H2 in-memory database, JUnit 5 + MockMvc.

## Run

```bash
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`. The H2 console is at `/h2-console` (JDBC URL `jdbc:h2:mem:interview`).

## Test

```bash
./mvnw test
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |
