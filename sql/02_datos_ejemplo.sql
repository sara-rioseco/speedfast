-- =====================================================================
-- SpeedFast — Semana 7
-- Datos de ejemplo (opcional): tres repartidores y seis pedidos
-- pendientes, para que la aplicación no parta vacía.
--
-- Ejecutar después de 01_crear_base_datos.sql:
--   mysql -u root -p < sql/02_datos_ejemplo.sql
-- =====================================================================

SET NAMES utf8mb4;
USE speedfast_db;

INSERT INTO repartidor (nombre) VALUES
    ('Juan Pérez'),
    ('Camila Soto'),
    ('Pedro Díaz');

INSERT INTO pedido (direccion, tipo, estado) VALUES
    ('Santiago Centro', 'COMIDA', 'PENDIENTE'),
    ('Providencia', 'ENCOMIENDA', 'PENDIENTE'),
    ('Ñuñoa', 'EXPRESS', 'PENDIENTE'),
    ('Recoleta', 'COMIDA', 'PENDIENTE'),
    ('Las Condes', 'ENCOMIENDA', 'PENDIENTE'),
    ('Providencia', 'EXPRESS', 'PENDIENTE');
