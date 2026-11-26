# PetShop Chile — Backend

API REST de la tienda PetShop Chile para la Evaluación Parcial 3 de DSY1104. Spring Boot recibe las solicitudes de React y guarda el catálogo, los usuarios y los pedidos en MySQL.

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

En Swagger, abre `POST /api/v1/auth/login`, usa una cuenta de la tabla y copia el valor `token` de la respuesta. Pégalo en **Authorize** para probar las rutas protegidas.

### Permisos de la API

| Acción | Sin sesión | Cliente | Vendedor | Administrador |
| --- | --- | --- | --- | --- |
| Consultar productos y categorías públicas | Sí | Sí | Sí | Sí |
| Consultar el propio perfil | — | Sí | Sí | Sí |
| Cambiar productos y categorías o gestionar usuarios | — | — | — | Sí |
| Crear un pedido, pagarlo y ver sus propias compras | — | Sí | — | — |
| Ver todos los pedidos y su detalle | — | — | Sí | Sí |

Al terminar, detén el backend y Vite con `Ctrl+C` en sus terminales.
