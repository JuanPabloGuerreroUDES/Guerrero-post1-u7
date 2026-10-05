# Borrador — decisión de la Parte 2 (antes de escribir código)

**Requisito:** pagar multas en línea con PagosUDES (sede principal) o Wompi (sede piloto). Cada sede
elige una por configuración, sin recompilar Controller ni Service, y al terminar el piloto se puede
agregar una tercera o quitar una.

## Lo que pesa en la decisión
- Los dos contratos HTTP son distintos: PagosUDES usa `idTransaccion` / `estadoTransaccion`
  (`APROBADA`) y el monto en pesos; Wompi usa `reference` / `status` (`APPROVED`) y el monto en
  **centavos**. Alguien tiene que traducir eso, y no debería ser `MultaService`.
- El número de pasarelas **no es estable**: el propio requisito dice que puede subir a tres o bajar a
  una. Lo que cambia es justamente "con quién se habla", no la regla de pago.
- La guía (7.3) recomienda hexagonal cuando hay varias fuentes externas que implementan el mismo
  puerto. Aquí son dos APIs externas con formatos distintos.
- El resto del proyecto (Multa, repositorio, controlador) es un CRUD simple: llevarlo todo a
  hexagonal sería sobre-ingeniería.

## Opciones
- **A (if/switch en MultaService):** la más rápida, pero el Service tendría los dos clientes HTTP,
  los DTOs y la conversión a centavos. Agregar o quitar una pasarela = editar y volver a probar el
  Service. Descartada.
- **B (Strategy en service/):** resuelve el intercambio con polimorfismo. El problema es que la
  interfaz quedaría en `service/` junto a implementaciones `@Service` que hablan HTTP, y nada impide
  que la interfaz termine devolviendo el DTO de una pasarela concreta. Es casi la misma cantidad de
  código que C, pero sin la regla de dependencia explícita.
- **C (puerto en domain/ + dos adaptadores en infrastructure/), solo para el pago:** el Service
  depende de `PasarelaPagoPort` y de un `ResultadoPago` neutral; cada adaptador traduce su propio
  formato. Agregar una pasarela = una clase nueva en `infrastructure/pago/` y una propiedad.

## Decisión
**Opción C, limitada a la porción de pago.** La Parte 1 queda en capas tal cual.

Ajuste propio respecto al enunciado: el puerto no recibe la entidad `Multa` (que tiene anotaciones
de JPA) sino un record `SolicitudPago` con solo lo que una pasarela necesita (id de la multa,
estudiante y monto), para que `domain/` compile únicamente con `java.*`.

Selección del adaptador: `@ConditionalOnProperty` (un solo bean activo por arranque), porque cada
sede usa una pasarela fija durante el piloto.
