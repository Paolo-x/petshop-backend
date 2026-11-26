# PetShop Chile — Backend

API REST de la tienda PetShop Chile para la Evaluación Parcial 3 de DSY1104 (Duoc UC). Spring Boot recibe las solicitudes de React y guarda el catálogo, los usuarios y los pedidos en MySQL. El [README del frontend](https://github.com/Sk1xez/Proyecto_2#readme) explica cómo usar la tienda y el panel.

**Documentación 3.0 · API v1 · Equipo:** Paolo Muñoz, Vicente Muñoz y Alexis Valencia.

[Instalación](#requisitos) · [Arquitectura y datos](#arquitectura-y-modelo-de-datos) · [Autenticación](#autenticación-y-permisos) · [Operaciones](#contrato-de-la-api-v1) · [Pruebas](#pruebas-verificadas)

## Requisitos

- JDK 21, MySQL 8 y el cliente de terminal `mysql`.
- Bash y `openssl` para ingresar la contraseña y generar la clave de sesión.
- Maven Wrapper incluido (`./mvnw`); no hace falta instalar Maven aparte.

## 1. Preparar MySQL

Si `petshop` y `petshop_app` ya existen, usa esa base y el usuario existente. En una instalación nueva, conecta a MySQL con una cuenta que pueda crear bases y usuarios, y ejecuta:

```sql
CREATE DATABASE IF NOT EXISTS petshop CHARACTER SET utf8mb4;
CREATE USER 'petshop_app'@'localhost' IDENTIFIED BY RANDOM PASSWORD;
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON petshop.* TO 'petshop_app'@'localhost';
```

MySQL muestra la contraseña generada al crear el usuario. Guárdala en un lugar personal y seguro.

## 2. Levantar el backend

Desde `petshop-backend/`, ejecuta en una terminal:

```bash
bash -c '
  IFS= read -r -s -p "Contraseña de MySQL para petshop_app: " DB_PASSWORD || exit 1
  printf "\n"
  JWT_SECRET=$(openssl rand -base64 48) || exit 1
  export DB_PASSWORD JWT_SECRET
  exec ./mvnw spring-boot:run
'
```

- `DB_PASSWORD` es la contraseña de `petshop_app`. Al escribirla en el comando no se muestra en pantalla.
- `JWT_SECRET` es la clave que firma los JWT. Debe venir en Base64 y representar al menos 32 bytes. El ejemplo genera 48 bytes al azar para una sesión local de prueba; al cambiar la clave, las sesiones anteriores dejan de ser válidas.
- Cada JWT dura hasta 1 hora (`petshop.jwt.duracion=PT1H`). `POST /api/v1/auth/refresh` recibe el JWT todavía válido en `Authorization: Bearer …` y devuelve uno nuevo, con los permisos actuales. El límite desde el inicio de sesión es de 8 horas (`petshop.jwt.duracion-maxima=PT8H`); después hay que volver a entrar.

El primer arranque crea las tablas faltantes con Hibernate (`ddl-auto=update`). La API queda en <http://localhost:8080/api/v1> y Swagger en <http://localhost:8080/swagger-ui.html>.

## 3. Cargar los datos de prueba

Con el backend iniciado y desde `petshop-backend/`, abre otra terminal y ejecuta:

```bash
mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=petshop_app --password \
  --default-character-set=utf8mb4 < docs/base-de-datos.sql
```

La carga agrega cuatro categorías, cinco productos con stock y una cuenta por rol. Si se vuelve a ejecutar, conserva las filas que ya existen. El registro público solo crea clientes; el script habilita la primera cuenta de administrador para la evaluación.

| Rol | Correo | Clave pública de prueba |
| --- | --- | --- |
| Administrador | `admin@petshop.test` | `DemoAdminEP3!` |
| Vendedor | `vendedor@petshop.test` | `DemoVentaEP3!` |
| Cliente | `cliente@petshop.test` | `DemoClienteEP3!` |

Úsalas solo en una instalación local de prueba. En MySQL se guardan sus hashes BCrypt.

## 4. Probar con el frontend o Swagger

Para abrir React, entra a `Proyecto_2/react-app`, ejecuta `npm install` y `npm run dev`, y visita <http://localhost:5173>. Vite envía las solicitudes de `/api` al backend en el puerto 8080.

Si el navegador llama a la API directamente, el origen autorizado por defecto es `http://localhost:5173`. Se cambia con `petshop.cors.origenes` en `application.properties`, usando orígenes concretos separados por comas.

En Swagger, abre `POST /api/v1/auth/login`, usa una cuenta de la tabla y copia el valor `token` de la respuesta. Pégalo en **Authorize** para probar las rutas protegidas.

## Arquitectura y modelo de datos

**Tecnologías:** Java 21, Spring Boot 4.1.2, Spring MVC, Spring Data JPA/Hibernate, MySQL 8, Spring Security con OAuth2 Resource Server, springdoc-openapi y Maven Wrapper. Las pruebas usan JUnit, MockMvc, Spring Security Test y H2 en memoria.

En [`cl.petshop.backend`](src/main/java/cl/petshop/backend), `controller` recibe HTTP; `dto` define y valida los datos de entrada y salida; `service` aplica las reglas de negocio; `repository` consulta MySQL mediante JPA; `model` define las entidades. `config` y `security` configuran los permisos, Swagger y la firma y verificación de la sesión.

| Entidad | Datos principales | Relaciones y restricciones |
| --- | --- | --- |
| [`USUARIO`](src/main/java/cl/petshop/backend/model/Usuario.java) | ID, RUN, nombre, apellidos, correo, hash BCrypt y rol | RUN y correo únicos; un cliente puede tener varios pedidos. |
| [`CATEGORIA`](src/main/java/cl/petshop/backend/model/Categoria.java) | ID, nombre e imagen | Nombre único; una categoría agrupa varios productos. |
| [`PRODUCTO`](src/main/java/cl/petshop/backend/model/Producto.java) | ID, código, nombre, descripción, precio, oferta, stock, stock crítico e imagen | Código único y `categoria_id` obligatorio. |
| [`PEDIDO`](src/main/java/cl/petshop/backend/model/Pedido.java) | ID, número, fecha, estado, total y datos de entrega | `cliente_id` referencia a `USUARIO`; un pedido contiene varias líneas. |
| [`DETALLE_PEDIDO`](src/main/java/cl/petshop/backend/model/DetallePedido.java) | ID, cantidad, nombre de producto, precio unitario y subtotal de la compra | `pedido_id` enlaza con `PEDIDO` y `producto_id` con `PRODUCTO`. |

[`PedidoService`](src/main/java/cl/petshop/backend/service/PedidoService.java) valida existencias y calcula el total con los precios de MySQL. La creación deja el pedido `pendiente`, sin reservar ni descontar stock. Al aprobar el pago, una transacción revisa otra vez las existencias, descuenta unidades y deja el pedido `pagada`. Si el monto supera $100.000, queda `fallida` sin descontar stock. No hay edición ni borrado directo de pedidos para conservar el historial. El comprobante es de prueba, no tributario.

## Autenticación y permisos

[`AuthController`](src/main/java/cl/petshop/backend/controller/AuthController.java) recibe el login. El servicio comprueba la contraseña con BCrypt y [`JwtService`](src/main/java/cl/petshop/backend/security/JwtService.java) emite un JWT firmado con HS256. Incluye ID, correo, rol, emisor y fechas, nunca la contraseña. [`SecurityConfig`](src/main/java/cl/petshop/backend/config/SecurityConfig.java) comprueba firma, emisor y vencimiento, y usa el rol para permitir o rechazar el acceso. En pedidos propios se verifica además el dueño.

El cliente envía `Authorization: Bearer <JWT>`. La renovación exige un JWT vigente y consulta el rol actual de la cuenta. El JWT anterior conserva su vencimiento original: renovarlo no lo revoca de inmediato. La vigencia y el máximo total se ajustan en [`application.properties`](src/main/resources/application.properties) con `petshop.jwt.duracion` y `petshop.jwt.duracion-maxima`.

| Acción | Sin sesión | Cliente | Vendedor | Administrador |
| --- | --- | --- | --- | --- |
| Consultar productos | Sí | Sí | Sí | Sí |
| Consultar categorías de la tienda | Sí | Sí | — | Sí |
| Consultar el propio perfil | — | Sí | — | Sí |
| Renovar una sesión vigente | — | Sí | Sí | Sí |
| Cambiar productos y categorías o gestionar usuarios | — | — | — | Sí |
| Crear un pedido, pagarlo y ver sus propias compras | — | Sí | — | — |
| Ver todos los pedidos y su detalle | — | — | Sí | Sí |

El pago simulado solo está habilitado con el perfil `dev`, que es el predeterminado para el trabajo local.

Al terminar, detén el backend y Vite con `Ctrl+C` en sus terminales.

## Contrato de la API v1

- **URL base:** <http://localhost:8080/api/v1> · **Swagger:** <http://localhost:8080/swagger-ui.html> · **OpenAPI JSON:** <http://localhost:8080/v3/api-docs>.
- Las rutas completas se indican abajo. Las entradas con cuerpo se envían como JSON (`Content-Type: application/json`). Los identificadores de ruta son numéricos.
- **Pública** no requiere JWT; si se envía uno inválido, la API puede responder `401`. **Protegida** requiere Bearer. Los roles de las tablas corresponden a `ADMINISTRADOR`, `VENDEDOR` y `CLIENTE`.
- `204` no tiene cuerpo. Los errores de la API indican `status` y `detail`; en errores de validación puede venir `campos`. `400` indica datos inválidos, `401` falta de una sesión válida, `403` falta de permiso, `404` recurso inexistente y `409` un conflicto de negocio (por ejemplo, falta de stock o un registro vinculado).

### Sesión · 4 operaciones

| Método | Ruta | Entrada | Acceso | Respuestas |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/registro` | `run`, `nombre`, `apellidos`, `correo`, `contrasena` | Pública | `201` cliente registrado; `400` datos inválidos; `409` RUN o correo repetido. |
| POST | `/api/v1/auth/login` | `correo`, `contrasena` | Pública | `200` JWT y datos básicos; `400` formato incorrecto; `401` credenciales incorrectas. |
| POST | `/api/v1/auth/refresh` | Bearer vigente; sin cuerpo | Los tres roles | `200` JWT nuevo; `401` sesión no renovable. |
| GET | `/api/v1/auth/perfil` | Sin cuerpo | Cliente y administrador | `200` perfil propio; `401`, `403`; `404` cuenta inexistente. |

### Productos · 7 operaciones

El cuerpo de alta y actualización usa `codigo`, `nombre`, `categoria: { "id": … }`, `precio` y `stock`. `descripcion`, `oferta`, `stockCritico` e `imagen` son opcionales; oferta y stock crítico parten en cero si se omiten. Código único, precio positivo y stock no negativo.

| Método | Ruta | Entrada | Acceso | Respuestas |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/productos` | Sin cuerpo | Pública | `200` listado público. |
| GET | `/api/v1/productos/{id}` | `id` | Pública | `200` detalle; `400` ID inválido; `404` producto inexistente. |
| GET | `/api/v1/productos/categoria/{categoriaId}` | `categoriaId` | Pública | `200` productos de la categoría; `400` ID inválido; `404` categoría inexistente. |
| GET | `/api/v1/productos/buscar` | Consulta `?nombre=…` opcional | Pública | `200` resultados; sin filtro devuelve el listado. |
| POST | `/api/v1/productos` | Cuerpo de producto | Administrador | `201` creado; `400`, `401`, `403`; `404` categoría inexistente; `409` código repetido. |
| PUT | `/api/v1/productos/{id}` | `id` y cuerpo de producto | Administrador | `200` actualizado; `400`, `401`, `403`; `404` producto o categoría inexistente; `409` código repetido. |
| DELETE | `/api/v1/productos/{id}` | `id` | Administrador | `204` eliminado; `400`, `401`, `403`, `404`; `409` producto vinculado a un pedido. |

### Categorías · 5 operaciones

| Método | Ruta | Entrada | Acceso | Respuestas |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/categorias` | Sin cuerpo | Pública; excluye sesión de vendedor | `200` listado; `403` sesión de vendedor. |
| GET | `/api/v1/categorias/{id}` | `id` | Pública; excluye sesión de vendedor | `200` detalle; `400`, `403`; `404` categoría inexistente. |
| POST | `/api/v1/categorias` | `nombre`, `imagen` opcional | Administrador | `201` creada; `400`, `401`, `403`; `409` nombre repetido. |
| PUT | `/api/v1/categorias/{id}` | `id`, `nombre`, `imagen` opcional | Administrador | `200` actualizada; `400`, `401`, `403`, `404`; `409` nombre repetido. |
| DELETE | `/api/v1/categorias/{id}` | `id` | Administrador | `204` eliminada; `400`, `401`, `403`, `404`; `409` tiene productos asignados. |

### Usuarios · 5 operaciones

Las altas y cambios reciben `run`, `nombre`, `apellidos`, `correo` y `rol`. `contrasena` se exige para el alta y se puede omitir al actualizar para conservar la anterior. Las respuestas no incluyen la contraseña ni su hash.

| Método | Ruta | Entrada | Acceso | Respuestas |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/usuarios` | Sin cuerpo | Administrador | `200` listado; `401`, `403`. |
| GET | `/api/v1/usuarios/{id}` | `id` | Administrador | `200` usuario; `400`, `401`, `403`, `404`. |
| POST | `/api/v1/usuarios` | Cuerpo de usuario con contraseña | Administrador | `201` creado; `400`, `401`, `403`; `409` RUN o correo repetido. |
| PUT | `/api/v1/usuarios/{id}` | `id` y cuerpo de usuario | Administrador | `200` actualizado; `400`, `401`, `403`, `404`; `409` RUN o correo repetido. |
| DELETE | `/api/v1/usuarios/{id}` | `id` | Administrador | `204` eliminado; `400`, `401`, `403`, `404`; `409` usuario con pedidos. |

### Pedidos y pago · 5 operaciones

Para crear un pedido se envían `nombreEntrega`, `apellidosEntrega`, `correoEntrega`, `calle`, `region`, `comuna` y `lineas` (cada una con `productoId` y `cantidad`); `departamento` e `indicaciones` son opcionales. El servidor calcula precio, subtotal y total.

| Método | Ruta | Entrada | Acceso | Respuestas |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/pedidos` | Cuerpo de pedido | Cliente | `201` pendiente; `400`, `401`, `403`; `404` producto inexistente; `409` stock insuficiente o datos de producto no válidos. |
| GET | `/api/v1/pedidos` | Sin cuerpo | Vendedor y administrador | `200` todos los pedidos; `401`, `403`. |
| GET | `/api/v1/pedidos/mis-pedidos` | Sin cuerpo | Cliente | `200` pedidos propios; `401`, `403`. |
| GET | `/api/v1/pedidos/{id}` | `id` | Cliente dueño, vendedor y administrador | `200` pedido con líneas; `400`, `401`, `403`, `404`. |
| POST | `/api/v1/pedidos/{id}/pagar-simulado` | `id`, sin cuerpo | Cliente dueño; perfil `dev` | `200` procesado con estado `pagada` o `fallida`; `400`, `401`, `403`, `404`; `409` pedido ya procesado o stock insuficiente. |

## Pruebas verificadas

Ejecuta `./mvnw test` desde la raíz del backend. Las **11 pruebas aprobadas** usan H2 en memoria; no requieren credenciales MySQL. En [`PetshopBackendApplicationTests.java`](src/test/java/cl/petshop/backend/PetshopBackendApplicationTests.java) están las comprobaciones de CRUD y validaciones, login, autorización, renovación, CORS, Swagger y reglas de pedido y pago.

En la integración con MySQL se verificó con `curl`: login `200` de cada rol; catálogo público `200`; creación sin sesión `401` y con rol cliente `403`; alta, lectura, actualización y borrado de un producto temporal (`201`, `200`, `200`, `204` y `404` al consultarlo otra vez); creación y pago de un pedido, stock descontado al aprobar, lista y detalle de órdenes del vendedor y renovación `200`. Swagger mostró las **26 operaciones**; su botón **Authorize** permitió consultar el perfil con Bearer.

Evidencia de la prueba: [catálogo en Swagger, HTTP 200](https://github.com/Sk1xez/Proyecto_2/blob/main/docs/capturas/26-api-exitosa.png) · [capturas y resultados de la integración](https://github.com/Sk1xez/Proyecto_2#pruebas-y-evidencia).
