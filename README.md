# UTEPet Shop & Care

Ứng dụng cửa hàng thú cưng và dịch vụ chăm sóc: bán sản phẩm, quản lý tồn kho theo chi nhánh, giỏ hàng và đơn hàng, đặt lịch dịch vụ, hồ sơ khách hàng, thông báo và quản lý vận hành.

## Công nghệ

- Java 21, Maven, Spring Boot 3.4.3.
- Spring MVC, Thymeleaf, Bootstrap, Spring Security và JWT.
- Spring Data JPA, Hibernate và MySQL.
- WebSocket/STOMP cho thông báo, SMTP cho email OTP và Cloudinary cho ảnh.

## 1. Chuẩn bị môi trường

1. Cài **JDK 21**, Maven và MySQL Server 8 nếu muốn lưu dữ liệu bằng MySQL. MySQL Workbench là công cụ quản lý, không thay thế MySQL Server.
2. Mở PowerShell và kiểm tra:

   ```powershell
   java -version
   mvn -version
   ```

3. Tải source và giải nén hoặc clone repository. Di chuyển vào **thư mục có file `pom.xml`**:

   ```powershell
   cd "DUONG-DAN-DEN-THU-MUC-CHUA-POM.XML"
   dir pom.xml
   ```

   Khi tải ZIP từ GitHub, thư mục có thể lồng hai cấp cùng tên. Nếu `dir pom.xml` báo không tìm thấy, hãy vào thư mục con chứa file đó trước khi chạy Maven.

## 2. Cấu hình MySQL trên máy cá nhân

1. Khởi động MySQL Server. Profile `mysql` mặc định dùng `localhost:3306`, database `utepet_shop` và tài khoản `root`. JDBC URL có `createDatabaseIfNotExist=true`; tài khoản cần có quyền tạo database. Nếu không có quyền này, tạo database trước trong Workbench bằng tài khoản có quyền:

   ```sql
   CREATE DATABASE IF NOT EXISTS utepet_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Trong PowerShell đang mở tại thư mục chứa `pom.xml`, khai báo mật khẩu MySQL của **máy bạn**, sau đó chạy:

   ```powershell
   $env:MYSQL_PASSWORD = "MAT-KHAU-MYSQL-CUA-BAN"
   mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
   ```

   Biến môi trường chỉ tồn tại trong cửa sổ PowerShell hiện tại. Mở cửa sổ mới thì cần khai báo lại. Đây là mật khẩu MySQL, không phải mật khẩu đăng nhập website.

3. Chờ ứng dụng khởi động, sau đó mở [http://localhost:8080/products](http://localhost:8080/products). Giữ PowerShell mở khi sử dụng và nhấn `Ctrl+C` để dừng. Trong Workbench, kết nối đến cùng server, làm mới mục **Schemas**, mở `utepet_shop` và mở **Tables** để xem các bảng.

Profile MySQL đặt `spring.jpa.hibernate.ddl-auto=update`: Hibernate cập nhật cấu trúc bảng và dữ liệu được giữ lại giữa các lần chạy. Các cài đặt mặc định nằm trong `src/main/resources/application-mysql.yml`. Có thể ghi đè bằng biến môi trường Spring Boot khi máy dùng tài khoản, cổng hoặc server khác:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/utepet_shop?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:SPRING_DATASOURCE_USERNAME = "TEN-TAI-KHOAN-MYSQL"
$env:SPRING_DATASOURCE_PASSWORD = "MAT-KHAU-MYSQL"
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Không lưu mật khẩu thật vào README hoặc commit lên GitHub.

## 3. Tài khoản demo và vai trò

Nếu bảng người dùng đang trống, `DataInitializer` tạo dữ liệu mẫu. Các tài khoản ban đầu dùng mật khẩu **`Password@123`**:

| Tài khoản | Vai trò | Khu vực sử dụng |
| --- | --- | --- |
| `customer@utepet.vn` | CUSTOMER | Cửa hàng, giỏ hàng, đơn hàng, thú cưng, lịch hẹn và hồ sơ cá nhân |
| `staff@utepet.vn` | STAFF | Xử lý đơn hàng và lịch hẹn tại khu vực nhân viên |
| `manager@utepet.vn` | MANAGER | Quản lý sản phẩm, kho, dịch vụ, khung giờ, khuyến mãi và dashboard |
| `admin@utepet.vn` | ADMIN | Các chức năng được cấp cho quản lý và vận hành trong cấu hình hiện tại |

Nếu database đã có người dùng, bộ khởi tạo bỏ qua dữ liệu mẫu; không mặc định cho rằng tài khoản hoặc mật khẩu demo sẽ được tạo lại. Đây là tài khoản thử nghiệm, không sử dụng nguyên trạng khi đưa hệ thống ra môi trường thật.

Một số đường dẫn chính: `/login`, `/register`, `/products`, `/services`, `/cart`, `/orders`, `/account/profile`, `/pets`, `/bookings`, `/staff/orders`, `/staff/bookings`, `/manager/dashboard` và `/manager/products`.

## 4. Cấu hình JWT cho API

Ứng dụng có thể chạy cục bộ khi không đặt `JWT_SECRET`; khi đó khóa được sinh ngẫu nhiên mỗi lần khởi động và token cũ không còn hợp lệ sau khi khởi động lại. Để sử dụng khóa cố định, tạo chuỗi Base64 từ ít nhất 32 byte ngẫu nhiên và khai báo trước khi chạy:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Lưu khóa trong nơi quản lý bí mật của môi trường triển khai để sử dụng lại khi khởi động. Không đưa khóa thật lên GitHub. JWT trong API được gửi bằng header `Authorization: Bearer <token>`; đăng nhập qua giao diện web dùng phiên đăng nhập.

## 5. Email OTP

Đăng ký và quên mật khẩu có bước gửi email OTP. Cấu hình SMTP trong `src/main/resources/application.yml`; nên ghi đè thông tin tài khoản qua biến môi trường thay vì đưa thông tin thật vào source:

```powershell
$env:SPRING_MAIL_HOST = "smtp.gmail.com"
$env:SPRING_MAIL_PORT = "587"
$env:SPRING_MAIL_USERNAME = "EMAIL-GUI-THU-CUA-BAN"
$env:SPRING_MAIL_PASSWORD = "MAT-KHAU-UNG-DUNG-CUA-BAN"
```

Cấu hình Gmail cần bật xác thực SMTP và STARTTLS như trong ứng dụng; dùng mật khẩu ứng dụng phù hợp với tài khoản email. Khởi động lại ứng dụng sau khi thay đổi cấu hình. Nếu không cấu hình SMTP hợp lệ, việc gửi OTP qua email có thể thất bại.

**Lưu ý:** `application.yml` trong phiên bản hiện tại có thông tin SMTP được ghi trực tiếp. Chủ sở hữu tài khoản cần thu hồi thông tin xác thực đã lộ và thay bằng cấu hình an toàn; chỉ đặt biến môi trường không xóa bí mật đã có trong lịch sử Git.

## 6. Cloudinary và WebSocket

Tính năng tải ảnh cần cấu hình tài khoản Cloudinary. Khai báo các giá trị do Cloudinary cung cấp trong PowerShell trước khi khởi động:

```powershell
$env:CLOUDINARY_CLOUD_NAME = "CLOUD-NAME"
$env:CLOUDINARY_API_KEY = "API-KEY"
$env:CLOUDINARY_API_SECRET = "API-SECRET"
```

Không đăng thông tin thật. Khi chưa cấu hình, tính năng tải ảnh lên Cloudinary không sử dụng được; các trang không phụ thuộc vào việc tải ảnh vẫn có thể hoạt động.

Thông báo realtime sử dụng WebSocket/STOMP với endpoint `/ws`. Để thử, mở trang của người nhận, đăng nhập tài khoản phù hợp và thực hiện thao tác tạo thông báo từ phiên khác. Nếu triển khai qua reverse proxy, cần cho phép nâng cấp kết nối WebSocket.

## 7. Kiểm tra API và chương trình

Chạy test trong thư mục dự án:

```powershell
mvn test
```

Ví dụ đăng nhập API: `POST /api/auth/login` với body JSON:

```json
{
  "email": "customer@utepet.vn",
  "password": "Password@123"
}
```

Nếu dùng tài khoản demo hợp lệ, lấy token từ phản hồi và gửi kèm header Bearer để thử `GET /api/orders`. `GET /api/products` là API công khai; các API quản lý yêu cầu vai trò phù hợp. Có thể import `postman/UTEPet_Phan1.postman_collection.json` để thử các request mẫu. Tài liệu cơ sở dữ liệu và tích hợp nằm trong `docs/`.

## 8. Đóng gói và triển khai

```powershell
mvn clean package
java -jar target/utepet-shop-1.0.0.jar --spring.profiles.active=mysql
```

Trước khi chạy JAR, cấu hình các biến MySQL, JWT, SMTP và Cloudinary theo môi trường thực tế. Kiểm tra tên file trong `target` nếu phiên bản dự án thay đổi. Với server thật, cần cấu hình HTTPS, kết nối cơ sở dữ liệu an toàn, sao lưu và quản lý bí mật; cấu hình MySQL localhost mặc định chỉ phù hợp để thử trên máy cá nhân. Việc đóng gói thành công không tự chứng minh hệ thống đã sẵn sàng đưa vào vận hành công khai.

## 9. Lỗi thường gặp

- **`No plugin found for prefix 'spring-boot'`:** kiểm tra đang đứng trong thư mục chứa `pom.xml`, sau đó chạy lại lệnh Maven.
- **`Access denied` hoặc không kết nối được MySQL:** kiểm tra MySQL Server đang chạy, cổng, tài khoản, mật khẩu và quyền tạo/truy cập database. Nếu mở PowerShell mới, khai báo lại `MYSQL_PASSWORD`.
- **Cổng 8080 đang được sử dụng:** dừng phiên ứng dụng cũ hoặc chạy với cổng khác, ví dụ `mvn spring-boot:run "-Dspring-boot.run.profiles=mysql" "-Dspring-boot.run.arguments=--server.port=8081"`.
- **Không nhận OTP:** kiểm tra cấu hình SMTP, xác thực tài khoản gửi, mạng và thông báo lỗi trong log ứng dụng.
- **Không đăng nhập được tài khoản demo:** kiểm tra ứng dụng có kết nối đúng database hay không; bộ khởi tạo chỉ thêm dữ liệu mẫu khi bảng người dùng trống.
- **`BUILD FAILURE`:** tìm dòng lỗi đầu tiên và phần `Caused by` phía trên log, thay vì chỉ xem thông báo Maven ở cuối.

## Cấu trúc chính

```text
src/main/java/com/utepetshop/
  config/       Cấu hình ứng dụng, bảo mật, WebSocket và dữ liệu mẫu
  controller/   Các controller web và REST API
  dto/          Dữ liệu request và response
  entity/       Các thực thể cơ sở dữ liệu
  repository/   Truy cập dữ liệu
  security/     JWT và thông tin xác thực
  service/      Xử lý nghiệp vụ
src/main/resources/
  templates/    Các trang Thymeleaf
  static/       Tài nguyên giao diện
  application.yml và application-mysql.yml
src/test/       Kiểm thử tự động
docs/           Tài liệu dự án
postman/        Bộ request API mẫu
```
