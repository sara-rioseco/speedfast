-- =====================================================================
-- SpeedFast — Semana 7
-- Crea la base de datos speedfast_db y las tablas del modelo relacional.
--
-- Ejecutar una sola vez en MySQL Workbench o por consola:
--   mysql -u root -p < sql/01_crear_base_datos.sql
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS speedfast_db;
USE speedfast_db;

CREATE TABLE repartidor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE pedido (
    id INT AUTO_INCREMENT PRIMARY KEY,
    direccion VARCHAR(150) NOT NULL,
    tipo VARCHAR(30) NOT NULL,    -- COMIDA | ENCOMIENDA | EXPRESS
    estado VARCHAR(20) NOT NULL   -- PENDIENTE | EN_REPARTO | ENTREGADO
);

CREATE TABLE entrega (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_repartidor INT NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    FOREIGN KEY (id_pedido) REFERENCES pedido(id),
    FOREIGN KEY (id_repartidor) REFERENCES repartidor(id)
);

-- ---------------------------------------------------------------------
-- Verificación de relaciones y restricciones
-- Debe listar las dos claves foráneas de la tabla entrega.
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'speedfast_db'
  AND REFERENCED_TABLE_NAME IS NOT NULL;
