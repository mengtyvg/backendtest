# Mengty POS Report Project Handoff

Last updated: 2026-06-04

## 1. Project Summary

This project is a Kotlin Spring Boot backend for a small POS/reporting system. It serves REST APIs for users, menu/master data, sales, payment methods, Bakong KHQR payments, inventory stock, and reports. It also serves static HTML/CSS/JS pages from `src/main/resources/static`.

Primary package:

```text
com.mengty.report
```

Main application entry:

```text
src/main/kotlin/com/mengty/report/ReportApplication.kt
```

## 2. Technology Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.2.21 |
| Framework | Spring Boot 4.0.6 |
| Build tool | Gradle Kotlin DSL |
| Java version | Java 21 |
| Database | PostgreSQL |
| Persistence | Spring Data JPA / Hibernate |
| Auth | Custom JWT service + request filter + role interceptor |
| Password hashing | BCrypt |
| Payment integration | Bakong KHQR SDK and Bakong check transaction API |
| Rate limiting | Bucket4j |
| API docs | Springdoc OpenAPI |
| Frontend | Static HTML/CSS/JavaScript served by Spring Boot |

## 3. How To Run

From the project folder:

```powershell
cd C:\Users\MENGTY\Downloads\trestres\backendtest\report
.\gradlew.bat bootRun
```

For local profile:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Useful checks:

```powershell
.\gradlew.bat compileKotlin
.\gradlew.bat test
```

Default configured port:

```text
8010 from application.properties when PORT is not provided
8124 from application-local.properties
```

## 4. Configuration

Main config files:

| File | Purpose |
| --- | --- |
| `src/main/resources/application.properties` | Production-style config using environment variables. |
| `src/main/resources/application-local.properties` | Local database, Bakong, Telegram, and port config. |
| `src/main/resources/application-default.properties` | Default profile config. |
| `src/main/resources/banner.txt` | Spring Boot terminal startup banner. |

Important environment variables:

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | PostgreSQL JDBC URL. |
| `DB_USERNAME` | Database username. |
| `DB_PASSWORD` | Database password. |
| `PORT` | Server port. |
| `BAKONG_TOKEN` | Bakong API token. |
| `BAKONG_BASE_URL` | Bakong API base URL. |
| `BAKONG_MERCHANT_ACCOUNT` | Merchant Bakong account. |
| `BAKONG_CURRENCY` | KHQR currency, usually `USD` or `KHR`. |
| `TELEGRAM_ENABLED` | Enables/disables Telegram notifications. |
| `TELEGRAM_BOT_TOKEN` | Telegram bot token. |
| `TELEGRAM_CHAT_ID` | Telegram chat/channel ID. |
| `JWT_SECRET` | JWT signing secret. Must be at least 32 UTF-8 bytes. |
| `JWT_EXPIRATION_MINUTES` | Token lifetime in minutes. |

Security note: `application-local.properties` currently contains real-looking local secrets/tokens. Before sharing or pushing publicly, rotate those credentials and move them to environment variables.

## 5. Request Flow

```text
HTTP request
  -> RateLimitFilter
  -> JwtAuthenticationFilter for /api/** except /api/users/login and OPTIONS
  -> Controller method
  -> RoleAuthorizationInterceptor checks @RequireRoles
  -> Service
  -> Repository
  -> PostgreSQL
```

Static pages under `src/main/resources/static` are served directly by Spring Boot.

## 6. Authentication And Authorization

### Login

Users log in through:

```text
POST /api/users/login
```

The login service validates username/password using BCrypt and returns a custom JWT.

### JWT

`JwtService` creates and validates HS256 JWTs. Token payload includes:

| Claim | Meaning |
| --- | --- |
| `sub` | User UUID. |
| `username` | Username. |
| `role` | User role string. |
| `iat` | Issued-at epoch seconds. |
| `exp` | Expiration epoch seconds. |

### Auth Filter

`JwtAuthenticationFilter` checks all `/api/**` requests except:

```text
/api/users/login
OPTIONS requests
non-/api paths
```

It reads the Bearer token, validates it, loads the user from `app_user`, checks `status = true`, then stores the user on the request as:

```text
authenticatedUser
```

### Roles

Roles are defined in:

```text
src/main/kotlin/com/mengty/report/config/AppRole.kt
```

Current roles:

| Role | Intended use |
| --- | --- |
| `ADMIN` | Full admin access. |
| `MANAGER` | Management access for reports, inventory, master data, sales management. |
| `CASHIER` | Sales and cashier-facing operations. |

Controllers use:

```kotlin
@RequireRoles(AppRole.ADMIN, AppRole.MANAGER)
```

The interceptor accepts stored role strings like `ADMIN` or `ROLE_ADMIN`.

## 7. API Endpoint Map

All protected endpoints require:

```http
Authorization: Bearer <token>
```

### User API

Base path:

```text
/api/users
```

| Method | Path | Roles | Function | Service method |
| --- | --- | --- | --- | --- |
| `POST` | `/login` | Public | Login and return JWT. | `AppUserService.login` |
| `GET` | `/api/users` | `ADMIN` | List users. | `AppUserService.getUsers` |
| `POST` | `/api/users` | `ADMIN` | Create user. | `AppUserService.createUser` |

### Master Data API

Base path:

```text
/api/master
```

Class roles: `ADMIN`, `MANAGER`, `CASHIER`.

| Method | Path | Roles | Function | Service method |
| --- | --- | --- | --- | --- |
| `GET` | `/categories` | Admin, Manager, Cashier | List categories and sync category status. | `MasterService.getAllCategories` |
| `GET` | `/items` | Admin, Manager, Cashier | List all items. | `MasterService.getAllItems` |
| `GET` | `/items-with-price` | Admin, Manager, Cashier | List active menu items with variant price and stock. | `MasterService.getItemsWithPrice` |
| `GET` | `/items/{itemCode}` | Admin, Manager, Cashier | Get item by item code. | `MasterService.getItemByCode` |
| `POST` | `/items` | `ADMIN`, `MANAGER` | Create item and first variant. | `MasterService.createItem` |
| `PUT` | `/items/{itemCode}` | `ADMIN`, `MANAGER` | Update item and variant. | `MasterService.updateItem` |
| `DELETE` | `/items/{itemCode}` | `ADMIN`, `MANAGER` | Delete item. | `MasterService.deleteItem` |

### Inventory API

Base path:

```text
/api/inventory
```

Class roles: `ADMIN`, `MANAGER`.

| Method | Path | Roles | Function | Service method |
| --- | --- | --- | --- | --- |
| `GET` | `/movements` | Admin, Manager | List stock movement history. | `InventoryService.getMovements` |
| `GET` | `/balances` | Admin, Manager | List current stock balances. | `InventoryService.getBalances` |
| `POST` | `/movements` | Admin, Manager | Create stock movement and update balance. | `InventoryService.createMovement` |

Movement types:

```text
IN
OUT
```

`OUT` cannot reduce balance below zero.

### Sale API

Base path:

```text
/api/sale
```

Class roles: `ADMIN`, `MANAGER`, `CASHIER`.

| Method | Path | Roles | Function | Service method |
| --- | --- | --- | --- | --- |
| `GET` | `/payment-methods` | Admin, Manager, Cashier | List active payment methods. | `SaleService.getPaymentMethods` |
| `GET` | `/payment-methods/all` | `ADMIN`, `MANAGER` | List all payment methods including inactive. | `SaleService.getAllPaymentMethods` |
| `POST` | `/payment-methods` | `ADMIN`, `MANAGER` | Create payment method. | `SaleService.createPaymentMethod` |
| `PUT` | `/payment-methods/{id}/status` | `ADMIN`, `MANAGER` | Activate/deactivate payment method. | `SaleService.updatePaymentMethodStatus` |
| `GET` | `/orders/today` | Admin, Manager, Cashier | List today's completed/voided orders. | `SaleService.getTodayOrders` |
| `GET` | `/orders/{invoiceId}` | Admin, Manager, Cashier | Get invoice and line items. | `SaleService.getOrderDetail` |
| `POST` | `/complete` | Admin, Manager, Cashier | Complete sale, create invoice/details, reduce stock, notify Telegram. | `SaleService.completeSale` |
| `PUT` | `/orders/{invoiceId}/void` | `ADMIN` | Void completed invoice and notify Telegram. | `SaleService.voidInvoice` |

Important sale behavior:

| Behavior | Detail |
| --- | --- |
| Invoice number | Generated as `INV` plus a 7-digit increment from the max existing invoice series. |
| Sale status | New sale invoices are saved as `Completed`. |
| Stock | Each sale creates an inventory `OUT` movement. |
| Paid QR validation | If `paymentId` is provided, payment must be `PAID` and not expired. |
| Void rule | Only `Completed` invoices can be voided. |
| Refund amount | Void refund equals invoice subtotal. |

### Payment / Bakong API

Base path:

```text
/api/payments
```

Class roles: `ADMIN`, `MANAGER`, `CASHIER`.

| Method | Path | Roles | Function | Service method |
| --- | --- | --- | --- | --- |
| `POST` | `/api/payments` | Admin, Manager, Cashier | Create KHQR payment for one or more item variants. | `PaymentService.createPayment` |
| `GET` | `/api/payments` | Admin, Manager, Cashier | Return latest 100 payment records. | `PaymentService.getPaymentHistory` |
| `GET` | `/api/payments/{id}` | Admin, Manager, Cashier | Return local payment status. | `PaymentService.getPaymentStatus` |
| `GET` | `/api/payments/{id}/check` | Admin, Manager, Cashier | Check Bakong by KHQR MD5 and mark payment paid/expired. | `PaymentService.checkPayment` |

Payment statuses:

| Status | Meaning |
| --- | --- |
| `PENDING` | KHQR was created but not confirmed paid. |
| `PAID` | Bakong check returned success and transaction hash. |
| `EXPIRED` | QR expired before payment. |

Payment expiration:

```text
10 minutes after creation
```

### Reports API

Base path:

```text
/api/reports
```

Class roles: `ADMIN`, `MANAGER`.

| Method | Path | Roles | Function | Service method |
| --- | --- | --- | --- | --- |
| `GET` | `/api/reports?date=YYYY-MM-DD` | Admin, Manager | Daily report for selected date. | `ReportService.getReportByDate` |
| `GET` | `/api/reports/test` | Admin, Manager | Test DTO response for today. | `ReportService.getReportByDate` |
| `GET` | `/api/reports/sale-summary?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD&periodType=...&shift=...` | Admin, Manager | Sales summary between dates. | `ReportService.getSaleSummary` |

Report behavior:

| Method | Detail |
| --- | --- |
| `getReportByDate` | Loads completed and voided invoices for a date. Total excludes voided invoices. |
| `getSaleSummary` | Calculates gross sale, discounts, net sale, grand total, cancel count, refund amount, voided count. |

### Test Invoice API

Base path:

```text
/api/test
```

| Method | Path | Roles | Function | Service method |
| --- | --- | --- | --- | --- |
| `GET` | `/api/test` | `ADMIN` | Returns invoice DTO list. Looks like a development/test endpoint. | `PosInvoiceService.mengty` |

## 8. Controllers

| Controller | Responsibility |
| --- | --- |
| `AppUserController` | Login, user listing, user creation. |
| `MasterController` | Category and item/menu master data. |
| `InventoryController` | Stock movements and balances. |
| `SaleController` | Payment methods, complete sale, order details, void invoice. |
| `PaymentController` | KHQR creation, payment history/status, Bakong checking. |
| `ReportController` | Daily reports and sales summaries. |
| `PosInvoiceControllerTEST` | Temporary/test invoice listing endpoint. |

## 9. Services

| Service | Main methods | What it does |
| --- | --- | --- |
| `AppUserService` | `getUsers`, `login`, `createUser` | User CRUD basics, BCrypt password validation, JWT issuing. |
| `JwtService` | `createToken`, `parseAndValidate` | Creates and validates custom HS256 JWT tokens. |
| `MasterService` | `getAllCategories`, `getAllItems`, `getItemsWithPrice`, `createItem`, `updateItem`, `deleteItem` | Item/category business logic, creates/updates variants, syncs category active status based on item count. |
| `InventoryService` | `getMovements`, `getBalances`, `createMovement` | Writes stock movement rows and keeps stock balance rows updated. |
| `SaleService` | `getPaymentMethods`, `createPaymentMethod`, `completeSale`, `voidInvoice`, `getTodayOrders`, `getOrderDetail` | Main POS sale logic. Creates invoice/detail rows, checks payment, reduces stock, sends Telegram notifications. |
| `PaymentService` | `createPayment`, `getPaymentStatus`, `getPaymentHistory`, `checkPayment` | Generates Bakong KHQR, stores payment rows, checks Bakong transaction status. |
| `ReportService` | `getReportByDate`, `getSaleSummary` | Aggregates invoice data for reports. |
| `PosInvoiceService` | `mengty` | Maps all invoices to a small DTO for test endpoint. |
| `TelegramNotificationService` | `notifyOrderCompleted`, `notifyInvoiceVoided` | Sends HTML Telegram messages when enabled. |

## 10. Repositories

| Repository | Entity | Important queries/methods |
| --- | --- | --- |
| `AppUserRepository` | `AppUser` | `findByUsernameAndStatusTrue`, `existsByUsernameIgnoreCase`. |
| `CategoryRepository` | `Category` | Basic JPA CRUD. |
| `ItemRepository` | `Item` | `findByItemCode`, `findByItemCodeAndStatusTrue`, `countByCategoryId`, `findDefaultAppearanceId`, native `getItemsWithPrice`. |
| `ItemVariantRepository` | `ItemVariant` | `findFirstByItemId`, `findFirstActiveSaleVariantByItemId`. |
| `InventoryStockMovementRepository` | `InventoryStockMovementModel` | Finds movements by item code or variant sorted by date. |
| `InventoryStockBalanceRepository` | `InventoryStockBalanceModel` | Finds balance by variant or item code. |
| `PaymentMethodRepository` | `PosPaymentMethod` | Active/all payment methods, duplicate payment subtype check. |
| `PaymentRepository` | `Payment` | Latest payment history by created date. |
| `BakongRepository` | `Payment` | Finds payment by KHQR MD5. Currently separate from `PaymentRepository` and may be redundant. |
| `PosInvoiceRepository` | `PosInvoice` | Date report query, sale summary query, last invoice number query. |
| `PosInvoiceDetailRepository` | `PosInvoiceDetail` | Finds invoice details ordered by creation time. |
| `PosInvoiceRepositoryTest` | `PosInvoice` | Basic JPA repository, appears unused/test-only. |

## 11. Main Database Models

| Model | Table | Purpose |
| --- | --- | --- |
| `AppUser` | `app_user` | Login user with username, password hash, display name, role, default page, status. |
| `Category` | `category` | Product/menu category. |
| `Item` | `item` | Product/menu item. Includes code, name, image URL, category, rates, stock opening, active/menu flags. |
| `ItemVariant` | `item_variants` | Sellable item variant with price, cost, SKU, active/status flags. |
| `InventoryStockMovementModel` | `inventory_stock_movement` | Stock movement ledger row. |
| `InventoryStockBalanceModel` | `inventory_stock_balance` | Current stock quantity per item variant. |
| `PosPaymentMethod` | `pos_payment_method` | Payment method names and active status. |
| `Payment` | `payments` | Bakong KHQR payment record, MD5, status, hash, expiration. |
| `PosInvoice` | `pos_invoice` | Sale invoice header. Includes status, total, subtotal, post date, void metadata, refund amount. |
| `PosInvoiceDetail` | `pos_invoice_detail` | Sale invoice line item detail. |

## 12. DTOs / Request And Response Objects

| DTO file | Purpose |
| --- | --- |
| `AppUserDTO.kt` | User response, create user request, login request/response. |
| `ItemRequest.kt` | Create/update item payload. |
| `ItemWithPriceDTO.kt` | Menu item response including variant price and stock quantity. |
| `PaymentDTO.kt` | Bakong payment create/status/check/history payloads. |
| `PaymentMethodDTO.kt` | Payment method create/status update payloads. |
| `PosInvoiceDTO.kt` | Small invoice response for test endpoint. |
| `ReportDTO.kt` | Daily report, sale summary, and test report responses. |
| `SaleCompleteDTO.kt` | Complete sale, void invoice, order detail payloads. |
| `StockMovementDTO.kt` | Stock movement and stock balance payloads. |

## 13. Static Frontend Files

Static files are served from:

```text
src/main/resources/static
```

| File | Purpose |
| --- | --- |
| `index.html` | Basic static landing/redirect page. |
| `login.html` | Login UI. |
| `category.html` | Category/menu management page. |
| `item.html` | Item management page. |
| `sale.html` | POS sale screen. |
| `salesummary.html` | Sales summary report page. |
| `report.html` | Daily report page. |
| `stock.html` | Inventory stock page. |
| `users.html` | User management page. |
| `payment-method.html` | Payment method management page. |
| `payment-history.html` | Payment history page. |
| `css/app-theme.css` | Shared app styling. |
| `css/sale.css` | Sale screen styling. |
| `js/auth.js` | Frontend auth helper, token storage/session logic. |
| `js/sale.js` | POS sale screen behavior. |

## 14. Important Business Rules

| Area | Rule |
| --- | --- |
| User login | User must exist and `status = true`; password must match BCrypt hash. |
| JWT | Secret must be at least 32 UTF-8 bytes; expired tokens are rejected. |
| Roles | Controller access is controlled by `@RequireRoles`. |
| Items | Creating an item also creates an item variant. |
| Categories | Category status is synchronized based on whether it has items. |
| Inventory | Stock movement quantity must be greater than zero. |
| Inventory | Movement type must be `IN` or `OUT`. |
| Inventory | `OUT` movement cannot make stock negative. |
| Sale completion | Sale must contain at least one item. |
| Sale completion | Payment method must exist and be active. |
| Sale completion | Optional Bakong payment must be `PAID` and not expired. |
| Sale completion | Each sold item must exist, be active, and have an active variant. |
| Sale completion | Completing sale creates invoice, invoice details, stock OUT movement, and Telegram message. |
| Void invoice | Only completed invoices can be voided. |
| Void invoice | Void reason is required. |
| Void invoice | Refund amount is set to invoice subtotal. |
| Bakong payment | KHQR expires after 10 minutes. |
| Telegram | Notifications are skipped if disabled or credentials are blank. |

## 15. Rate Limiting

Implemented in:

```text
src/main/kotlin/com/mengty/report/config/RateLimitFilter.kt
```

Limits are per IP and route group:

| Route group | Limit per minute |
| --- | --- |
| `/api/payments` | 30 |
| `/api/sale/complete` | 10 |
| Any URI containing `/void` | 5 |
| `/api/master` | 60 |
| General | 120 |

## 16. OpenAPI

OpenAPI metadata is configured in:

```text
src/main/kotlin/com/mengty/report/config/OpenApiConfig.kt
```

Expected Swagger UI path in Springdoc projects:

```text
/swagger-ui/index.html
```

## 17. Known Risks And Cleanup Tasks

| Priority | Item | Why it matters |
| --- | --- | --- |
| High | Move real secrets out of `application-local.properties`. | Prevent leaked database, Bakong, and Telegram credentials. |
| High | Confirm role values in existing database. | Stored user roles must match `ADMIN`, `MANAGER`, `CASHIER`, or `ROLE_*` versions. |
| High | Add a real Spring Security config or document why custom auth is preferred. | Current auth is custom and simple; future devs need a clear security direction. |
| Medium | Remove or rename `PosInvoiceControllerTEST` and `PosInvoiceRepositoryTest` if no longer needed. | Test endpoints can confuse production API users. |
| Medium | Validate totals server-side in `SaleService.completeSale`. | Current request provides line totals and invoice total; server should verify arithmetic to prevent tampering. |
| Medium | Add database migrations. | `ddl-auto=update` is convenient locally but risky for production schema changes. |
| Medium | Normalize money types. | Some models use `Double`, others use `BigDecimal`; money should prefer `BigDecimal`. |
| Medium | Add API tests for auth/roles and sale completion. | These flows affect business correctness and security. |
| Low | Clean unused imports and formatting. | Several files have duplicate or loose imports. |
| Low | Decide whether `BakongRepository` is needed. | It overlaps with `PaymentRepository`. |

## 18. Suggested Next Developer Workflow

1. Start with `.\gradlew.bat test` and confirm the baseline is green.
2. Configure local PostgreSQL and verify `application-local.properties` points to the right database.
3. Create or verify admin user in `app_user`.
4. Login with `POST /api/users/login`.
5. Use returned JWT as `Authorization: Bearer <token>`.
6. Test master item APIs before sale APIs, because sale depends on active items and variants.
7. Test inventory balance before and after sale completion.
8. Test Bakong payment flow separately from regular sale completion.
9. Rotate and remove any committed real credentials before sharing the project.

## 19. Quick API Examples

### Login

```http
POST /api/users/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

### Create Stock Movement

```http
POST /api/inventory/movements
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemVariantId": "00000000-0000-0000-0000-000000000000",
  "itemCode": "ITEM001",
  "movementType": "IN",
  "quantity": 10,
  "unitCost": 1.50,
  "referenceNo": "OPENING",
  "movementDate": "2026-06-04",
  "note": "Opening stock",
  "createdBy": "admin"
}
```

### Complete Sale

```http
POST /api/sale/complete
Authorization: Bearer <token>
Content-Type: application/json

{
  "paymentMethodId": "00000000-0000-0000-0000-000000000000",
  "total": 4.50,
  "items": [
    {
      "itemCode": "ITEM001",
      "itemName": "Example Item",
      "unitPrice": 1.50,
      "qty": 3,
      "total": 4.50
    }
  ]
}
```

### Create Bakong Payment

```http
POST /api/payments
Authorization: Bearer <token>
Content-Type: application/json

{
  "items": [
    {
      "itemVariantId": "00000000-0000-0000-0000-000000000000",
      "qty": 2
    }
  ]
}
```

### Check Bakong Payment

```http
GET /api/payments/{paymentId}/check
Authorization: Bearer <token>
```

## 20. Mental Model For The Project

```text
Users log in
  -> frontend stores JWT
  -> frontend calls /api/master to load menu
  -> cashier completes sale through /api/sale/complete
  -> backend creates invoice and invoice details
  -> backend writes inventory OUT movement
  -> backend sends Telegram notification
  -> reports read invoice tables for daily and summary totals

Bakong QR flow
  -> frontend creates payment through /api/payments
  -> backend generates KHQR and stores PENDING payment
  -> frontend checks /api/payments/{id}/check
  -> backend calls Bakong by MD5
  -> if paid, backend marks payment PAID
  -> sale can reference paymentId when completing
```
