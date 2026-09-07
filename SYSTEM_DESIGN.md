# System Design — Personal Finance API

---

## 1. Tổng quan

REST API backend cho ứng dụng quản lý tài chính cá nhân. Người dùng theo dõi thu/chi, phân bổ ngân sách theo danh mục, đặt mục tiêu tiết kiệm và xem báo cáo.

| Tiêu chí | Giải pháp |
|---|---|
| Bảo mật | JWT stateless, refresh token rotation, HttpOnly cookie |
| Hiệu năng | Redis cache cho dữ liệu hay truy vấn |
| Async | RabbitMQ — gửi email không block request |
| Migration | Flyway, version-controlled |
| Test | JaCoCo ≥ 80% line coverage |
| API docs | Swagger UI |

---

## 2. Kiến trúc

### Luồng tổng thể

```
Client (HTTP)
    │
    ▼
SecurityFilterChain  ──── JWT validation
    │
    ▼
Controller  ──── @Valid (Bean Validation)
    │
    ▼
Service  ──── SecurityUtil.getCurrentUserLogin()
    │               │                   │
    ▼               ▼                   ▼
Repository      Redis Cache      RabbitMQ Producer
    │                                   │
    ▼                                   ▼
PostgreSQL                       RabbitMQ Consumer
                                        │
                                        ▼
                                  EmailService (SMTP)
```

Mọi response (thành công lẫn lỗi) đều qua `GlobalExceptionHandler` và trả về cấu trúc chuẩn.

---

## 3. Data Model

### ERD

```
┌──────────┐     ┌──────────────┐     ┌───────────────┐
│   Role   │◄────│     User     │────▶│ RefreshToken  │
│ id, name │     │ id, email    │     │ token         │
└──────────┘     │ password     │     │ expiryDate    │
                 │ status, otp  │     └───────────────┘
                 └──────┬───────┘
                        │ 1:N
          ┌─────────────┼──────────────┬─────────────┐
          ▼             ▼              ▼             ▼
      ┌────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐
      │ Wallet │  │ Category │  │  Budget  │  │  Goal   │
      └───┬────┘  └────┬─────┘  └──────────┘  └─────────┘
          │             │
          └──────┬──────┘
                 ▼
          ┌─────────────────┐
          │   Transaction   │
          │ type, amount    │
          │ transactionDate │
          │ receiptUrl      │
          └─────────────────┘
```

### Mô tả bảng

| Bảng | Trường đáng chú ý |
|---|---|
| `users` | `email` (unique), `status` (ACTIVE/INACTIVE), `otpCode` |
| `roles` | `name` (ADMIN/USER) |
| `refresh_tokens` | `token`, `expiryDate` |
| `wallets` | `type` (CASH/BANK/E_WALLET/CREDIT_CARD), `money` |
| `categories` | `type` (INCOME/EXPENSE) |
| `transactions` | `type`, `amount`, `transactionDate`, `receiptUrl` |
| `budgets` | `amountLimit`, `startDate`, `endDate` |
| `goals` | `targetAmount`, `currentAmount`, `deadline` |

Tất cả entities kế thừa `BaseEntity` với `createdAt` và `updatedAt`.

---

## 4. Bảo mật

### Luồng đăng nhập

```
POST /auth/login
    │
    ├── Xác thực credentials (BCrypt)
    ├── Tạo Access Token  (JWT HS512, expire = JWT_ACCESS_EXPIRE)
    ├── Tạo Refresh Token (UUID, lưu DB, expire = JWT_REFRESH_EXPIRE)
    └── Set refresh_token → HttpOnly + Secure Cookie

Mỗi request tiếp theo:
    Authorization: Bearer <access_token>
    → JwtDecoder → SecurityContext → UserDetails
```

### Refresh token rotation

```
GET /auth/refresh
    ├── Đọc refresh_token từ Cookie
    ├── Validate + xóa token cũ khỏi DB
    ├── Tạo Access Token mới
    ├── Tạo Refresh Token mới → lưu DB
    └── Set Cookie mới
```

### Phân quyền

| Endpoint | Quyền |
|---|---|
| `/auth/login`, `/register`, `/send-otp`, `/reset-password` | Public |
| `/auth/refresh` | Public (cookie) |
| `/v3/api-docs/**`, `/swagger-ui/**` | Public |
| `/storage/**`, `/api/v1/files/**` | Public |
| Tất cả còn lại | Authenticated (JWT) |

### Data isolation

Service dùng `SecurityUtil.getCurrentUserLogin()` để lấy email từ JWT và filter toàn bộ query theo `user_id` — người dùng chỉ thấy dữ liệu của mình.

---

## 5. Response & Exception

### Cấu trúc response chuẩn

```json
// Thành công
{ "status": 200, "message": "Login account", "error": null, "data": { ... } }

// Lỗi
{ "status": 400, "message": "Email không được để trống", "error": "Bad Request", "data": null }
```

### Exception mapping

| Exception | HTTP |
|---|---|
| `BadRequestException`, `IdInvalidException` | 400 |
| `MethodArgumentNotValidException` | 400 |
| `StorageException` | 400 |
| `UsernameNotFoundException`, `BadCredentialsException` | 401 |
| `ForbiddenException` | 403 |
| `ResourceNotFoundException` | 404 |
| `ExistsEmailException`, `ExistsPhoneNumberException` | 409 |
| `Exception` (generic) | 500 |

---

## 6. Async — RabbitMQ

Dùng để gửi OTP email mà không block request.

```
POST /auth/send-otp
    │
    ├── Tạo OTP 6 chữ số (expire 5 phút) → lưu vào User
    └── RabbitMQ Producer ──▶ Queue: email.otp
                                    │
                               Consumer → EmailService
                                    │
                              Thymeleaf template → SMTP
```

---

## 7. Caching — Redis

Dùng Spring Cache (`@Cacheable`, `@CacheEvict`) cho các dữ liệu hay lặp lại như danh mục, danh sách ví.

---

## 8. File Upload

- Lưu tại `./uploads/` (Docker volume)
- URL public: `{UPLOAD_BASE_URI}/storage/{filename}`
- `receiptUrl` trong Transaction trỏ đến URL này

---

## 9. Database Migration & Initialization

### Migration — Flyway

Scripts đặt tại `src/main/resources/db/migration/`:

```
V1__create_roles_users.sql
V2__create_wallets.sql
V3__create_categories.sql
V4__create_transactions.sql
V5__create_budgets_goals.sql
...
```

### Khởi tạo dữ liệu mặc định (DatabaseInit)

Lớp `DatabaseInit` lắng nghe sự kiện `ApplicationReadyEvent` khi server khởi động:

- **Roles**: Nếu bảng `roles` trống, tự động tạo 2 vai trò `ROLE_ADMIN` và `ROLE_USER`.
- **Admin account**: Nếu bảng `users` trống, tự động tạo tài khoản quản trị viên:
  - Email: lấy từ `EMAIL_USERNAME` (`spring.mail.username`)
  - Password: mã hóa BCrypt từ `DEFAULT_ADMIN_PASSWORD` (`app.default-admin.password`, mặc định `admin123`)
  - Role: `ROLE_ADMIN`
- Tự động bỏ qua nếu cơ sở dữ liệu đã có người dùng.

---

## 10. Docker Compose

| Service | Image | Port |
|---|---|---|
| `postgres` | `postgres:16-alpine` | `5432` |
| `redis` | `redis:7-alpine` | `6379` |
| `rabbitmq` | `rabbitmq:3-management-alpine` | `5672`, `15672` |
| `backend` | `./Dockerfile` | `8080` |

Backend chỉ khởi động sau khi cả ba service infra đều healthy (`depends_on: condition: service_healthy`).

Dockerfile dùng multi-stage build: Maven build → JRE 21 slim runtime.

---

## 11. Testing

| Loại | Công cụ | Phạm vi |
|---|---|---|
| Unit | JUnit 5 + Mockito | Service layer |
| Integration | Spring Boot Test | Controller + Repository |
| Coverage | JaCoCo | ≥ 80% line (loại trừ Entity, Config, Exception, Enum) |

```bash
./mvnw verify
# Report: target/site/jacoco/index.html
```

---

## 12. Monitoring

| Công cụ | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ Management | http://localhost:15672 |
| JaCoCo Report | `target/site/jacoco/index.html` |
