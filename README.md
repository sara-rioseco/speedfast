# SpeedFast — Sistema de Gestión de Entregas

## Descripción

SpeedFast es un prototipo de software desarrollado en Java para las actividades formativas y sumativas de la asignatura **Desarrollo Orientado a Objetos II**. Modela la operación de una empresa de reparto a domicilio que ofrece tres tipos de servicio: pedidos de comida de restaurantes, encomiendas (documentos o paquetes) y compras express en supermercado o farmacia.

El proyecto se construye de forma incremental:

* **Semana 1 — "Explorando la sobrecarga y sobreescritura en clases derivadas"**: jerarquía de pedidos y método `asignarRepartidor()` sobrecargado y sobrescrito según el tipo de servicio.
* **Semana 2 — "Definiendo una clase abstracta y su jerarquía"**: `Pedido` se convierte en **clase abstracta**, se incorpora el atributo común `distanciaKm`, el método implementado `mostrarResumen()` y el método abstracto `calcularTiempoEntrega()`.
* **Semana 3 — "Diseñando un sistema orientado a objetos con clases abstractas, polimorfismo e interfaces"**: se incorporan las interfaces `Despachable`, `Cancelable` y `Rastreable`, el estado del pedido, y la clase `ControladorDeEnvios`, que concentra la lógica de gestión sobre colecciones dinámicas (`ArrayList`).
* **Semana 4 — "Ejecutando tareas en paralelo con hilos en Java"**: `Repartidor` implementa `Runnable` y cada repartidor pasa a ejecutarse como un hilo independiente que recorre su propia lista de pedidos. `Main` lanza a todos los repartidores en paralelo con `ExecutorService`, y el acceso al historial compartido se protege con `synchronized`.
* **Semana 5 — "Sincronizando procesos en sistemas concurrentes"**: los pedidos dejan de repartirse de antemano y pasan a una `ZonaDeCarga` compartida, desde la cual los repartidores los retiran de a uno compitiendo entre sí. La sincronización garantiza que cada pedido sea retirado y entregado por un único repartidor, y un `MonitorEstado` audita el sistema en tiempo real.
* **Semana 6 — "Diseñando interfaces gráficas para aplicaciones en Java"**: el sistema incorpora una interfaz de escritorio construida con **Java Swing**. Desde una `VentanaPrincipal` se abre un formulario para registrar pedidos con validación de campos, un listado de pedidos en `JTable`, y se inician las entregas concurrentes sin bloquear el hilo gráfico.

> El proyecto se mantiene como un único proyecto Maven en la raíz del repositorio: cada semana se construye sobre la anterior, y el avance semanal queda registrado en los commits en lugar de duplicar el código en carpetas separadas.

Cada tipo de pedido tiene criterios distintos, tanto para asignar repartidor como para estimar su tiempo de entrega:

* **Comida** — requiere un repartidor con mochila térmica.
* **Encomienda** — requiere validar el peso del bulto y su embalaje.
* **Compra express** — debe asignarse al repartidor más cercano con disponibilidad inmediata.

El sistema aplica los principios fundamentales de la Programación Orientada a Objetos:

* **Abstracción** — `Pedido` es una clase abstracta: reúne lo común a todo pedido y no puede instanciarse por sí sola, ya que un "pedido genérico" no existe en el dominio.
* **Encapsulamiento** — todos los atributos son `private` y se acceden mediante getters y setters públicos. El estado del pedido solo cambia a través de sus propias operaciones.
* **Herencia** — jerarquía `Pedido → PedidoComida / PedidoEncomienda / PedidoExpress`, donde la clase base concentra los atributos y el comportamiento común. Cada ventana de la interfaz hereda de `JFrame`.
* **Métodos abstractos** — `calcularTiempoEntrega()` y `cumpleRequisitos()` se declaran sin cuerpo en la clase base y obligan a cada subclase a definir su propia regla.
* **Sobrescritura (*overriding*)** — cada subclase redefine `asignarRepartidor()`, `calcularTiempoEntrega()` y `cumpleRequisitos()` con `@Override`.
* **Sobrecarga (*overloading*)** — el método `asignarRepartidor()` existe en tres firmas distintas: sin parámetros, con el nombre del repartidor (`String`) y con el objeto `Repartidor` completo.
* **Interfaces** — `Despachable`, `Cancelable` y `Rastreable` desacoplan las operaciones de despacho, cancelación y seguimiento de la jerarquía de pedidos.
* **Polimorfismo** — el `ControladorDeEnvios` trabaja con referencias `Pedido` y con listas `List<Pedido>`, sin conocer el tipo concreto de cada objeto. El formulario crea un `PedidoComida`, `PedidoEncomienda` o `PedidoExpress` según el tipo elegido, y el controlador lo registra simplemente como `Pedido`.
* **Colecciones dinámicas** — el controlador administra `ArrayList` de pedidos, repartidores e historial de entregas.
* **Separación de responsabilidades (MVC)** — el modelo no conoce la interfaz; las ventanas solo disparan operaciones del `ControladorDeEnvios` y muestran sus datos; `Main` solo arma el sistema y abre la ventana principal.
* **Concurrencia** — `Repartidor` implementa `Runnable` y se ejecuta en paralelo mediante `ExecutorService`, con pausas aleatorias que simulan cada entrega.
* **Recurso compartido** — la `ZonaDeCarga` es accedida simultáneamente por los tres hilos de repartidor; sus métodos `synchronized` garantizan que cada pedido sea retirado por un único repartidor.
* **Sincronización** — `synchronized` protege las secciones críticas, `AtomicInteger` lleva los contadores sin bloqueo y `volatile` comunica la orden de detención al monitor.
* **Manejo de excepciones** — `EntregaException` e `InterruptedException` se capturan por pedido, de modo que un fallo no detiene el recorrido completo. En la interfaz, los datos inválidos se informan con `JOptionPane` sin cerrar el formulario.

---

## Estructura del proyecto

```text
speedfast/
├── src/main/java/com/speedfast/
│   ├── main/
│   │   └── Main.java                   # Arma el sistema y abre la ventana principal
│   ├── model/
│   │   ├── Pedido.java                 # Clase abstracta; implementa las 3 interfaces
│   │   ├── PedidoComida.java           # Mochila térmica · 15 min + 2 min/km
│   │   ├── PedidoEncomienda.java       # Peso y embalaje · 20 min + 1,5 min/km
│   │   ├── PedidoExpress.java          # Cercanía y disponibilidad · 10 min (+5 si > 5 km)
│   │   ├── Repartidor.java             # Implementa Runnable: retira y entrega pedidos en un hilo
│   │   ├── EstadoPedido.java           # Enum: pendiente, asignado, en reparto, entregado, cancelado
│   │   ├── Despachable.java            # Interfaz: despachar()
│   │   ├── Cancelable.java             # Interfaz: cancelar()
│   │   └── Rastreable.java             # Interfaz: verHistorial()
│   ├── exception/
│   │   └── EntregaException.java       # Error de dominio al entregar un pedido
│   ├── service/
│   │   ├── ControladorDeEnvios.java    # Controlador compartido por todas las ventanas
│   │   ├── ZonaDeCarga.java            # Recurso compartido: retiro sincronizado de pedidos
│   │   ├── SimuladorEntregas.java      # Ciclo de vida de los hilos de cada ronda de entregas
│   │   └── MonitorEstado.java          # Hilo que audita el sistema en tiempo real
│   └── view/
│       ├── VentanaPrincipal.java       # Botones de navegación e inicio de entregas
│       ├── VentanaRegistroPedido.java  # Formulario de registro con validación de campos
│       └── VentanaListaPedidos.java    # Listado de pedidos en JTable con DefaultTableModel
├── src/main/resources/images/          # Evidencias de ejecución
├── pom.xml
└── README.md
```

---

## Diagrama de clases

### Dominio y servicios

```mermaid
classDiagram
    class Despachable {
        <<interface>>
        +despachar() String
    }
    class Cancelable {
        <<interface>>
        +cancelar() String
    }
    class Rastreable {
        <<interface>>
        +verHistorial() List~String~
    }

    class Pedido {
        <<abstract>>
        -int idPedido
        -String direccionEntrega
        -float distanciaKm
        -String tipoPedido
        -EstadoPedido estado
        -Repartidor repartidorAsignado
        -List~String~ historial
        +calcularTiempoEntrega()* int
        +cumpleRequisitos(Repartidor)* boolean
        +mostrarResumen() void
        +asignarRepartidor() String
        +asignarRepartidor(String) String
        +asignarRepartidor(Repartidor) String
        +despachar() String
        +cancelar() String
        +confirmarEntrega() boolean
        +verHistorial() List~String~
        #confirmarAsignacion(Repartidor) void
        #encabezado() String
    }

    class PedidoComida {
        -String restaurante
        -int cantidadPlatos
    }
    class PedidoEncomienda {
        -float pesoKg
        -String tipoEmbalaje
    }
    class PedidoExpress {
        -String tienda
        -float radioMaximoKm
    }

    class ControladorDeEnvios {
        -List~Pedido~ pedidos
        -List~Repartidor~ repartidores
        -List~String~ historialEntregas
        -ZonaDeCarga zonaDeCarga
        +registrarPedido(Pedido) void
        +buscarPedidoPorId(int) Pedido
        +contarPedidos(EstadoPedido) int
        +asignarPedidoA(Pedido, Repartidor) String
        +asignarRepartidor(Pedido, String) String
        +buscarRepartidorPorNombre(String) Repartidor
        +asignarAutomaticamente(Pedido) String
        +despachar(Pedido) String
        +registrarEntrega(Pedido) boolean
        +cancelar(Pedido) String
        +verHistorial() List~String~
    }

    class Runnable {
        <<interface>>
        +run() void
    }

    class ZonaDeCarga {
        -Queue~Pedido~ pedidosPendientes
        -AtomicInteger pedidosEnReparto
        -AtomicInteger pedidosEntregados
        +agregarPedido(Pedido) void
        +retirarPedido() Pedido
        +retirarPedido(Repartidor) Pedido
        +confirmarEntrega() void
        +todoEntregado() boolean
    }

    class SimuladorEntregas {
        -ZonaDeCarga zonaDeCarga
        -List~Repartidor~ repartidores
        +ejecutarEntregas() void
    }

    class MonitorEstado {
        -volatile boolean activo
        +run() void
        +informarEstado() void
        +detener() void
    }

    class Repartidor {
        -String nombre
        -float pesoMaximo
        -boolean mochilaTermica
        -boolean disponibleInmediato
        -float distanciaKm
        -List~Pedido~ pedidosAsignados
        +agregarPedido(Pedido) void
        +run() void
        -entregarPedido(Pedido) void
    }

    class EstadoPedido {
        <<enumeration>>
        PENDIENTE
        ASIGNADO
        EN_REPARTO
        ENTREGADO
        CANCELADO
    }

    class EntregaException {
        <<exception>>
    }

    Despachable <|.. Pedido
    Cancelable <|.. Pedido
    Rastreable <|.. Pedido
    Rastreable <|.. ControladorDeEnvios
    Runnable <|.. Repartidor
    Runnable <|.. MonitorEstado
    Repartidor --> ZonaDeCarga : retira pedidos
    MonitorEstado --> ZonaDeCarga : audita
    ZonaDeCarga o-- Pedido : pendientes
    ControladorDeEnvios --> ZonaDeCarga : deja pedidos
    SimuladorEntregas --> Repartidor : ejecuta en paralelo
    SimuladorEntregas --> MonitorEstado : ejecuta
    Pedido <|-- PedidoComida
    Pedido <|-- PedidoEncomienda
    Pedido <|-- PedidoExpress
    Pedido --> Repartidor : repartidorAsignado
    Pedido --> EstadoPedido : estado
    Repartidor o-- Pedido : pedidosAsignados
    Repartidor ..> EntregaException : lanza
    Repartidor --> ControladorDeEnvios : registra entregas
    ControladorDeEnvios o-- Pedido
    ControladorDeEnvios o-- Repartidor
```

### Interfaz gráfica

```mermaid
classDiagram
    class JFrame {
        <<Swing>>
    }
    class VentanaPrincipal {
        +VentanaPrincipal(ControladorDeEnvios, SimuladorEntregas)
        -iniciarEntregas() void
    }
    class VentanaRegistroPedido {
        -JTextField txtId
        -JTextField txtDireccion
        -JComboBox cmbTipo
        -guardarPedido() void
        -crearPedido(int, String, String) Pedido
    }
    class VentanaListaPedidos {
        -DefaultTableModel modelo
        -refrescarTabla() void
    }

    JFrame <|-- VentanaPrincipal
    JFrame <|-- VentanaRegistroPedido
    JFrame <|-- VentanaListaPedidos
    VentanaPrincipal --> VentanaRegistroPedido : abre
    VentanaPrincipal --> VentanaListaPedidos : abre
    VentanaPrincipal --> SimuladorEntregas : inicia entregas (SwingWorker)
    VentanaRegistroPedido --> ControladorDeEnvios : registra pedidos
    VentanaListaPedidos --> ControladorDeEnvios : consulta pedidos
```

---

## Clases principales

* **`Pedido`** *(abstracta)* — atributos comunes: `idPedido`, `direccionEntrega`, `distanciaKm`, `tipoPedido`, `estado`, `repartidorAsignado` e `historial`. Implementa las tres interfaces y ofrece `mostrarResumen()`, `confirmarAsignacion()` y `encabezado()` como comportamiento reutilizable.
* **`PedidoComida`** — agrega `restaurante` y `cantidadPlatos`. Solo acepta repartidores con **mochila térmica**.
* **`PedidoEncomienda`** — agrega `pesoKg` y `tipoEmbalaje`. Valida la **capacidad de carga** del repartidor y que el **embalaje** esté declarado.
* **`PedidoExpress`** — agrega `tienda` y `radioMaximoKm`. Exige **disponibilidad inmediata** y **cercanía** dentro del radio de cobertura.
* **`Repartidor`** — datos del repartidor (`pesoMaximo`, `mochilaTermica`, `disponibleInmediato`, `distanciaKm`), que son los atributos que permiten validar cada tipo de pedido. Implementa `Runnable`: su método `run()` retira pedidos de la `ZonaDeCarga` mientras queden compatibles con su perfil, y los entrega simulando el traslado con pausas aleatorias.
* **`ZonaDeCarga`** — recurso compartido donde llegan los pedidos. Sus métodos `agregarPedido()` y `retirarPedido()` son `synchronized`, lo que impide que dos repartidores retiren el mismo pedido. Lleva contadores `AtomicInteger` de pedidos en reparto y entregados.
* **`MonitorEstado`** — hilo que informa periódicamente cuántos pedidos hay pendientes, en reparto y entregados. Consulta solo los contadores atómicos, por lo que audita el sistema sin interferir con el trabajo de los repartidores.
* **`SimuladorEntregas`** — ejecuta una ronda de entregas: lanza a los repartidores con `ExecutorService` y al monitor en su propio hilo, espera el término de todos y cierra el monitor con `join()`.
* **`EstadoPedido`** — enumeración con los estados válidos de un pedido, evitando textos sueltos repartidos por el código.
* **`EntregaException`** — excepción de dominio que permite informar por qué falló una entrega sin interrumpir el recorrido completo del repartidor.
* **`ControladorDeEnvios`** — registra pedidos y repartidores, asigna, despacha, cancela y mantiene el historial de entregas. Es el controlador que comparten todas las ventanas: al registrar un pedido rechaza IDs repetidos y lo deja en la zona de carga. Sus métodos son `synchronized` porque varios hilos de repartidor lo utilizan al mismo tiempo.
* **Ventanas (`view`)** — `VentanaPrincipal`, `VentanaRegistroPedido` y `VentanaListaPedidos`, descritas en la sección [Interfaz gráfica](#interfaz-gráfica-semana-6).

### Interfaces implementadas

| Interfaz | Método | Implementada por | Propósito |
|---|---|---|---|
| `Despachable` | `despachar()` | `PedidoComida`, `PedidoEncomienda`, `PedidoExpress` (heredado de `Pedido`) | Enviar el pedido a reparto validando su estado |
| `Cancelable` | `cancelar()` | `PedidoComida`, `PedidoEncomienda`, `PedidoExpress` (heredado de `Pedido`) | Anular un pedido que aún no ha sido despachado |
| `Rastreable` | `verHistorial()` | `Pedido` y `ControladorDeEnvios` | Consultar el avance: eventos del pedido e historial global del sistema |

### Método abstracto `calcularTiempoEntrega()`

| Subclase | Fórmula | Ejemplo |
|---|---|---|
| `PedidoComida` | 15 min base + 2 min por kilómetro | 4 km → **23 minutos** |
| `PedidoEncomienda` | 20 min base + 1,5 min por kilómetro (redondeado a entero) | 6 km → **29 minutos** |
| `PedidoExpress` | 10 min base, +5 min si la distancia supera los 5 km | 7 km → **15 minutos** |

### Método abstracto `cumpleRequisitos(Repartidor)`

Permite que el `ControladorDeEnvios` busque un repartidor adecuado **sin conocer las reglas de cada tipo de pedido**: se las pregunta al propio pedido.

| Subclase | Requisito |
|---|---|
| `PedidoComida` | El repartidor cuenta con mochila térmica |
| `PedidoEncomienda` | La carga no supera la capacidad del repartidor y hay embalaje declarado |
| `PedidoExpress` | El repartidor tiene disponibilidad inmediata y está dentro del radio de cobertura |

### Sobrecarga del método `asignarRepartidor()`

| Firma | Comportamiento |
|---|---|
| `asignarRepartidor()` | Mensaje genérico: aún no se designa a nadie |
| `asignarRepartidor(String nombreRepartidor)` | Registra el candidato en el historial del pedido y advierte que la asignación aún no se confirma: sin el objeto no es posible validar los requisitos |
| `asignarRepartidor(Repartidor repartidor)` | Valida los requisitos del tipo de pedido y, si se cumplen, registra la asignación |

Para asignar por nombre de forma efectiva se usa `ControladorDeEnvios.asignarRepartidor(pedido, nombre)`: el controlador es quien conoce la lista de repartidores, resuelve el nombre y delega en la versión que sí valida. Así el mensaje mostrado en consola nunca afirma algo que el estado del objeto no refleje.

---

## Interfaz gráfica (Semana 6)

### Organización en capas (MVC)

Los paquetes de las capas conservan los nombres en inglés usados desde la Semana 1, y el punto de entrada está en el paquete `main`, como indican las instrucciones. Cada uno cumple un rol del patrón Modelo–Vista–Controlador:

| Capa | Paquete | Contenido |
|---|---|---|
| Modelo | `model` | `Pedido` y sus subclases, `Repartidor`, `EstadoPedido` e interfaces del dominio |
| Vista | `view` | `VentanaPrincipal`, `VentanaRegistroPedido`, `VentanaListaPedidos` |
| Controlador | `service` | `ControladorDeEnvios`, apoyado por `ZonaDeCarga`, `SimuladorEntregas` y `MonitorEstado` |
| Punto de entrada | `main` | `Main`, que arma el sistema y abre `new VentanaPrincipal(...)` |

El modelo no importa ninguna clase de Swing: las ventanas usan el controlador para registrar y consultar pedidos, y el simulador para iniciar las entregas.

### Ventanas

| Ventana | Componentes | Función |
|---|---|---|
| `VentanaPrincipal` | `BorderLayout` con un título y tres `JButton` en `GridLayout` | Abrir las demás ventanas e iniciar las entregas |
| `VentanaRegistroPedido` | `JTextField` para ID y dirección, `JComboBox` para el tipo (comida, encomienda, express), botón **Guardar** | Validar los datos, crear el pedido, agregarlo al controlador y confirmar con `JOptionPane` |
| `VentanaListaPedidos` | `JTable` con `DefaultTableModel`, botón **Refrescar** | Mostrar todos los pedidos con su ID, tipo, dirección, estado y repartidor |

Cada botón de la ventana principal abre una ventana nueva, y todas reciben la misma instancia de `ControladorDeEnvios`: un pedido registrado en el formulario aparece en el listado al abrirlo o al presionar **Refrescar**.

El botón **Asignar repartidor / Iniciar entrega** lanza a los tres repartidores en paralelo. Cada uno retira de la zona de carga los pedidos pendientes que cumplen con su perfil, que quedan así asignados a él, y los entrega. Al terminar se informa cuántos pedidos fueron entregados, y el listado muestra qué repartidor atendió cada uno.

### Validación del formulario de registro

| Campo | Regla |
|---|---|
| ID | Obligatorio, número entero mayor que cero y no repetido |
| Dirección | Obligatoria |
| Tipo | Se elige de una lista, por lo que siempre es válido |

Si un dato no es válido se informa el motivo con `JOptionPane` y el pedido no se guarda. El ID repetido lo detecta el propio `ControladorDeEnvios`, de modo que la regla se cumple aunque el pedido llegue por otra vía.

El formulario solicita solo los datos indicados en las instrucciones. Los demás datos que exige cada subclase (distancia, peso, tienda, etc.) se completan con valores estándar, definidos como constantes en `VentanaRegistroPedido` y elegidos de modo que cualquier pedido registrado pueda ser atendido por al menos un repartidor.

### Hilo gráfico y concurrencia

Swing ejecuta todo el dibujo y los eventos en un único hilo, el *Event Dispatch Thread* (EDT). Si ese hilo esperara a que los repartidores terminen, la ventana quedaría congelada durante toda la ronda. Por eso:

* `Main` crea la ventana principal dentro de `SwingUtilities.invokeLater()`.
* El botón de entregas ejecuta `SimuladorEntregas.ejecutarEntregas()` dentro de un `SwingWorker`, es decir, en un hilo de fondo. Mientras dura la ronda el botón queda deshabilitado; las demás ventanas siguen funcionando, y el listado puede refrescarse para ver el avance.
* Al terminar, `done()` vuelve al EDT para habilitar el botón y mostrar el resultado.

---

## Concurrencia y sincronización (Semanas 4 y 5)

En la Semana 4 cada repartidor recorría una lista de pedidos que se le asignaba de antemano, por lo que en la práctica no competía con nadie. En la Semana 5 los pedidos pasan a una **zona de carga común** y los repartidores los retiran de a uno: recién ahí aparece la competencia real por un recurso compartido, y con ella el riesgo de que dos repartidores tomen el mismo pedido.

| Mecanismo | Uso en el proyecto |
|---|---|
| `Runnable` | Lo implementan `Repartidor` (retira y entrega pedidos) y `MonitorEstado` (informa el avance) |
| `ExecutorService` | `SimuladorEntregas` usa `Executors.newFixedThreadPool()` para lanzar a los tres repartidores en paralelo |
| `shutdown()` + `awaitTermination()` | La ronda continúa hasta que todos los repartidores terminan sus recorridos |
| `join()` | Tras detener al monitor, se espera explícitamente el término de su hilo |
| `Thread.sleep()` | Simula el traslado con una pausa aleatoria de entre 500 y 2000 ms por pedido |
| `synchronized` | Protege `agregarPedido()` y `retirarPedido()` en `ZonaDeCarga`, las transiciones de estado de `Pedido` y el historial de `ControladorDeEnvios` |
| `AtomicInteger` | Contadores de pedidos en reparto y entregados, que el monitor consulta **sin tomar el bloqueo** |
| `volatile` | Bandera `activo` del `MonitorEstado`: el hilo ve de inmediato la orden de detenerse |
| `SwingWorker` | Ejecuta la ronda de entregas fuera del hilo gráfico (Semana 6) |

### Por qué `retirarPedido()` es la sección crítica

Sin sincronización, dos repartidores podrían consultar la cola en el mismo instante, ver el mismo pedido y retirarlo ambos: el pedido se entregaría dos veces. Al declarar el método `synchronized`, la consulta y la extracción ocurren de forma indivisible, de modo que el segundo repartidor solo entra cuando el primero ya retiró su pedido y este ya no está en la cola.

La cola se recorre con un `Iterator` explícito y el pedido se extrae con `iterator.remove()`, la forma segura de quitar un elemento de una colección mientras se la recorre.

Ninguno de los métodos sincronizados realiza pausas — el `Thread.sleep()` ocurre **fuera** del bloqueo, mientras el repartidor viaja — por lo que los hilos nunca quedan esperando unos por otros y la ejecución se mantiene realmente paralela.

### Mejoras aplicadas a partir de la retroalimentación de la Semana 5

* `retirarPedido(Repartidor)` usaba un *for-each* y eliminaba el pedido con `Queue.remove()`. Ahora usa `Iterator` e `iterator.remove()`, lo que deja la intención explícita y protege el método frente a futuras modificaciones.
* Después de `monitor.detener()` se espera el término del hilo del monitor con `join()`. Aunque el hilo es *daemon* y la bandera `volatile` ya detenía su ciclo, así ningún hilo creado por la aplicación queda activo al cerrar una ronda.
* Tal como se proyectaba en el *feedforward*, `Pedido`, `ZonaDeCarga` y `ControladorDeEnvios` siguen siendo independientes de la presentación: la interfaz solo dispara operaciones y muestra sus resultados, sin bloquear el hilo gráfico.

### Decisiones de diseño

* Se usó `synchronized` en lugar de `ReentrantLock` porque la zona de carga solo necesita exclusión mutua simple; no requiere intentos con tiempo límite ni múltiples condiciones de espera, que son las ventajas que justificarían el lock explícito.
* No se usó `Semaphore`: permitir que varios repartidores accedan a la vez a la zona de carga es precisamente el problema que se debe evitar, y un semáforo de un solo permiso equivaldría a `synchronized`.
* Los contadores del monitor son `AtomicInteger` y no variables protegidas por el mismo bloqueo, para que auditar el sistema no frene a los repartidores.
* Los atributos de `Repartidor` que definen su perfil (`pesoMaximo`, `mochilaTermica`, `tipoVehiculo`, `distanciaKm`, nombre e identificador) son `final` y ya no exponen setters. La zona de carga los consulta desde otro hilo al evaluar `cumpleRequisitos()`, de modo que un atributo inmutable garantiza que la decisión se tome siempre sobre datos estables. El único atributo mutable es `disponibleInmediato`, declarado `volatile` porque se escribe y se lee bajo bloqueos distintos.
* El ciclo de vida de los hilos pasó de `Main` a `SimuladorEntregas`, para que las entregas puedan iniciarse desde la interfaz tantas veces como se necesite. Por eso cada repartidor informa al final solo los pedidos entregados en el recorrido actual.

### Manejo de excepciones

| Punto crítico | Tratamiento |
|---|---|
| `Thread.sleep()` durante una entrega | Se captura `InterruptedException`, se restaura la marca de interrupción y el repartidor termina de forma controlada |
| Pedido que no puede entregarse | Se lanza `EntregaException` y se captura por pedido: el repartidor informa el motivo y continúa con el siguiente |
| Error inesperado dentro del hilo | Un `catch (RuntimeException)` evita que el hilo muera en silencio, ya que `execute()` no propaga las excepciones |
| `awaitTermination()` y `join()` | Se captura `InterruptedException`, se llama a `shutdownNow()` y se restaura la interrupción |
| Repartidor sin pedidos | Se informa por consola en lugar de tratarse como error |
| Dato inválido en el formulario | Se informa con `JOptionPane` y el pedido no se guarda |
| Error durante una ronda iniciada desde la interfaz | `SwingWorker.get()` lo entrega como `ExecutionException` y se informa con `JOptionPane` |

---

## Aporte del diseño a la escalabilidad, reutilización y mantenibilidad

* **Escalabilidad** — agregar un nuevo tipo de servicio (por ejemplo, `PedidoFarmacia`) solo requiere crear una subclase de `Pedido` e implementar sus dos métodos abstractos; en la interfaz basta con sumar una opción al `JComboBox` y su caso en `crearPedido()`. `ControladorDeEnvios` no necesita modificarse, porque trabaja con referencias `Pedido`.
* **Reutilización** — el estado, el historial, el despacho, la cancelación y `mostrarResumen()` se escriben una sola vez en la clase abstracta y quedan disponibles para las tres subclases. `encabezado()` evita repetir el formato de los mensajes.
* **Mantenibilidad** — cada regla de negocio vive en un solo lugar: las fórmulas de tiempo y los requisitos de asignación están en su subclase, la disponibilidad de repartidores en el controlador, y los estados válidos en el enum `EstadoPedido`. Las interfaces permiten que otras clases usen las operaciones sin depender de la jerarquía concreta, y la interfaz gráfica puede cambiar sin tocar el modelo.

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
mvn exec:java -Dexec.mainClass="com.speedfast.main.Main"
```

Desde IntelliJ IDEA: abrir el proyecto y ejecutar el método `main()` de la clase `Main` (paquete `com.speedfast.main`).

### Uso de la aplicación

1. Al iniciar se abre la **ventana principal**. El sistema parte con seis pedidos de ejemplo y tres repartidores: Juan (motocicleta, con mochila térmica), Camila (furgón, 50 kg de carga) y Pedro (bicicleta, con mochila térmica).
2. **Registrar pedido** abre el formulario. Al presionar **Guardar** se validan los datos: si hay un error se informa el motivo; si todo es correcto se confirma el registro.
3. **Listar pedidos** muestra todos los pedidos en una tabla; el botón **Refrescar** la vuelve a cargar.
4. **Asignar repartidor / Iniciar entrega** lanza a los repartidores en paralelo. Al terminar se informa cuántos pedidos fueron entregados, y el listado muestra el repartidor que atendió cada uno.
5. La consola sigue mostrando los mensajes de los repartidores y del monitor, que evidencian la ejecución concurrente:

```text
Pedido #101 agregado. Destino: Santiago Centro
...
[Repartidor - Juan] Retirando pedido #101... Destino: Santiago Centro
[Repartidor - Juan] Estado: EN_REPARTO
[Repartidor - Juan] Entregando pedido #101... (23 min estimados)
[Repartidor - Camila] Retirando pedido #102... Destino: Providencia
[Repartidor - Pedro] Retirando pedido #103... Destino: Ñuñoa
   [Monitor] Pendientes: 3 | En reparto: 3 | Entregados: 0 de 6
[Repartidor - Camila] Estado: ENTREGADO
...
[Repartidor - Juan] Recorrido finalizado: 2 pedidos entregados.
   [Monitor] Pendientes: 0 | En reparto: 0 | Entregados: 6 de 6
```

El orden de las líneas y el reparto de pedidos entre repartidores cambian en cada ronda, ya que las pausas son aleatorias, pero ningún pedido es retirado dos veces.

---

## Autor

Sara Rioseco
