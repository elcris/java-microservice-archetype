#set( $symbol_dollar = '$' )
# ${artifactId}

Java 21 / Spring Boot 4.1.1, Maven Wrapper y arquitectura hexagonal.

## Ejecutar

Windows usa `mvnw.cmd`; Linux/macOS usa `sh mvnw`.

```sh
sh mvnw clean verify
java -jar target/service.jar
```

Health: `http://localhost:8080/actuator/health`. La API está cerrada por defecto.
Para el ejemplo local y Swagger:

```sh
java -jar target/service.jar --spring.profiles.active=demo
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`.
Contrato: `http://localhost:8080/openapi.yaml`, fuente en `src/main/resources/openapi/openapi.yaml`.

```sh
curl -i -X PUT http://localhost:8080/api/v1/entries/00000000-0000-0000-0000-000000000001 \
  -H 'Content-Type: application/json' -d '{"name":"technical-example"}'
curl -i http://localhost:8080/api/v1/entries/00000000-0000-0000-0000-000000000001
```

El UUID es un dato ficticio del ejemplo, no un identificador de negocio. PUT crea (201), repite
el mismo contenido (200) o rechaza otro contenido con el mismo id (409). No hay updates ni deletes.

## Elegir almacenamiento

`.mvn/maven.config` conserva la selección inicial `${persistence}`. Puede editarse o sobrescribirse:

| Motor | Compilar | Configuración al arrancar |
|---|---|---|
| Memoria | `sh mvnw -Dstorage=memory clean verify` | Ninguna base ni credenciales |
| PostgreSQL | `sh mvnw -Dstorage=postgresql clean verify` | DB_URL, DB_USERNAME, DB_PASSWORD |
| DynamoDB | `sh mvnw -Dstorage=dynamodb clean verify` | AWS_REGION, DYNAMODB_TABLE y rol IAM |

No combinar motores. Cambiar motor requiere reconstruir con `clean`; no basta cambiar un perfil Spring.
Las pruebas normales no necesitan ninguna BD ni cuenta AWS. El JAR seleccionado contiene solo sus
dependencias. Las fuentes opcionales están en src/postgresql/java y src/dynamodb/java.
Domain, application y los puertos siguen en src/main/java/${package}.
`verify` incluye Spotless; para aplicar el formato antes de verificar use `sh mvnw spotless:apply`.

PostgreSQL: DB_URL es una URL JDBC externa. Flyway ejecuta `src/main/resources/db/migration`;
por defecto habilitado solo para PostgreSQL. `FLYWAY_ENABLED=false` requiere que el despliegue
haya aplicado el esquema. No usar ddl-auto ni insertar datos sensibles en migraciones.
DynamoDB: tabla existente con clave de partición `id` tipo String; no sort key.
La identidad AWS se obtiene de la cadena estándar (preferir rol de workload); permisos mínimos
GetItem/PutItem sobre la tabla. No colocar access keys en YAML. En pruebas se usa DynamoDB Local.
Lecturas consistentes, timeout total 5 s y por intento 3 s. No se aprovisionan recursos AWS.
Health comprueba el proceso; no certifica acceso a la tabla DynamoDB. Añadir un indicador de
dependencia según los criterios de disponibilidad de plataforma antes del despliegue real.

## Pruebas con contenedores

```sh
sh mvnw -Dstorage=postgresql -Pdatabase-it clean verify
sh mvnw -Dstorage=dynamodb -Pdatabase-it clean verify
```

Requieren Docker en ejecución. Failsafe falla si no puede iniciar los contenedores.
Los tests de Flyway comprueban migración, validación y segunda ejecución sin cambios.
CI ejecuta ambas opciones. Informes: target/surefire-reports y target/failsafe-reports.

## Docker

```sh
docker build --build-arg STORAGE=memory -t sample-service .
docker run --rm -p 127.0.0.1:8080:8080 -e SPRING_PROFILES_ACTIVE=demo sample-service
```

Para PostgreSQL/DynamoDB cambie STORAGE e inyecte configuración por el mecanismo de secretos
del entorno. El runtime usa UID 10001. Nunca incluya secretos en argumentos de build.

## Evolución

Reemplazar el ejemplo técnico por un bounded context definido; no es un modelo bancario canónico.
Actualizar OpenAPI, casos de uso y adaptadores conjuntamente; mantener las reglas ArchUnit.
Antes de exposición real, completar identidad, permisos por recurso y operación según ADR-004.
Estándares: docs/standards/README.md. Decisiones y excepciones: docs/decisions/README.md.
