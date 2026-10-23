# Estándares efectivos del generador

Procedencia: agenteEspecializadoJavaSPHexV2.agent.md, instructions-backend.md y
lineamientos-openapi-bian.md suministrados por el usuario. No se encontraron AGENTS.md
ni docs/standards preexistentes en el espacio de trabajo o en GuardaRails.
Este resumen es una adaptación trazable, no una aprobación normativa del banco.

- Java 21, Spring Boot 4.x, Maven Wrapper; dependencias gestionadas por BOM.
- domain.models.<concepto>, domain.ports.inbound/outbound y domain.exceptions son Java puro.
- Commands y resultados de puertos viven en domain; application implementa puertos inbound.
- application solo depende de domain, JDK y anotaciones Service/Transactional.
- infrastructure configura frameworks y contiene adaptadores; inyección por constructor.
- Controladores usan puertos inbound y mappers. No invocan servicios concretos ni puertos outbound.
- Modelos internos separados de DTO REST y persistencia. Sin lógica de negocio en mappers.
- Validación sintáctica en DTO; invariantes en dominio; Clock inyectado.
- Errores RFC 9457, sin SQL, credenciales, payloads ni trazas públicas.
- OpenAPI 3.1.1: respuestas directas, 201+Location, códigos reales, operationId único.
- No atribuir semántica BIAN sin activo, versión y mapeo verificables.
- Logs JSON con correlación; sin cuerpos, query strings, tokens, nombres de clientes ni IDs como etiquetas métricas.
- Todo puerto de almacenamiento debe preservar escritura atómica por id y lectura de ausencia Optional.
- Los componentes opcionales se compilan en sus respectivos perfiles Maven.
- ArchUnit, pruebas de dominio/casos de uso, slice MVC y contrato; verificar cada motor afectado.
- Flyway es el único dueño del esquema PostgreSQL; nunca JPA DDL ni scripts paralelos.
- Registrar decisiones y excepciones; no introducir secretos ni identificadores bancarios fijos.
- Antes de producción definir IAM, autorización sobre recursos, límites de tráfico, observabilidad exportada,
  imágenes por digest, escaneo de dependencias, SLO, respaldo y operación aprobados por el banco.
