# 🗄️ DocVault — Spring Boot Document Storage App

A full-stack document management system built with **Spring Boot 3**, **MySQL**, **Spring Security**, and **Thymeleaf**.

---

## Tech Stack

| Layer        | Technology                                              |
|--------------|---------------------------------------------------------|
| Backend      | Spring Boot 3.2, Java 17                               |
| ORM          | Spring Data JPA + Hibernate                            |
| Database     | MySQL 8.x (files stored as LONGBLOB)                   |
| Migrations   | Flyway                                                  |
| Security     | Spring Security (form login, BCrypt, remember-me)       |
| File detect  | Apache Tika (detects real content type, not just ext)  |
| Frontend     | Thymeleaf + Vanilla JS + CSS                           |
| Boilerplate  | Lombok, MapStruct                                       |
| Testing      | JUnit 5, Mockito, Spring Boot Test                     |

---

## Project Structure

```
docvault/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/docvault/
    │   │   ├── DocVaultApplication.java
    │   │   ├── config/
    │   │   │   └── SecurityConfig.java          # Spring Security setup
    │   │   ├── controller/
    │   │   │   ├── AuthController.java           # Login / Register pages
    │   │   │   ├── DashboardController.java      # Main UI page
    │   │   │   └── DocumentController.java       # REST API (/api/documents)
    │   │   ├── dto/
    │   │   │   ├── AuthDto.java
    │   │   │   └── DocumentDto.java
    │   │   ├── exception/
    │   │   │   ├── DocVaultExceptions.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── model/
    │   │   │   ├── Document.java
    │   │   │   ├── DocumentAudit.java
    │   │   │   └── User.java
    │   │   ├── repository/
    │   │   │   ├── DocumentAuditRepository.java
    │   │   │   ├── DocumentRepository.java
    │   │   │   └── UserRepository.java
    │   │   └── service/
    │   │       ├── DocumentService.java          # Core upload/download/delete logic
    │   │       └── UserService.java              # Auth + UserDetailsService
    │   └── resources/
    │       ├── application.properties
    │       ├── db/migration/
    │       │   └── V1__init_schema.sql           # Flyway creates tables on startup
    │       ├── static/
    │       │   ├── css/styles.css
    │       │   └── js/app.js
    │       └── templates/
    │           ├── dashboard.html
    │           └── auth/
    │               ├── login.html
    │               └── register.html
    └── test/
        └── java/com/docvault/
            └── DocumentServiceTest.java
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.x running locally

---

## Setup & Run

### 1. Create the MySQL database

```sql
CREATE DATABASE docvault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'docvault_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON docvault.* TO 'docvault_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Update application.properties

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/docvault?useSSL=false&serverTimezone=UTC
spring.datasource.username=docvault_user
spring.datasource.password=your_password
```

### 3. Build and run

```bash
mvn clean install
mvn spring-boot:run
```

Flyway will **automatically create all tables** on first startup via `V1__init_schema.sql`.

### 4. Open in browser

```
http://localhost:8080
```

Register an account → start uploading documents!

---

## REST API

All endpoints require authentication (session cookie from login).

| Method | Endpoint                          | Description                         |
|--------|-----------------------------------|-------------------------------------|
| POST   | `/api/documents`                  | Upload a file (multipart/form-data) |
| GET    | `/api/documents`                  | List documents (search, filter, page)|
| GET    | `/api/documents/stats`            | Storage stats for current user      |
| GET    | `/api/documents/{id}/download`    | Download a file                     |
| DELETE | `/api/documents/{id}`             | Delete a document                   |

### Upload example (curl)

```bash
curl -X POST http://localhost:8080/api/documents \
  -b "JSESSIONID=your_session_id" \
  -H "X-XSRF-TOKEN: your_csrf_token" \
  -F "file=@/path/to/invoice.pdf" \
  -F "description=Q1 invoice" \
  -F "tags=invoice,2024"
```

### List with search & filter

```
GET /api/documents?query=invoice&type=pdf&page=0&size=20
```

---

## Features

- ✅ **Multi-file upload** with drag-and-drop
- ✅ **BLOB storage** in MySQL (no filesystem dependency)
- ✅ **Real file type detection** via Apache Tika (prevents extension spoofing)
- ✅ **Search** by filename, description, or tags
- ✅ **Filter** by type (PDF, image, document, spreadsheet)
- ✅ **Pagination** with configurable page size
- ✅ **Per-user isolation** — users can only see their own files
- ✅ **Audit log** — every upload, download, and delete is tracked
- ✅ **Spring Security** with BCrypt + CSRF protection + remember-me
- ✅ **Flyway migrations** for safe schema evolution
- ✅ **Global error handler** with consistent JSON error responses
- ✅ **File size limit** (50 MB by default, configurable)

---

## Configuration

All tunable in `application.properties`:

| Property                               | Default   | Description              |
|----------------------------------------|-----------|--------------------------|
| `server.port`                          | `8080`    | HTTP port                |
| `app.document.max-file-size-bytes`     | `52428800`| 50 MB per file           |
| `app.document.allowed-content-types`  | see file  | Comma-separated MIME list|
| `spring.servlet.multipart.max-file-size` | `50MB` | Multipart upload limit   |

---

## Running Tests

```bash
mvn test
```
