# Microservices Notification System

A secure, asynchronous notification system built with Spring Boot, Spring Cloud Gateway, NATS JetStream, Keycloak, and PostgreSQL.

## Architecture

The system contains three independently deployable applications:

- **API Gateway:** public entry point on port `8080`; validates Keycloak JWTs and routes user requests.
- **User Service:** owns users and its PostgreSQL database; independently validates JWTs and records `user.created` events through a transactional outbox.
- **Notification Service:** has no business REST API; consumes `user.created` asynchronously and owns notifications in a separate PostgreSQL database.

User Service and Notification Service communicate only through NATS JetStream. See [Architecture](docs/architecture.md) for component and sequence diagrams.

## Reliability and Security

- OAuth 2.0 resource-server security at both the Gateway and User Service.
- Keycloak realm configuration imported at startup.
- Separate NATS users for producer and consumer with least-privilege subject permissions.
- Secrets supplied through environment variables and excluded from Git.
- Persistent JetStream stream and durable consumer.
- Explicit acknowledgements, delayed negative acknowledgements, and a maximum of five deliveries.
- Transactional outbox makes user persistence and event recording atomic.
- JetStream message IDs provide broker-side duplicate suppression.
- Notification `user_id` uniqueness and an idempotency check make redelivery safe.
- Separate PostgreSQL databases and persistent volumes per service.
- Validation, structured API errors, health probes, non-root containers, and automated tests.

The included Compose setup is intended for local evaluation. Keycloak uses development mode and traffic is confined to the local Docker network. A public deployment should terminate TLS at the ingress, enable TLS for NATS and database links, use a production Keycloak database/mode, and manage secrets with the target platform's secret store.

## Prerequisites

- Docker Desktop with Docker Compose v2
- At least 4 GB of free Docker memory
- PowerShell for the examples below
- Java 21 only if running tests outside Docker

## Run Locally

1. Create the local environment file:

```powershell
Copy-Item .env.example .env
```

2. Replace every placeholder value in `.env` with a strong, distinct password.

3. Build and start the complete system:

```powershell
docker compose up -d --build
```

The first build downloads Maven and Java images and can take several minutes. Later builds use Docker layer caching.

4. Check container health:

```powershell
docker compose ps
```

5. Check public services:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8090/realms/notification-system/.well-known/openid-configuration
```

## Create a Local Keycloak User

1. Open `http://localhost:8090/admin`.
2. Sign in with `KEYCLOAK_ADMIN_USERNAME` and `KEYCLOAK_ADMIN_PASSWORD` from `.env`.
3. Select the `notification-system` realm.
4. Create a user with first name, last name, email, and `Email verified` enabled.
5. Set a non-temporary password on the Credentials tab.

Acquire a development token:

```powershell
$tokenResponse = Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8090/realms/notification-system/protocol/openid-connect/token' `
  -ContentType 'application/x-www-form-urlencoded' `
  -Body @{
    client_id = 'notification-system-client'
    username = '<username>'
    password = '<password>'
    grant_type = 'password'
  }
$token = $tokenResponse.access_token
```

Direct password grants are enabled only to make local assignment evaluation simple. Production browser clients should use Authorization Code Flow with PKCE.

## Use the API

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/users' `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType 'application/json' `
  -Body '{"name":"Ajeet Baghel","email":"ajeet@example.com"}'
```

A successful request returns `201 Created`. User persistence returns immediately; notification creation happens asynchronously. See [API documentation](docs/api.md) for the complete contract and error format.

## Observe Event Delivery

JetStream monitoring is available locally at `http://localhost:8222/jsz?streams=true&consumers=true`.

Inspect persisted records:

```powershell
docker compose exec user-postgres `
  psql -U user_service -d user_service `
  -c 'SELECT id, subject, attempts, published_at FROM outbox_events;'

docker compose exec notification-postgres `
  psql -U notification_service -d notification_service `
  -c 'SELECT id, user_id, recipient, created_at FROM notifications;'
```

## Run Tests

```powershell
./user-service/mvnw.cmd -f user-service/pom.xml test
./user-service/mvnw.cmd -f notification-service/pom.xml test
./user-service/mvnw.cmd -f api-gateway/pom.xml test
```

The suites cover JWT enforcement, validation, API errors, duplicate users, event creation, outbox success and retry behavior, notification creation, idempotent duplicate handling, and database uniqueness.

## Stop or Reset

Stop containers while preserving data:

```powershell
docker compose down
```

Delete containers and all local persisted data:

```powershell
docker compose down -v
```

## Configuration

All required local secrets are listed in `.env.example`. Application settings can additionally be overridden with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`, `NATS_URL`, `NATS_USERNAME`, `NATS_PASSWORD`, `JWT_ISSUER_URI`, `JWT_JWK_SET_URI`, and `USER_SERVICE_URL`.

## Technology Stack

- Java 21
- Spring Boot 3.3.4
- Spring Cloud Gateway 2023.0.3
- Keycloak 26.3.3
- NATS JetStream 2.10
- PostgreSQL 16
- Maven, JUnit 5, Mockito, and Docker Compose