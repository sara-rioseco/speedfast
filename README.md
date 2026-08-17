# SpeedFast — Asignación de Repartidores

## Descripción

SpeedFast es un prototipo de software desarrollado en Java para la actividad formativa individual **"Explorando la sobrecarga y sobreescritura en clases derivadas"** (Semana 1) de la asignatura **Desarrollo Orientado a Objetos II**. Modela la operación de una empresa de reparto a domicilio que ofrece tres tipos de servicio: pedidos de comida de restaurantes, encomiendas (documentos o paquetes) y compras express en supermercado o farmacia.

Cada tipo de pedido tiene un criterio distinto para asignar repartidor:

* **Comida** — requiere un repartidor con mochila térmica.
* **Encomienda** — requiere validar el peso del bulto y su embalaje.
* **Compra express** — debe asignarse al repartidor más cercano con disponibilidad inmediata.

El sistema resuelve estos tres criterios con un único método, `asignarRepartidor()`, aplicando los principios fundamentales de la Programación Orientada a Objetos:

* **Encapsulamiento** — todos los atributos son `private` y se acceden mediante getters y setters públicos.
* **Herencia** — jerarquía `Pedido → PedidoComida / PedidoEncomienda / PedidoExpress`, donde la clase base concentra los atributos y el comportamiento común.
* **Sobrescritura (*overriding*)** — cada subclase redefine `asignarRepartidor()` con `@Override` para aplicar la regla de negocio de su tipo de servicio.
* **Sobrecarga (*overloading*)** — el mismo método existe en tres firmas distintas: sin parámetros, recibiendo el nombre del repartidor (`String`) y recibiendo el objeto `Repartidor` completo.
* **Polimorfismo** — un arreglo `Pedido[]` se recorre invocando `asignarRepartidor()` sobre referencias de la clase base, y cada objeto responde según su tipo real.
* **Asociación** — `Repartidor` no forma parte de la jerarquía de pedidos: se recibe como parámetro para validar sus características (mochila térmica, capacidad de carga, disponibilidad y distancia) antes de confirmar la asignación.
* **Reutilización mediante `protected`** — el método `encabezado()` de la clase base construye el encabezado común de cada mensaje y es reutilizado por todas las subclases.

La clase `Main` ejecuta la aplicación por consola, instanciando un pedido de cada tipo y mostrando el resultado de las tres versiones del método, incluidos los casos en que el repartidor **no** cumple los requisitos del pedido.

---

## Estructura del proyecto

```text
speedfast/
├── src/main/java/com/speedfast/
│   ├── app/
│   │   └── Main.java                  # Punto de entrada y pruebas del sistema
│   └── model/
│       ├── Pedido.java                # Clase base (3 versiones sobrecargadas del método)
│       ├── PedidoComida.java          # Valida mochila térmica
│       ├── PedidoEncomienda.java      # Valida peso y embalaje
│       ├── PedidoExpress.java         # Valida cercanía y disponibilidad inmediata
│       └── Repartidor.java            # Repartidor de la plataforma
├── pom.xml
└── README.md
```

---

## Clases principales

### Jerarquía de pedidos

```text
Pedido (clase base)
├── PedidoComida
├── PedidoEncomienda
└── PedidoExpress

Repartidor (clase independiente, asociada por parámetro)
```

* **`Pedido`** *(base)* — atributos comunes de un pedido: `idPedido`, `direccionEntrega` y `tipoPedido`. Incluye un constructor completo, getters y setters, el método `encabezado()` (`protected`) y las tres versiones sobrecargadas de `asignarRepartidor()` con lógica genérica, pensada para ser heredada y sobrescrita.
* **`PedidoComida`** — agrega `restaurante` y `cantidadPlatos`. Solo acepta repartidores que cuenten con **mochila térmica**.
* **`PedidoEncomienda`** — agrega `pesoKg` y `tipoEmbalaje`. Compara el peso declarado con la **capacidad de carga** del repartidor y verifica que el **embalaje** esté declarado.
* **`PedidoExpress`** — agrega `tienda` y `radioMaximoKm`. Exige **disponibilidad inmediata** y que la distancia del repartidor esté dentro del **radio de cobertura**.
* **`Repartidor`** — datos del repartidor: `idRepartidor`, `nombre`, `apellido`, `telefono`, `direccion`, `tipoVehiculo`, `pesoMaximo`, `mochilaTermica`, `disponibleInmediato` y `distanciaKm`. Estos tres últimos atributos son los que permiten validar cada tipo de pedido. Incluye además `getNombreCompleto()` y `toString()`.

### Sobrecarga y sobrescritura del método `asignarRepartidor()`

| Firma | `Pedido` (genérico) | `PedidoComida` | `PedidoEncomienda` | `PedidoExpress` |
|---|---|---|---|---|
| `asignarRepartidor()` | Busca un repartidor disponible en la zona | Indica que se requiere mochila térmica | Indica que se debe validar peso y embalaje | Busca al repartidor más cercano |
| `asignarRepartidor(String nombreRepartidor)` | Verifica disponibilidad general | Verifica mochila térmica | Valida peso y embalaje | Confirma cercanía y disponibilidad |
| `asignarRepartidor(Repartidor repartidor)` | Asigna sin validaciones específicas | Acepta o **rechaza** según `mochilaTermica` | Acepta o **rechaza** según `pesoMaximo` y embalaje | Acepta o **rechaza** según `disponibleInmediato` y `distanciaKm` |

Las dos primeras firmas corresponden a lo solicitado en la actividad; la tercera se agregó para validar los datos reales del repartidor y evidenciar los casos rechazados.

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

Al ejecutar el programa, la consola muestra cinco secciones:

1. **Sobrescritura** — se recorre un arreglo `Pedido[]` invocando `asignarRepartidor()`; cada objeto responde con la lógica de su propia clase.
2. **Sobrecarga con `String`** — se asigna cada pedido indicando el nombre del repartidor.
3. **Sobrecarga con `Repartidor`** — se asigna cada pedido con un repartidor que **sí** cumple los requisitos.
4. **Validaciones rechazadas** — se intenta asignar un repartidor que **no** cumple los requisitos de cada tipo de pedido.
5. **Pedido genérico** — se instancia la clase base para contrastar su comportamiento con el de las subclases.

### Ejemplo de salida

```text
============================================================
2. SOBRECARGA: asignarRepartidor(String nombreRepartidor)
============================================================
[Pedido de Comida] N° 101
Dirección de entrega: Av. Providencia 1234, Providencia
Asignando repartidor...
   Retiro en: Sushi Kai (3 platos)
   Verificando mochila térmica... OK
   Pedido asignado a Juan Pérez

[Pedido de Encomienda] N° 102
Dirección de entrega: Los Leones 456, Providencia
Asignando repartidor...
   Peso declarado: 12,5 kg | Embalaje: Caja de cartón sellada
   Validando peso y embalaje... OK
   Pedido asignado a Camila Soto

[Pedido Express] N° 103
Dirección de entrega: Irarrázaval 789, Ñuñoa
Asignando repartidor...
   Compra en: Farmacia Central
   Repartidor más cercano con disponibilidad inmediata encontrado.
   Pedido asignado a Luis Díaz
```

---

## Autor

Sara Rioseco
