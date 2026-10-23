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

The Spring Boot project has been initialized.

Verification completed on October 21, 2026:

- Maven build succeeded.
- One application context test passed.
- The packaged application started successfully on port 8080.
- A request to `/` returned HTTP 404.

The homepage and business features are pending implementation.