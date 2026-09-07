# Personal Finance API

Backend REST API cho ứng dụng quản lý tài chính cá nhân — theo dõi thu chi, ngân sách, mục tiêu tiết kiệm và báo cáo.

---

## Tính năng

| Module          | Mô tả                                                                     |
| --------------- | ------------------------------------------------------------------------- |
| **Auth**        | Đăng ký, đăng nhập, JWT access/refresh token, đổi mật khẩu, OTP qua email |
| **Wallet**      | Quản lý nhiều ví (Tiền mặt, Ngân hàng, Ví điện tử, Thẻ tín dụng)          |
| **Transaction** | Ghi thu/chi, đính kèm hóa đơn, lọc theo ngày/danh mục/ví                  |
| **Category**    | Danh mục thu chi tùy chỉnh                                                |
| **Budget**      | Hạn mức chi tiêu theo danh mục và khoảng thời gian                        |
| **Goal**        | Mục tiêu tiết kiệm với theo dõi tiến độ                                   |
| **Report**      | Thống kê tổng quan, báo cáo theo tháng/năm                                |
| **File**        | Upload và quản lý hóa đơn/ảnh giao dịch                                   |

---

## Tech stack

- **Java 21** + **Spring Boot 4.1.1**
- **Spring Security** — OAuth2 Resource Server, JWT HS512
- **Spring Data JPA** + **PostgreSQL 16**
- **Flyway** — Database migration
- **Redis 7** — Cache với Spring Cache
- **RabbitMQ 3** — Gửi email OTP bất đồng bộ
- **Spring Mail** + **Thymeleaf** — Email template
- **Springdoc OpenAPI 2.8.5** — Swagger UI
- **JUnit 5** + **Mockito** + **JaCoCo** — Test coverage ≥ 80%
- **Docker** + **Docker Compose**

---

## Yêu cầu

| Công cụ        | Phiên bản |
| -------------- | --------- |
| Java (JDK)     | 21        |
| Maven          | 3.9+      |
| Docker Desktop | 24+       |

---

## Cài đặt & Chạy

**1. Clone**

```bash
git clone https://github.com/vennhuu/PersonalFinance.git
cd PersonalFinance
```

**2. Cấu hình biến môi trường**

```bash
cp .env.example .env
# Điền các giá trị thực vào .env
```

**3. Chạy toàn bộ stack với Docker Compose**

```bash
docker-compose up -d

# Xem log backend
docker-compose logs -f backend
```

**4. Chạy development (backend chạy ngoài Docker)**

```bash
# Chỉ khởi động infra
docker-compose up -d postgres redis rabbitmq

# Chạy app
./mvnw spring-boot:run
```

**5. Swagger UI:** http://localhost:8080/swagger-ui.html

**6. Chạy tests**

```bash
./mvnw verify
# Coverage report: target/site/jacoco/index.html
```

---

## Biến môi trường

| Biến                   | Mô tả                         | Ví dụ                                                  |
| ---------------------- | ----------------------------- | ------------------------------------------------------ |
| `POSTGRES_DB`          | Tên database                  | `personal_finance_db`                                  |
| `DB_USERNAME`          | Username PostgreSQL           | `postgres`                                             |
| `DB_PASSWORD`          | Password PostgreSQL           | `secret`                                               |
| `DB_URL`               | JDBC URL                      | `jdbc:postgresql://localhost:5432/personal_finance_db` |
| `POSTGRES_PORT`        | Port PostgreSQL               | `5432`                                                 |
| `JPA_DDL_AUTO`         | DDL strategy                  | `validate`                                             |
| `SHOW_SQL`             | In SQL ra console             | `false`                                                |
| `JWT_SECRET`           | Secret key (Base64, 512-bit)  | `your_base64_key`                                      |
| `JWT_ACCESS_EXPIRE`    | Thời hạn access token (giây)  | `3600`                                                 |
| `JWT_REFRESH_EXPIRE`   | Thời hạn refresh token (giây) | `1209600`                                              |
| `EMAIL_HOST`           | SMTP host                     | `smtp.gmail.com`                                       |
| `EMAIL_PORT`           | SMTP port                     | `587`                                                  |
| `EMAIL_USERNAME`       | Email gửi đi                  | `your@gmail.com`                                       |
| `EMAIL_PASSWORD`       | App password Gmail            | `xxxx xxxx xxxx xxxx`                                  |
| `REDIS_HOST`           | Redis host (local)            | `localhost`                                            |
| `REDIS_PORT`           | Redis port                    | `6379`                                                 |
| `DOCKER_REDIS_HOST`    | Redis host (Docker)           | `redis`                                                |
| `RABBITMQ_HOST`        | RabbitMQ host (local)         | `localhost`                                            |
| `RABBITMQ_PORT`        | RabbitMQ port                 | `5672`                                                 |
| `RABBITMQ_USER`        | RabbitMQ username             | `guest`                                                |
| `RABBITMQ_PASSWORD`    | RabbitMQ password             | `guest`                                                |
| `DOCKER_RABBITMQ_HOST` | RabbitMQ host (Docker)        | `rabbitmq`                                             |
| `UPLOAD_BASE_URI`      | Base URL cho file upload      | `http://localhost:8080`                                |

---

## API Endpoints

> Base URL: `http://localhost:8080/api/v1`
> ✅ = Yêu cầu JWT Bearer Token

### Auth — `/auth`

| Method | Endpoint                | Mô tả                         | Auth |
| ------ | ----------------------- | ----------------------------- | ---- |
| `POST` | `/auth/register`        | Đăng ký                       | ❌   |
| `POST` | `/auth/login`           | Đăng nhập                     | ❌   |
| `GET`  | `/auth/account`         | Thông tin tài khoản hiện tại  | ✅   |
| `GET`  | `/auth/refresh`         | Làm mới access token (cookie) | ❌   |
| `POST` | `/auth/logout`          | Đăng xuất                     | ✅   |
| `POST` | `/auth/send-otp`        | Gửi OTP quên mật khẩu         | ❌   |
| `POST` | `/auth/reset-password`  | Đặt lại mật khẩu bằng OTP     | ❌   |
| `POST` | `/auth/change-password` | Đổi mật khẩu                  | ✅   |

### User — `/users`

| Method | Endpoint         | Mô tả            | Auth |
| ------ | ---------------- | ---------------- | ---- |
| `GET`  | `/users/profile` | Xem profile      | ✅   |
| `PUT`  | `/users/profile` | Cập nhật profile | ✅   |

### Wallet — `/wallets`

| Method   | Endpoint        | Mô tả        | Auth |
| -------- | --------------- | ------------ | ---- |
| `GET`    | `/wallets`      | Danh sách ví | ✅   |
| `POST`   | `/wallets`      | Tạo ví       | ✅   |
| `PUT`    | `/wallets/{id}` | Cập nhật ví  | ✅   |
| `DELETE` | `/wallets/{id}` | Xóa ví       | ✅   |

### Transaction — `/transactions`

| Method   | Endpoint             | Mô tả                                 | Auth |
| -------- | -------------------- | ------------------------------------- | ---- |
| `GET`    | `/transactions`      | Danh sách giao dịch (phân trang, lọc) | ✅   |
| `POST`   | `/transactions`      | Tạo giao dịch                         | ✅   |
| `PUT`    | `/transactions/{id}` | Cập nhật giao dịch                    | ✅   |
| `DELETE` | `/transactions/{id}` | Xóa giao dịch                         | ✅   |

### Category — `/categories`

| Method   | Endpoint           | Mô tả              | Auth |
| -------- | ------------------ | ------------------ | ---- |
| `GET`    | `/categories`      | Danh sách danh mục | ✅   |
| `POST`   | `/categories`      | Tạo danh mục       | ✅   |
| `PUT`    | `/categories/{id}` | Cập nhật danh mục  | ✅   |
| `DELETE` | `/categories/{id}` | Xóa danh mục       | ✅   |

### Budget — `/budgets`

| Method   | Endpoint        | Mô tả               | Auth |
| -------- | --------------- | ------------------- | ---- |
| `GET`    | `/budgets`      | Danh sách ngân sách | ✅   |
| `POST`   | `/budgets`      | Tạo ngân sách       | ✅   |
| `PUT`    | `/budgets/{id}` | Cập nhật ngân sách  | ✅   |
| `DELETE` | `/budgets/{id}` | Xóa ngân sách       | ✅   |

### Goal — `/goals`

| Method   | Endpoint      | Mô tả              | Auth |
| -------- | ------------- | ------------------ | ---- |
| `GET`    | `/goals`      | Danh sách mục tiêu | ✅   |
| `POST`   | `/goals`      | Tạo mục tiêu       | ✅   |
| `PUT`    | `/goals/{id}` | Cập nhật mục tiêu  | ✅   |
| `DELETE` | `/goals/{id}` | Xóa mục tiêu       | ✅   |

### Report — `/reports`

| Method | Endpoint           | Mô tả               | Auth |
| ------ | ------------------ | ------------------- | ---- |
| `GET`  | `/reports/summary` | Tổng quan tài chính | ✅   |
| `GET`  | `/reports/monthly` | Báo cáo theo tháng  | ✅   |

### File — `/files`

| Method | Endpoint        | Mô tả          | Auth |
| ------ | --------------- | -------------- | ---- |
| `POST` | `/files/upload` | Upload hóa đơn | ❌   |

---

## Cấu trúc dự án

```
src/main/java/com/vennhuu/PersonalFinance/
├── Config/           # Security, Redis, RabbitMQ, CORS, OpenAPI
├── Controller/       # REST Controllers
├── Entity/
│   ├── Request/      # DTOs đầu vào
│   ├── Response/     # DTOs đầu ra
│   └── *.java        # JPA Entities
├── Enum/             # TransactionType, WalletType, UserStatus, RoleName
├── Exception/        # Custom exceptions + GlobalExceptionHandler
├── Repository/       # Spring Data JPA Repositories
├── Service/
│   ├── Consumer/     # RabbitMQ consumers
│   ├── Producer/     # RabbitMQ producers
│   └── *.java        # Business logic
└── Utils/            # SecurityUtil, Annotations

src/main/resources/
├── db/migration/     # Flyway SQL scripts
└── templates/        # Thymeleaf email templates
```

Thiết kế hệ thống chi tiết: [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md)

---

MIT License — © 2026 PhuocfromQuangBinh
