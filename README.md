# UTEPet Shop & Care

MVP Spring Boot cho phần 1: catalog, RBAC, giỏ hàng, checkout trừ tồn kho theo chi nhánh, order processing và JWT API.

## Chạy dự án

```powershell
$env:JWT_SECRET = "your-base64-encoded-32-byte-secret"
mvn spring-boot:run
```

Mở `http://localhost:8080/products`.

Tài khoản demo (cùng mật khẩu `Password@123`):

- `customer@utepet.vn`
- `staff@utepet.vn`
- `manager@utepet.vn`
- `admin@utepet.vn`

Mặc định app dùng H2 in-memory để demo. Muốn dùng MySQL, tạo database rồi chạy:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Hãy sửa `spring.datasource.username` và `spring.datasource.password` trong `src/main/resources/application-mysql.yml` trước khi chạy profile này.

## REST API demo

`POST /api/auth/login`

```json
{"email":"customer@utepet.vn","password":"Password@123"}
```

Sao chép `token` trả về và gửi header `Authorization: Bearer <token>` cho `GET /api/orders`.

- `GET /api/products` công khai
- `GET /api/orders` yêu cầu đăng nhập/JWT
- `GET /api/admin/products` yêu cầu MANAGER hoặc ADMIN

## Tài liệu và Postman

- ERD, contract tích hợp với Phần 2 và cách kiểm tra MySQL: `docs/PHAN1_ERD_VA_TICH_HOP.md`.
- Import `postman/UTEPet_Phan1.postman_collection.json` vào Postman để kiểm tra JWT/role.

## Quy tắc tồn kho

Checkout chạy trong `@Transactional`; mỗi bản ghi Inventory được lấy bằng `PESSIMISTIC_WRITE`, kiểm tra lại số lượng, trừ tồn kho và tạo Order trong cùng transaction. Bất cứ lỗi nào sẽ rollback cả đơn lẫn tồn kho.
