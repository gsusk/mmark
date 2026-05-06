# Minimarket - Documentacion basica del sstema

## CRUD Relacional de Productos

La gestión de productos se realiza de forma relacional utilizando JPA y una base de datos MySQL. A continuación se
detallan los componentes principales:

### Modelo de Datos (Relacional)

- **Entidad `Product`**: Almacena la información básica como nombre, descripción, precio, marca y stock.
- **Entidad `Category`**: Cada producto pertenece a una categoría que define sus reglas de validación.
- **Entidad `Image`**: Relación 1:N con `Product`. Permite almacenar múltiples URLs de imágenes para un mismo producto.
- **Atributos Dinámicos**: Los productos almacenan especificaciones técnicas en formato JSON (columna `attributes`), lo
  que permite flexibilidad para diferentes tipos de productos (ej. talla para ropa, RAM para electrónica).

### Operaciones CRUD

- **Crear (`POST /products`)**: Crea un nuevo producto, valida sus atributos según la categoría, inicializa el stock y
  lo indexa opcionalmente en el motor de búsqueda.
- **Leer (`GET /products/{id}`)**: Obtiene la información detallada del producto, incluyendo sus imágenes y stock
  actual.
- **Actualizar (`PUT /products/{id}`)**: Permite modificar cualquier campo del producto. Si el stock cambia, se registra
  automáticamente una transacción en el historial de inventario (RESTOCK o SALE).
- **Eliminar (`DELETE /products/{id}`)**: Elimina el producto y sus registros asociados (imágenes) de la base de datos.

---

## Guia de uso

### Autenticación

Para realizar acciones privadas, debe obtener un token JWT.

- **Login**: `POST /auth/login`
    - **Ejemplo del cuerpo**:
      ```json
      {
        "email": "admin@gmail.com",
        "password": "123456789"
      }
      ```
- **Credenciales por Defecto**: `admin@gmail.com` / `123456789`
- **Uso**: Incluya el token en el encabezado `Authorization: Bearer <token>`.

### Flujo de Compra

El flujo de compra es un proceso de dos pasos principales:

1. **Carrito**: El usuario añade productos al carrito. Internamente, el sistema mantiene un carrito activo por usuario (
   o sesión de invitado).
    - **Añadir item**: `POST /cart/items`
    - **Ejemplo del cuerpo**:
      ```json
      {
        "productId": 1,
        "quantity": 2
      }
      ```

2. **Finalizar Compra (Checkout)**: Una vez que el carrito tiene productos, el usuario "inicializa" el checkout. Este
   paso es el que **convierte el carrito en una Orden**, descuenta el stock y vacía el carrito.
    - **Endpoint**: `POST /checkout/initialize`
    - **Ejemplo del cuerpo**:
      ```json
      {
        "shippingFullName": "Juan Pérez",
        "shippingAddressLine": "Calle Falsa 123",
        "shippingCity": "Miami",
        "shippingZipCode": "33101",
        "shippingCountry": "USA"
      }
      ```

3. **Consulta de Pedidos**: Tras el checkout, el usuario puede ver su historial de pedidos y el estado de los mismos.
    - **Listar pedidos**: `GET /orders`

### Uso como Administrador

Para gestionar el catálogo, asegúrese de estar logueado con una cuenta con rol `ADMIN`.

#### Gestión de Productos

- **Crear Producto**: `POST /products`
- **Actualizar Producto**: `PUT /products/{id}`
    - **Ejemplo del cuerpo (Crear/Actualizar)**:
      ```json
      {
        "name": "Smartphone XYZ",
        "description": "Un gran teléfono",
        "brand": "TechBrand",
        "price": 599.99,
        "categoryId": 7,
        "stock": 50,
        "specifications": {
          "storage_gb": 128,
          "warranty_months": 12
        },
        "images": ["https://ejemplo.com/foto.jpg"]
      }
      ```
- **Eliminar Producto**: `DELETE /products/{id}`

#### Gestión de Categorías

- **Crear Categoría**: `POST /admin/categories`
    - **Ejemplo del cuerpo**:
      ```json
      {
        "name": "Nueva Categoría",
        "parentId": 1,
        "attributeDefinitions": []
      }
      ```

---

## Motor de Busqueda (Elasticsearch)

Se necesita iniciar el dockerfile como el setup de elasticsearch previo a iniciar el backend