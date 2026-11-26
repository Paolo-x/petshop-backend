-- Solo para una instalación local de prueba. Estas cuentas son públicas.
-- Primero inicia el backend para que Hibernate prepare las tablas.
-- Puedes volver a ejecutar el archivo: no cambia filas que ya existen.
CREATE DATABASE IF NOT EXISTS petshop CHARACTER SET utf8mb4;
USE petshop;

START TRANSACTION;

INSERT INTO categoria (nombre, imagen)
SELECT demo.nombre, demo.imagen
FROM (
    SELECT 'Alimentos' AS nombre, '/img/productos/alimento.jpg' AS imagen
    UNION ALL SELECT 'Juguetes', '/img/productos/juguete.jpg'
    UNION ALL SELECT 'Accesorios', '/img/productos/collar.avif'
    UNION ALL SELECT 'Higiene', '/img/productos/shampoo.jpg'
) AS demo
LEFT JOIN categoria c ON c.nombre = demo.nombre
WHERE c.id IS NULL;

INSERT INTO producto (codigo, nombre, descripcion, categoria_id, precio, oferta, stock, stock_critico, imagen)
SELECT demo.codigo, demo.nombre, demo.descripcion, c.id, demo.precio, demo.oferta, demo.stock, 3, demo.imagen
FROM (
    SELECT 'EP3-ALI-01' AS codigo, 'Alimento para perros' AS nombre,
        'Bolsa de 3 kg para perros adultos.' AS descripcion, 'Alimentos' AS categoria,
        14990 AS precio, 0 AS oferta, 25 AS stock, '/img/productos/alimento.jpg' AS imagen
    UNION ALL SELECT 'EP3-ALI-02', 'Alimento para gatos', 'Bolsa de 1 kg para gatos adultos.',
        'Alimentos', 8990, 10, 18, '/img/productos/alimento-gato.avif'
    UNION ALL SELECT 'EP3-JUG-01', 'Pelota para perros', 'Pelota de goma para jugar al aire libre.',
        'Juguetes', 4490, 0, 15, '/img/productos/pelota.jpg'
    UNION ALL SELECT 'EP3-ACC-01', 'Collar ajustable', 'Collar liviano con broche de seguridad.',
        'Accesorios', 6990, 15, 12, '/img/productos/collar.avif'
    UNION ALL SELECT 'EP3-HIG-01', 'Champú para mascotas', 'Champú suave en botella de 250 ml.',
        'Higiene', 5490, 0, 20, '/img/productos/shampoo.jpg'
) AS demo
JOIN categoria c ON c.nombre = demo.categoria
LEFT JOIN producto p ON p.codigo = demo.codigo
WHERE p.id IS NULL;

-- Hashes BCrypt de las tres claves públicas de prueba indicadas en el README.
INSERT INTO usuario (run, nombre, apellidos, correo, contrasena_hash, rol)
SELECT demo.run, demo.nombre, 'PetShop Demo', demo.correo, demo.contrasena_hash, demo.rol
FROM (
    SELECT '90.000.001-4' AS run, 'Administración' AS nombre, 'admin@petshop.test' AS correo,
        '$2a$10$QjPChcHMq1fHIADh5DXopOpUGuXLVjeh4xgmn.9zQ9DM5Ltxc4zuq' AS contrasena_hash,
        'ADMINISTRADOR' AS rol
    UNION ALL SELECT '90.000.002-2', 'Ventas', 'vendedor@petshop.test',
        '$2a$10$om3Pf.MDoN10qizWibbCSedUu4SaiDitQq6Ez.toIJ00pppibWKJe', 'VENDEDOR'
    UNION ALL SELECT '90.000.003-0', 'Cliente', 'cliente@petshop.test',
        '$2a$10$gWdTK/gbn5h7rPA6IOgPE.tMSCDONfKK3Ri9gc5TQ.Yj/2IhhR1Ya', 'CLIENTE'
) AS demo
LEFT JOIN usuario u ON u.correo = demo.correo OR u.run = demo.run
WHERE u.id IS NULL;

COMMIT;
