# API Documentation

Base URL: `http://localhost:8080`

## Authentication

All business endpoints require an OAuth 2.0 bearer JWT issued by the `notification-system` Keycloak realm.

```http
Authorization: Bearer <access-token>
```

Missing, expired, malformed, incorrectly signed, or wrong-issuer tokens receive `401 Unauthorized`.

## Create User

`POST /api/users`

Creates a user and atomically records a `user.created` outbox event. Notification creation is asynchronous and is not part of the HTTP response transaction.

### Request

```json
{
  "name": "Ajeet Baghel",
  "email": "ajeet@example.com"
}
```

| Field | Type | Required | Validation |
|---|---|---:|---|
| `name` | string | yes | Non-blank, maximum 100 characters |
| `email` | string | yes | Valid email, non-blank, maximum 150 characters, globally unique |

### Successful Response

Status: `201 Created`

Header:

```http
Location: /api/users/1
```

Body:

```json
{
  "id": 1,
  "name": "Ajeet Baghel",
  "email": "ajeet@example.com",
  "createdAt": "2026-09-10T12:00:00.000000"
}
```

### Validation Error

Status: `400 Bad Request`

```json
{
  "timestamp": "2026-09-10T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/users",
  "validationErrors": {
    "name": "Name is required",
    "email": "Email must be valid"
  }
}
```

Malformed JSON also returns `400` with `message` set to `Request body is malformed`.

### Duplicate Email

Status: `409 Conflict`

```json
{
  "timestamp": "2026-09-10T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "User already exists with email: ajeet@example.com",
  "path": "/api/users",
  "validationErrors": {}
}
```

### Unexpected Error

Status: `500 Internal Server Error`

```json
{
  "timestamp": "2026-09-10T12:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/users",
  "validationErrors": {}
}
```

Internal exception details are logged but are not exposed to clients.

## Health Endpoints

| Component | Endpoint | Authentication |
|---|---|---|
| API Gateway | `GET /actuator/health` | Public |
| User Service | Internal `GET /actuator/health` | Public inside Docker network |
| Notification Service | Internal `GET /actuator/health` | Public inside Docker network |
| NATS monitoring | `GET http://localhost:8222/healthz` | Local monitoring endpoint |
| Keycloak discovery | `GET http://localhost:8090/realms/notification-system/.well-known/openid-configuration` | Public |

## Event Contract

Subject: `user.created`

```json
{
  "id": 1,
  "name": "Ajeet Baghel",
  "email": "ajeet@example.com",
  "createdAt": "2026-09-10T12:00:00.000000"
}
```

The outbox row UUID is sent as the JetStream message ID. Notification Service uses `id` as its persistent idempotency key.
