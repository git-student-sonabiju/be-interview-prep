# be-interview-prep

Spring Boot 3 backend exercises: five small APIs built one branch and one PR at a time.

## Stack

Java 21, Spring Boot 3.5, Maven (wrapper included), H2 in-memory database, JUnit 5 + MockMvc.

## Run

The JWT signing secret is never stored in the repo. Provide it (and optionally an admin account) through environment variables; see `.env.example`.

| Variable | Required | Purpose |
|----------|----------|---------|
| `JWT_SECRET` | yes | HS256 signing key, at least 32 characters |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | no | Creates an ADMIN account on startup if it does not exist |

```bash
export JWT_SECRET=$(openssl rand -hex 32)
export ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=<choose-one>
./mvnw spring-boot:run
```

PowerShell: `$env:JWT_SECRET = -join ((48..57)+(97..102) | Get-Random -Count 64 | % {[char]$_})`, then `.\mvnw.cmd spring-boot:run`.

The API listens on `http://localhost:8080`. The H2 console is at `/h2-console` (JDBC URL `jdbc:h2:mem:interview`).

## Test

```bash
./mvnw test
```

Tests need no environment variables: the test profile generates a random JWT secret per run. On Windows use `mvnw.cmd` instead of `./mvnw`.

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | [#1](https://github.com/git-student-sonabiju/be-interview-prep/pull/1) |
| 2 | URL Shortener | [#2](https://github.com/git-student-sonabiju/be-interview-prep/pull/2) |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |
