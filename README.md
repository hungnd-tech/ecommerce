# E-commerce API

Backend e-commerce API bằng Spring Boot — dự án ôn tập, tập trung vào các phần dễ vỡ trong thực tế (concurrency, cache,
xử lý async) hơn là CRUD đơn thuần.

## Tech stack

Java 21 · Spring Boot 4.1 (MVC, Data JPA, Security) · MySQL · Redis · Kafka · JWT · JUnit 5 + Mockito · Docker Compose ·
GitHub Actions

## Tính năng

- **Auth**: đăng ký/đăng nhập bằng JWT, phân quyền `CUSTOMER`/`ADMIN`.
- **Product**: CRUD, đa hình `PhysicalProduct`/`DigitalProduct`, gán category N-N, soft delete, optimistic lock khi
  update.
- **Cart**: giỏ hàng theo user, giá luôn lấy real-time từ product.
- **Order**: checkout khoá tồn kho (pessimistic lock) chống bán quá số lượng, đóng băng giá tại thời điểm mua, state
  machine `PENDING → PAID → SHIPPED → COMPLETED` (hoặc `CANCELLED`, hoàn lại tồn kho).
- **Payment**: mock, xử lý async qua Kafka consumer riêng, độc lập với consumer gửi notification.
- **Cache**: Redis cho danh sách/chi tiết sản phẩm, evict đúng key khi ghi.

## Điểm kỹ thuật đáng chú ý

- Checkout dùng **pessimistic lock** (không dùng optimistic) vì đây là thao tác kho hàng cần đúng tuyệt đối ngay lúc
  đọc; mọi lock đều khoá theo 1 thứ tự cố định (`productId`) để loại bỏ hoàn toàn deadlock.
- Chống **double-submit checkout**: khoá luôn `cart_item` trước khi xử lý, request trùng bị chặn tới khi request đầu
  commit xong.
- State machine của Order có **guard 2 chiều** — chặn được cả trường hợp khách huỷ đơn đúng lúc Payment consumer đang xử
  lý cùng đơn đó.
- Kafka event chỉ publish **sau khi transaction DB đã commit** (`AFTER_COMMIT`), tránh rollback oan hoặc gửi event cho
  order không tồn tại.
- Exception xử lý tập trung, trả về đúng chuẩn `ProblemDetail` (RFC 7807) cho mọi loại lỗi, kể cả lỗi phân quyền.

## API chính

Tất cả có prefix `/api`. `GET /products` và `/categories` public, còn lại cần JWT (`Authorization: Bearer <token>`).

| Method              | Endpoint                                     | Quyền               |
|---------------------|----------------------------------------------|---------------------|
| POST                | `/auth/register`, `/auth/login`              | Public              |
| GET                 | `/auth/me`                                   | Đăng nhập           |
| GET                 | `/products`, `/products/{id}`, `/categories` | Public              |
| POST/PUT/DELETE     | `/products/{id}`                             | Admin               |
| GET/POST/PUT/DELETE | `/cart`, `/cart/items/{productId}`           | Đăng nhập           |
| POST                | `/orders/checkout`                           | Đăng nhập           |
| GET                 | `/orders`, `/orders/{id}`                    | Đăng nhập           |
| PUT                 | `/orders/{id}/cancel`                        | Đăng nhập (chủ đơn) |
| PUT                 | `/orders/{id}/ship`, `/{id}/complete`        | Admin               |

## Chạy thử

```bash
cp .env.example .env   # điền giá trị thật
docker compose up -d --build
```

App chạy ở `http://localhost:8080`, health check `GET /api/health`, Kafka UI ở `http://localhost:8090`.

## Test

Unit test (Service, mock Repository) + `@DataJpaTest` chạy trên MySQL thật cho các query cần pessimistic lock. Kịch bản
test concurrency (race, deadlock, double-submit) chạy bằng `curl` song song

## Trạng thái

Đã hoàn thành đủ 5 module chính (auth/product/cart/order/payment), cache Redis, xử lý async Kafka, test đầy đủ tầng
Service, Dockerize, CI chặn merge khi test fail. Phần deploy lên VM đã có kế hoạch, chưa triển khai.
