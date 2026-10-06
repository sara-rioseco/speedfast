-- =====================================================================
-- SpeedFast — Semana 8
-- Crea la base de datos speedfast_db y las tablas del esquema relacional
-- entregado en las instrucciones.
--
-- Ejecutar una sola vez en MySQL Workbench o por consola:
--   mysql -u root -p < sql/01_crear_base_datos.sql
--
-- Si existe la base speedfast_db de la Semana 7 (tablas repartidor,
-- pedido y entrega), elimínala antes de ejecutar este script:
--   DROP DATABASE speedfast_db;
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS speedfast_db;
USE speedfast_db;

CREATE TABLE repartidores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE pedidos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    direccion VARCHAR(100) NOT NULL,
    tipo ENUM('COMIDA','ENCOMIENDA','EXPRESS'),
    estado ENUM('PENDIENTE','EN_REPARTO','ENTREGADO')
);

CREATE TABLE entregas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT,
    id_repartidor INT,
    fecha DATE,
    hora TIME,
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id),
    FOREIGN KEY (id_repartidor) REFERENCES repartidores(id)
);

-- ---------------------------------------------------------------------
-- Verificación de relaciones y restricciones
-- Debe listar las dos claves foráneas de la tabla entregas.
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'speedfast_db'
  AND REFERENCED_TABLE_NAME IS NOT NULL;
