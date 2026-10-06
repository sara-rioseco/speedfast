-- =====================================================================
-- SpeedFast — Semana 8
-- Datos de ejemplo (opcional): tres repartidores, seis pedidos pendientes
-- y dos pedidos con su entrega registrada (uno entregado y otro en
-- reparto), para que las tres tablas de la aplicación no partan vacías.
--
-- Ejecutar después de 01_crear_base_datos.sql, sobre la base recién
-- creada (las entregas usan los ID 1 a 8 que asigna AUTO_INCREMENT):
--   mysql -u root -p < sql/02_datos_ejemplo.sql
-- =====================================================================

SET NAMES utf8mb4;
USE speedfast_db;

INSERT INTO repartidores (nombre) VALUES
    ('Juan Pérez'),
    ('Camila Soto'),
    ('Pedro Díaz');

INSERT INTO pedidos (direccion, tipo, estado) VALUES
    ('Santiago Centro', 'COMIDA', 'PENDIENTE'),
    ('Providencia', 'ENCOMIENDA', 'PENDIENTE'),
    ('Ñuñoa', 'EXPRESS', 'PENDIENTE'),
    ('Recoleta', 'COMIDA', 'PENDIENTE'),
    ('Las Condes', 'ENCOMIENDA', 'PENDIENTE'),
    ('Providencia', 'EXPRESS', 'PENDIENTE'),
    ('Maipú', 'COMIDA', 'ENTREGADO'),
    ('La Florida', 'ENCOMIENDA', 'EN_REPARTO');

INSERT INTO entregas (id_pedido, id_repartidor, fecha, hora) VALUES
    (7, 1, '2026-10-01', '13:15:00'),
    (8, 2, '2026-10-02', '18:40:00');
