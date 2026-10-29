# Print Creations

A website project for a print shop serving customers in the United States.

## Planned Features

- Product and service information
- Quote and order requests
- Artwork uploads
- Admin tools for managing customers and orders
- Order notifications and promotional emails

## Tech Stack

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper

## Build and Run

Run the following commands from the project root.

Build the application and run the configured checks:

```bash
./mvnw clean verify
```

Start the packaged application:

```bash
java -jar target/print-creations-0.0.1-SNAPSHOT.jar
```

The application runs at http://localhost:8080.

Press Control+C in the terminal running the application to stop it.

## Current Status

The public category listing API is implemented using an in-memory catalog.
The homepage and other business workflows are pending implementation.

Automated verification on October 28, 2026:

- Command: `./mvnw test`
- Results: 2 tests passed, 0 failures, 0 errors, 0 skipped.

## Public Category API

`GET /api/categories`

This public endpoint requires no authentication.
It returns HTTP 200 and a JSON array containing the following categories
in ascending ID order.

| ID | Name |
| --- | --- |
| 1 | Marketing Materials |
| 2 | Stationery & Forms |
| 3 | Posters, Banners & Signs |
| 4 | Events & Calendars |

With the application running, send a request:

```bash
curl -i http://localhost:8080/api/categories
```
