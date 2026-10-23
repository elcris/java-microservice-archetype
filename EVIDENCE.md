# Evidencias de entrega

Fecha de cierre: 2026-10-23T11:04:45-05:00 (America/Bogota).
Plataforma ejecutada: Windows 11, JDK 21.0.12.1, Maven 3.9.16, Spring Boot 4.1.1.

## Resultado ejecutado

El arquetipo se empaquetó e instaló en una caché local aislada. Se generaron tres servicios
con coordenadas y paquetes distintos, sin intervención manual sobre los proyectos generados.
Cada uno ejecutó clean verify, incluyendo compilación, pruebas y Spotless.

| Motor generado | Pruebas ejecutadas | Fallos | Errores |
|---|---:|---:|---:|
| memory | 26 | 0 | 0 |
| postgresql | 27 | 0 | 0 |
| dynamodb | 30 | 0 | 0 |

Son ejecuciones por variante; las pruebas comunes se repiten entre ellas.
Ocho reglas ArchUnit por variante comprueban las dependencias, ciclos, ubicación y field injection.
Se compilaron además las clases de integración PostgreSQL y DynamoDB Local con Testcontainers.
Esas integraciones NO se ejecutaron aquí: se pasó skipITs=true expresamente por falta de Docker.

La variante PostgreSQL se recompiló con storage=memory: 26 pruebas correctas y JAR sin PostgreSQL/Flyway.
Los archivos de informe PostgreSQL del último build corresponden a ese cambio a memoria;
el resultado anterior de 27 pruebas se conserva en verify-postgresql.log y summary.json.

Arranque real de target/service.jar en dos procesos separados:
- Configuración predeterminada: health UP y rutas de ejemplo/documentación denegadas (403).
- Perfil demo: health/readiness UP, PUT 201+Location, GET 200, repetición PUT 200 con mismo
  recurso, conflicto 409, validación 400, ausencia 404 y UUID inválido 400.
- YAML servido idéntico a resources/openapi/openapi.yaml. Swagger UI HTTP 200 y configuración
  apuntando a ese YAML. Endpoint de contrato automático deshabilitado (404).
- Logs JSON ECS comprobados con correlationId propagado y sin el contenido del recurso de prueba.
- Inspección de JAR: cada motor incluye solo las librerías opcionales correspondientes;
  memory no incorpora PostgreSQL, Flyway, DynamoDB, H2 ni Kafka.
- Validación OpenAPI 3.1.1 mediante swagger-parser y comprobación de parámetros sustituidos.
- Comprobación heurística de patrones de claves privadas/tokens sin coincidencias.

## Cómo reproducir

```sh
sh mvnw clean install
python scripts/verify_generator.py
# Con Docker disponible, ejecutar también:
python scripts/verify_generator.py --database-it
```

En esta sesión se utilizó el ejecutable Maven 3.9.16 previamente disponible, con --repo
apuntando a una caché dentro del espacio de trabajo. El Wrapper también resolvió esa versión
con distribución precargada: la descarga/renombrado en TEMP fue restringida por el sandbox Windows.
No se cambió el Wrapper para eludir restricciones del equipo; los scripts estándar se conservan.

## Pendiente y no afirmado

- Docker Desktop no respondió y no existía conexión al daemon, incluso tras intentar iniciarlo.
  No se ejecutaron PostgreSQL/DynamoDB Local ni docker build; los workflows los dejan reproducibles.
- GitHub Actions está configurado, pero no se ha ejecutado en GitHub ni publicado un repositorio o paquete.
- No se probaron conexiones a bases del banco, permisos IAM reales, alta disponibilidad ni rendimiento.
- Seguridad productiva pendiente del proveedor de identidad y permisos por recurso. Demo es local;
  el template normal permanece cerrado. No se afirma conformidad regulatoria ni BIAN.

## Evidencia adjunta

- evidence/summary.json: resultados del verificador.
- evidence/verify-*.log y evidence/switch-to-memory.log: builds y pruebas.
- evidence/smoke-*.log: arranque y solicitudes HTTP.
- evidence/test-suites.json: detalle de suites del último build de cada carpeta.
- evidence/source-documents.sha256.json: identidad de los documentos analizados, sin redistribuirlos.
- evidence/secret-pattern-check.json: alcance y resultado del chequeo de patrones.

Las rutas y el usuario local están redactados en las copias de logs entregadas.
Las correcciones aplicadas incluyen compatibilidad de tests Spring Boot 4, agente Mockito explícito,
configuración de Swagger UI sobre YAML y formato portable del arquetipo.
