# PetShop Chile — Backend

API REST de la tienda PetShop Chile (DSY1104, Duoc UC). Spring Boot recibe las solicitudes de React, aplica los permisos y las reglas de compra, y guarda el catálogo, los usuarios y los pedidos en MySQL. El [repositorio frontend](https://github.com/Sk1xez/Proyecto_2#readme) explica cómo iniciar React.

**Equipo:** Paolo Muñoz, Vicente Muñoz y Alexis Valencia.

## Tecnologías y requisitos

- JDK 21, Spring Boot 4.1.2, Spring MVC, JPA/Hibernate, MySQL 8 y Spring Security con JWT HS256.
- Swagger con springdoc-openapi. Maven Wrapper incluido; no hace falta instalar Maven aparte.
- Bash, el cliente de terminal `mysql` y `openssl`. Las pruebas usan JUnit, MockMvc y H2 en memoria.

## 1. Preparar MySQL

Si `petshop` y `petshop_app` ya existen, usa esa base y ese usuario. En una instalación nueva, conecta a MySQL con una cuenta que pueda crear bases y usuarios y ejecuta:

```sql
CREATE DATABASE IF NOT EXISTS petshop CHARACTER SET utf8mb4;
CREATE USER 'petshop_app'@'localhost' IDENTIFIED BY RANDOM PASSWORD;
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON petshop.* TO 'petshop_app'@'localhost';
```

MySQL muestra la contraseña generada al crear el usuario. Guárdala fuera del repositorio.

## 2. Levantar el backend

Abrir PowerShell en la carpeta petshop-backend y ejecutar:

Remove-Item Env:SPRING_DATASOURCE_USERNAME -ErrorAction SilentlyContinue; Remove-Item Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue; .\mvnw.cmd spring-boot:run

## 3. Cargar los datos de prueba

Con el backend iniciado, ejecuta desde otra terminal en `petshop-backend/`:

```bash
mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=petshop_app --password \
  --default-character-set=utf8mb4 < docs/base-de-datos.sql
```

La carga agrega cuatro categorías, cinco productos y una cuenta por rol, sin reemplazar filas existentes. Estas son **cuentas públicas de demostración**, solo para el entorno local:

| Rol | Correo | Clave de prueba |
| --- | --- | --- |
| Administrador | `admin@petshop.test` | `DemoAdminEP3!` |
| Vendedor | `vendedor@petshop.test` | `DemoVentaEP3!` |
| Cliente | `cliente@petshop.test` | `DemoClienteEP3!` |

## Pruebas y Swagger

La API usa <http://localhost:8080/api/v1>; abre Swagger en <http://localhost:8080/swagger-ui.html>. Para probar una ruta protegida, inicia sesión en Swagger y pega el valor `token` en **Authorize**, sin agregar un prefijo. Abre el frontend en <http://localhost:5173> después de ejecutar los comandos de su README.

```bash
./mvnw test
```

Pasaron **11 pruebas** sobre H2 en memoria; este comando no necesita la contraseña de MySQL. Para detener el servidor local, presiona `Ctrl+C`.

## Documentación de la EP3

- [APIs e integración — Anexo 2](docs/APIs-e-Integracion.docx)
- [Catálogo de las 26 operaciones REST — Anexo 3](docs/Documentacion-de-APIs.xlsx)
- [ERS, manual y pruebas](https://github.com/Sk1xez/Proyecto_2#documentación-de-la-ep3)
