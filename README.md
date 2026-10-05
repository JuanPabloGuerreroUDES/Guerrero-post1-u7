# Post-contenido — Unidad 7: Patrones Arquitectónicos I

**Estudiante:** Juan Pablo Guerrero Hernandez
**Asignatura:** Patrones de Diseño de Software

## Descripción
Repositorio del post-contenido de la Unidad 7 de Patrones de Diseño de Software. Es un único
proyecto Spring Boot (`multas-biblioteca-api`) para la gestión de multas de la biblioteca: cuando un
estudiante devuelve un libro tarde se registra una multa, se calcula su monto y luego se marca como
pagada.

1. **Parte 1:** API REST en capas (Model, Repository, Service, Controller) sobre H2.
2. **Parte 2:** pago en línea de multas con dos pasarelas intercambiables *(en progreso)*.

## Parte 1 — Arquitectura en Capas

### Estructura de paquetes

```
multas-biblioteca-api/src/main/java/com/example/multas/
├── MultasApplication.java
├── controller/   ← Presentación   MultaController (/api/multas), GenerarMultaRequest,
│                                  GlobalExceptionHandler
├── service/      ← Aplicación     MultaService (reglas de negocio y orquestación)
├── model/        ← Dominio        Multa, EstadoMulta, MultaNotFoundException,
│                                  LimiteMultasPendientesException, MultaYaPagadaException
└── repository/   ← Infraestructura MultaRepository (JpaRepository + consulta de conteo)
```

Las dependencias van en un solo sentido:

```
MultaController ──► MultaService ──► MultaRepository ──► H2
       │                 │
       └────────► model/ (Multa y excepciones) ◄────────┘
```

- **controller/** solo traduce HTTP ↔ Java: recibe el JSON, lo valida con `@Valid` y convierte las
  excepciones de negocio en códigos HTTP (`GlobalExceptionHandler`). Nunca toca el repositorio.
- **service/** contiene las decisiones de negocio: el tope de 3 multas pendientes por estudiante y la
  generación de la multa. No sabe nada de HTTP.
- **model/** es la entidad `Multa` con su propio comportamiento (`calcularMonto` y
  `marcarComoPagada`) y las excepciones del dominio.
- **repository/** es la única capa que habla con la base de datos.

### Reglas de negocio y dónde viven

| Regla | Dónde | Por qué ahí |
|---|---|---|
| Monto = días × $500, con tope de $15.000 | `Multa.calcularMonto` (entidad) | No necesita ningún colaborador externo (ver Punto de decisión 1) |
| Una multa pagada no se puede volver a pagar | `Multa.marcarComoPagada` (entidad) | Es una invariante del estado de la propia multa |
| Máximo 3 multas pendientes por estudiante | `MultaService.generar` | Necesita datos que solo tiene la BD; la decisión la toma el Service y el conteo lo hace `countByEstudianteIdAndEstado` (ver Punto de decisión 2) |

### Endpoints

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/multas` | 200 con todas las multas |
| GET | `/api/multas/{id}` | 200, o 404 si no existe |
| GET | `/api/multas/estudiante/{estudianteId}` | 200 con las multas del estudiante |
| POST | `/api/multas` | 201 con la multa creada; 400 si faltan datos; 409 si ya tiene 3 pendientes |
| PATCH | `/api/multas/{id}/pagar` | 200 con la multa PAGADA (VENTANILLA); 409 si ya estaba pagada |

### Capturas de los endpoints probados (Parte 1)
Todas las capturas son la salida real de `curl` contra la aplicación corriendo en `localhost:8080`.

**GET /api/multas al inicio — 200 OK con lista vacía**
![GET vacío](docs/capturas/01-listar-vacio.png)

**POST /api/multas — 201 Created.** 4 días → $2.000; 45 días → $15.000 por el tope.
![POST 201](docs/capturas/02-generar-multa.png)

**POST /api/multas sin estudianteId — 400 Bad Request**
![POST 400](docs/capturas/03-validacion-400.png)

**Cuarta multa pendiente del mismo estudiante — 409 Conflict**
![POST 409](docs/capturas/04-tope-pendientes-409.png)

**GET /api/multas/999 — 404 Not Found**
![GET 404](docs/capturas/05-no-encontrada-404.png)

**PATCH /api/multas/1/pagar — 200 OK y al repetirlo 409 Conflict**
![PATCH pagar](docs/capturas/06-pagar-ventanilla.png)

**GET /api/multas/estudiante/U00123**
![GET por estudiante](docs/capturas/07-listar-por-estudiante.png)

### Pruebas automáticas
`MultaTest` prueba la regla del monto y el doble pago sobre la entidad sola (sin Spring), y
`MultaControllerTest` repite con MockMvc los checkpoints de la Parte 1 (200, 201, 400, 404, 409).

## Cómo ejecutar
Requisitos: Java 17 o superior y Maven 3.8+.

```bash
cd multas-biblioteca-api
mvn clean package
mvn spring-boot:run
```

- API: http://localhost:8080/api/multas
- Consola H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:multas_biblioteca_db`, usuario `sa`, sin contraseña)

## Herramientas utilizadas
- Java 17, Spring Boot 3.5, Spring Data JPA, H2, Bean Validation
- Apache Maven, curl, JUnit 5 + MockMvc, Git, GitHub

## Decisiones de diseño

### Punto de decisión 1 — Cálculo del monto: ¿entidad o Service?
`calcularMonto` vive en la entidad `Multa` como método estático. El criterio que usé fue: **si una
regla no necesita ningún colaborador externo (repositorio, otro service, configuración), es del
objeto de dominio; si necesita consultar algo, es del Service.** El monto solo depende de los días de
atraso, que ya llegan como parámetro, así que no hay razón para que pase por una capa de Spring.

Si la fórmula fuera un método privado de `MultaService`:
- `Multa` quedaría como un contenedor de getters y setters sin comportamiento (modelo anémico) y la
  regla "una multa nunca pasa de $15.000" no estaría donde se define qué es una multa.
- Cualquier otra parte que necesite el monto (por ejemplo, simular cuánto pagaría un estudiante si
  devuelve el libro hoy) tendría que duplicar la fórmula o inyectar el Service completo para algo que
  no necesita base de datos.
- Para probarla habría que levantar o simular el Service con su repositorio; hoy `MultaTest` la
  prueba con una línea de Java puro.

Con el mismo criterio, `marcarComoPagada` también está en la entidad: verificar que no esté pagada
ya solo depende del estado de la misma multa.

### Punto de decisión 2 — Conteo de multas pendientes: ¿consulta o filtrado en memoria?
La regla "máximo 3 pendientes" sí necesita datos que solo tiene la base de datos. La **decisión** de
negocio (¿se permite una multa más?) la toma `MultaService`, pero el **dato** lo calcula el motor de
BD con `countByEstudianteIdAndEstado`. Con `spring.jpa.show-sql=true` se ve en la consola la
consulta que genera Hibernate:

```sql
select count(m1_0.id) from multas m1_0 where m1_0.estudiante_id=? and m1_0.estado=?
```

La alternativa era `findByEstudianteId(...)` y filtrar con `stream().filter(PENDIENTE).count()`.
Funciona igual con 5 multas de prueba, pero trae a memoria **todo el historial** del estudiante
(pagadas incluidas) solo para obtener un número. Si un estudiante tuviera miles de multas, cada vez
que se genera una nueva se cargarían y convertirían en objetos miles de filas, así que el tiempo de
crear una multa crecería en proporción a su historial. Con el `COUNT` la BD devuelve un solo número
y, si hiciera falta, se podría acelerar con un índice sobre `(estudiante_id, estado)` sin cambiar
nada del Service.

*(Los puntos de decisión 3 y 4 se documentan con la Parte 2.)*
