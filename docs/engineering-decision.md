# Engineering Decisions

## Why Transactional Outbox?

To prevent the dual-write problem between PostgreSQL and NATS. User data and its event are recorded in one database transaction, while the relay publishes committed events asynchronously and retries failures.

## Why NATS JetStream?

For durable asynchronous event delivery, explicit acknowledgement, redelivery, persistent streams, and durable consumer state.

## Why Keycloak?

To delegate OAuth 2.0 and OpenID Connect authentication rather than implementing credential storage and authentication logic inside each service.

## Why Separate Databases?

To preserve service ownership of data, support independent evolution, and reduce tight coupling between microservices.
