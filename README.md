# Hospital Appointment Booking System

A web application where patients book doctor appointments, doctors manage their schedule, and admins manage the hospital. A built-in NLP assistant (Apache OpenNLP) reads the patient's symptoms and suggests the right department.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 (Spring MVC, Spring Security, Spring Data JPA / Hibernate) |
| Database | MySQL 8 |
| UI | Thymeleaf + Bootstrap 5 |
| NLP | Apache OpenNLP (document categorizer) |
| API docs | Swagger UI (springdoc-openapi) |
| Testing | JUnit 5, Mockito, MockMvc, H2 (in-memory, tests only) |
| Build | Maven (wrapper included) |

## Features

**Patient**
- Register and log in
- Describe symptoms → NLP suggests a department (with confidence)
- Pick a doctor, date and a free 30-minute slot (slots load live)
- View appointment history and doctor notes; cancel pending/confirmed appointments

**Doctor**
- Dashboard with today's / pending / confirmed counts
- Confirm, complete or cancel appointments, with notes

**Admin**
- Statistics dashboard and list of all appointments
- Add doctors (creates their login), enable/disable doctors
- Add departments, view registered patients

**Business rules**
- Slots: 09:00–17:00 every 30 min, lunch break 13:00–14:00
- No double booking of a doctor's slot, or of a patient at the same time
- No bookings in the past or more than 60 days ahead
- Status flow: `PENDING → CONFIRMED → COMPLETED`, and `PENDING/CONFIRMED → CANCELLED`

## Project structure

```
src/main/java/com/hospital
├── config/        SecurityConfig, AppConfig (Swagger + Clock), DataInitializer (demo data)
├── controller/    MVC controllers: Home, Patient, Doctor, Admin
│   └── api/       REST controllers: Public, Patient, Doctor, Admin
├── dto/           Form objects and API request/response records
├── entity/        User, Doctor, Department, Appointment, Role, AppointmentStatus
├── exception/     BookingException, ResourceNotFoundException, ApiExceptionHandler
├── repository/    Spring Data JPA repositories
├── security/      CustomUserDetailsService, RoleBasedSuccessHandler
└── service/       AppointmentService, DoctorService, DepartmentService, UserService, SymptomAnalyzerService
src/main/resources
├── nlp/symptom-training.txt   training data for the OpenNLP model
├── templates/                 Thymeleaf pages
└── application.properties
```

## Setup

### 1. MySQL 8

```bash
brew install mysql@8.4
brew services start mysql@8.4
mysql -u root -e "CREATE DATABASE hospital_db;
  CREATE USER 'hospital'@'localhost' IDENTIFIED BY 'hospital123';
  GRANT ALL PRIVILEGES ON hospital_db.* TO 'hospital'@'localhost';"
```

The default config connects to `localhost:3307` (this machine runs MySQL 8.4 on 3307 because another MySQL server already uses 3306). To use a different server, set environment variables:

```bash
export DB_URL="jdbc:mysql://localhost:3306/hospital_db?createDatabaseIfNotExist=true"
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
```

Tables are created automatically by Hibernate (`ddl-auto=update`) and demo data is loaded on the first run.

### 2. Run

```bash
./mvnw spring-boot:run
```

Or in VS Code: install the recommended extensions, open `HospitalBookingApplication.java` and click **Run**.

Open http://localhost:8080

### 3. Test

```bash
./mvnw test
```

Tests use an in-memory H2 database, so they don't need MySQL.

## Demo accounts

| Role | Email | Password |
|---|---|---|
| Admin | admin@hospital.com | admin123 |
| Doctor | ananya@hospital.com (also vikram, priya, arjun, kavya, suresh, meera, rahul) | doctor123 |
| Patient | patient@hospital.com | patient123 |

## REST API

Swagger UI: http://localhost:8080/swagger-ui.html. Click **Authorize** and log in with a demo account (HTTP Basic).

| Method | Endpoint | Role |
|---|---|---|
| GET | `/api/departments` | public |
| GET | `/api/doctors?departmentId=` | public |
| GET | `/api/doctors/{id}/slots?date=YYYY-MM-DD` | public |
| POST | `/api/symptoms/analyze` | public |
| GET / POST | `/api/patient/appointments` | PATIENT |
| PATCH | `/api/patient/appointments/{id}/cancel` | PATIENT |
| GET | `/api/doctor/appointments` | DOCTOR |
| PATCH | `/api/doctor/appointments/{id}/status` | DOCTOR |
| GET | `/api/admin/appointments`, `/api/admin/stats`, `/api/admin/doctors` | ADMIN |
| POST | `/api/admin/doctors` | ADMIN |
| PATCH | `/api/admin/doctors/{id}/availability` | ADMIN |

Example:

```bash
curl -X POST localhost:8080/api/symptoms/analyze \
  -H 'Content-Type: application/json' \
  -d '{"symptoms":"chest pain and palpitations"}'
# {"department":"Cardiology","confidence":0.75,...}
```

## How the NLP works

`SymptomAnalyzerService` trains an OpenNLP **maximum-entropy document categorizer** at startup from `nlp/symptom-training.txt`. Each line is `<Department> <example symptoms>`. Input text is lower-cased and tokenized, then the model returns a probability for each department. The highest one is suggested and saved with the appointment. To improve accuracy, or to support a new department, add more example lines to the training file.
