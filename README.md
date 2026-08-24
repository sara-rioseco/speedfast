# SpeedFast — Asignación de Repartidores y Tiempos de Entrega

## Descripción

SpeedFast es un prototipo de software desarrollado en Java para las actividades formativas individuales de la asignatura **Desarrollo Orientado a Objetos II**. Modela la operación de una empresa de reparto a domicilio que ofrece tres tipos de servicio: pedidos de comida de restaurantes, encomiendas (documentos o paquetes) y compras express en supermercado o farmacia.

El proyecto se construye de forma incremental:

* **Semana 1 — "Explorando la sobrecarga y sobreescritura en clases derivadas"**: jerarquía de pedidos y método `asignarRepartidor()` sobrecargado y sobrescrito según el tipo de servicio.
* **Semana 2 — "Definiendo una clase abstracta y su jerarquía"**: `Pedido` se convierte en **clase abstracta**, se incorpora el atributo común `distanciaKm`, el método implementado `mostrarResumen()` y el método abstracto `calcularTiempoEntrega()`, que cada subclase resuelve con su propia fórmula.

Cada tipo de pedido tiene criterios distintos, tanto para asignar repartidor como para estimar su tiempo de entrega:

* **Comida** — requiere un repartidor con mochila térmica.
* **Encomienda** — requiere validar el peso del bulto y su embalaje.
* **Compra express** — debe asignarse al repartidor más cercano con disponibilidad inmediata.

El sistema aplica los principios fundamentales de la Programación Orientada a Objetos:

* **Abstracción** — `Pedido` es una clase abstracta: reúne lo común a todo pedido y no puede instanciarse por sí sola, ya que un "pedido genérico" no existe en el dominio.
* **Encapsulamiento** — todos los atributos son `private` y se acceden mediante getters y setters públicos.
* **Herencia** — jerarquía `Pedido → PedidoComida / PedidoEncomienda / PedidoExpress`, donde la clase base concentra los atributos y el comportamiento común.
* **Método abstracto** — `calcularTiempoEntrega()` se declara sin cuerpo en la clase base y obliga a cada subclase a implementar su propia fórmula.
* **Sobrescritura (*overriding*)** — cada subclase redefine `asignarRepartidor()` con `@Override` para aplicar la regla de negocio de su tipo de servicio.
* **Sobrecarga (*overloading*)** — el mismo método `asignarRepartidor()` existe en tres firmas distintas: sin parámetros, recibiendo el nombre del repartidor (`String`) y recibiendo el objeto `Repartidor` completo.
* **Polimorfismo** — un arreglo `Pedido[]` se recorre invocando `mostrarResumen()`, `calcularTiempoEntrega()` y `asignarRepartidor()` sobre referencias de la clase base, y cada objeto responde según su tipo real.
* **Reutilización de comportamiento común** — `mostrarResumen()` está implementado una sola vez en la clase abstracta y desde allí invoca al método abstracto `calcularTiempoEntrega()`, que aporta cada subclase. El método `encabezado()` (`protected`) construye el encabezado común de todos los mensajes.
* **Asociación** — `Repartidor` no forma parte de la jerarquía de pedidos: se recibe como parámetro para validar sus características (mochila térmica, capacidad de carga, disponibilidad y distancia) antes de confirmar la asignación.

La clase `Main` ejecuta la aplicación por consola, instanciando un pedido de cada tipo y mostrando sus resúmenes, la comparación de tiempos estimados y el resultado de las tres versiones de `asignarRepartidor()`, incluidos los casos en que el repartidor **no** cumple los requisitos del pedido.

---

## Estructura del proyecto

```text
speedfast/
├── src/main/java/com/speedfast/
│   ├── app/
│   │   └── Main.java                  # Punto de entrada y pruebas del sistema
│   └── model/
│       ├── Pedido.java                # Clase abstracta (atributos comunes + método abstracto)
│       ├── PedidoComida.java          # Mochila térmica · 15 min + 2 min/km
│       ├── PedidoEncomienda.java      # Peso y embalaje · 20 min + 1,5 min/km
│       ├── PedidoExpress.java         # Cercanía y disponibilidad · 10 min (+5 si > 5 km)
│       └── Repartidor.java            # Repartidor de la plataforma
├── pom.xml
└── README.md
```

---

## Clases principales

### Jerarquía de pedidos

```text
Pedido (clase abstracta)
├── PedidoComida
├── PedidoEncomienda
└── PedidoExpress

Repartidor (clase independiente, asociada por parámetro)
```

* **`Pedido`** *(abstracta)* — atributos comunes de un pedido: `idPedido`, `direccionEntrega`, `distanciaKm` y `tipoPedido`. Incluye un constructor completo, getters y setters, el método `encabezado()` (`protected`), el método implementado `mostrarResumen()`, las tres versiones sobrecargadas de `asignarRepartidor()` y el método abstracto `calcularTiempoEntrega()`.
* **`PedidoComida`** — agrega `restaurante` y `cantidadPlatos`. Solo acepta repartidores que cuenten con **mochila térmica**.
* **`PedidoEncomienda`** — agrega `pesoKg` y `tipoEmbalaje`. Compara el peso declarado con la **capacidad de carga** del repartidor y verifica que el **embalaje** esté declarado.
* **`PedidoExpress`** — agrega `tienda` y `radioMaximoKm`. Exige **disponibilidad inmediata** y que la distancia del repartidor esté dentro del **radio de cobertura**.
* **`Repartidor`** — datos del repartidor: `idRepartidor`, `nombre`, `apellido`, `telefono`, `direccion`, `tipoVehiculo`, `pesoMaximo`, `mochilaTermica`, `disponibleInmediato` y `distanciaKm`. Estos tres últimos atributos son los que permiten validar cada tipo de pedido. Incluye además `getNombreCompleto()` y `toString()`.

### Método abstracto `calcularTiempoEntrega()`

Declarado sin cuerpo en `Pedido`, cada subclase lo implementa con su propia fórmula:

| Subclase | Fórmula | Ejemplo |
|---|---|---|
| `PedidoComida` | 15 min base + 2 min por kilómetro | 4 km → **23 minutos** |
| `PedidoEncomienda` | 20 min base + 1,5 min por kilómetro (redondeado a entero) | 6 km → **29 minutos** |
| `PedidoExpress` | 10 min base, +5 min si la distancia supera los 5 km | 7 km → **15 minutos** |

Las constantes de cada fórmula (`TIEMPO_BASE`, `MINUTOS_POR_KM`, `DISTANCIA_LIMITE_KM`, `RECARGO_DISTANCIA`) se declaran como `private static final` en la subclase correspondiente, de modo que los valores no queden repartidos por el código.

### Sobrecarga y sobrescritura del método `asignarRepartidor()`

| Firma | `PedidoComida` | `PedidoEncomienda` | `PedidoExpress` |
|---|---|---|---|
| `asignarRepartidor()` | Indica que se requiere mochila térmica | Indica que se debe validar peso y embalaje | Busca al repartidor más cercano |
| `asignarRepartidor(String nombreRepartidor)` | Verifica mochila térmica | Valida peso y embalaje | Confirma cercanía y disponibilidad |
| `asignarRepartidor(Repartidor repartidor)` | Acepta o **rechaza** según `mochilaTermica` | Acepta o **rechaza** según `pesoMaximo` y embalaje | Acepta o **rechaza** según `disponibleInmediato` y `distanciaKm` |

Las dos primeras firmas corresponden a lo solicitado en la actividad de la Semana 1; la tercera se agregó para validar los datos reales del repartidor y evidenciar los casos rechazados.

---

## Requisitos

* Java 21 o superior.
* Maven.
* IntelliJ IDEA (recomendado).

---

## Instrucciones para clonar y ejecutar

```bash
git clone https://github.com/sara-rioseco/speedfast.git
cd speedfast
mvn compile
mvn exec:java -Dexec.mainClass="com.speedfast.app.Main"
```

Desde IntelliJ IDEA: abrir el proyecto y ejecutar el método `main()` de la clase `Main` (paquete `com.speedfast.app`).

Al ejecutar el programa, la consola muestra seis secciones:

1. **Resumen de pedidos y tiempo estimado de entrega** — se recorre un arreglo `Pedido[]` invocando `mostrarResumen()`, que a su vez llama al `calcularTiempoEntrega()` de cada subclase.
2. **Comparación de tiempos estimados** — tabla con el tipo, la distancia y el tiempo calculado de cada pedido.
3. **Sobrescritura** — se invoca `asignarRepartidor()`; cada objeto responde con la lógica de su propia clase.
4. **Sobrecarga con `String`** — se asigna cada pedido indicando el nombre del repartidor.
5. **Sobrecarga con `Repartidor`** — se asigna cada pedido con un repartidor que **sí** cumple los requisitos.
6. **Validaciones rechazadas** — se intenta asignar un repartidor que **no** cumple los requisitos de cada tipo de pedido.

### Ejemplo de salida

```text
============================================================
1. RESUMEN DE PEDIDOS Y TIEMPO ESTIMADO DE ENTREGA
============================================================
[Pedido de Comida] N° 101
Dirección de entrega: Av. Italia 456, Providencia
Distancia: 4,0 km
Tiempo estimado de entrega: 23 minutos

[Pedido de Encomienda] N° 102
Dirección de entrega: Av. Independencia 123, Independencia
Distancia: 6,0 km
Tiempo estimado de entrega: 29 minutos

[Pedido Express] N° 103
Dirección de entrega: Av. Apoquindo 1500, Las Condes
Distancia: 7,0 km
Tiempo estimado de entrega: 15 minutos

============================================================
2. COMPARACIÓN DE TIEMPOS ESTIMADOS
============================================================
TIPO DE PEDIDO           N°     DISTANCIA    TIEMPO ESTIMADO
------------------------------------------------------------
Pedido de Comida         101    4,0 km       23 minutos
Pedido de Encomienda     102    6,0 km       29 minutos
Pedido Express           103    7,0 km       15 minutos
------------------------------------------------------------
```

---

## Autor

Sara Rioseco
