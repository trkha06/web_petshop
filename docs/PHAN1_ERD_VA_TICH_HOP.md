# UTEPet Phan 1 Commerce Security Inventory

## ERD phan 1

```text
users --< addresses
users >--< roles (user_roles)
users --1 carts --< cart_items >-- products
users --< orders --< order_items >-- products
categories --< products >-- brands
products --< product_images
products --< inventories >-- branches
inventories --< inventory_transactions
branches --< orders
```

## Contract cho Phan 2

- PK dung `Long`; table dung snake_case; field Java dung camelCase.
- Enum khong doi ten: `RoleName`, `OrderStatus`, `BookingStatus`, `PaymentMethod`, `DeliveryType`, `PetType`, `DiscountType`.
- Phan 2 chi lien ket den `User`, `Product`, `Branch`, `CustomerOrder` va khong sua relationship/field cua cac entity nay.
- Flow browser: Thymeleaf Controller -> Service (`@Transactional`) -> Repository/JPA -> MySQL; REST dung DTO va JWT filter truoc Controller.
- Checkout lock Inventory bang `PESSIMISTIC_WRITE`, kiem tra lai so luong, tao Order/OrderItem va tru kho trong mot transaction. Loi bat ky se rollback.

## Kiem tra MySQL

Chay profile `mysql`, mo DBeaver/MySQL Workbench va kiem tra: `users`, `products`, `inventories`, `inventory_transactions`, `orders`, `order_items`. Sau checkout, quantity phai giam va co dong `SALE`; sau huy PENDING, quantity tang va co dong `CANCEL_ORDER`.
