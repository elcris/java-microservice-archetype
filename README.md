# Generador de microservicios Java para Banco Mundo Mujer

Propuesta de repositorio privado: **java-microservice-archetype**, en la organización GitHub
designada por el banco. Coordenadas propuestas: `co.com.bmm.platform:java-microservice-archetype:1.0.0`.
No se creó ningún repositorio remoto. Los nombres de servicio, paquete y coordenadas generadas son parámetros.

## Instalar y generar

Requisitos: JDK 21. Maven 3.9.16 se descarga mediante Wrapper. En Windows use `mvnw.cmd`;
en Linux/macOS `sh mvnw`. Desde la raíz del generador:

```sh
sh mvnw clean install
sh mvnw org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeCatalog=local \
  -DarchetypeGroupId=co.com.bmm.platform \
  -DarchetypeArtifactId=java-microservice-archetype \
  -DarchetypeVersion=1.0.0 \
  -DgroupId=co.com.bmm.services \
  -DartifactId=sample-service \
  -Dpackage=co.com.bmm.services.sample \
  -Dversion=0.1.0-SNAPSHOT \
  -Dpersistence=memory \
  -DinteractiveMode=false \
  -DoutputDirectory=target/generated
cd target/generated/sample-service
sh mvnw clean verify
java -jar target/service.jar --spring.profiles.active=demo
```

En PowerShell pegue el comando de generación en una línea, o use acento grave como continuación
en vez de `\`. `groupId`, `artifactId`, `package`, `version` se validan; `persistence` permite
**memory (predeterminado), postgresql o dynamodb**. El package debe ser válido en Java (sin palabras reservadas).

Cambiar posteriormente: `sh mvnw -Dstorage=postgresql clean verify` o `-Dstorage=dynamodb`.
El valor queda persistido únicamente si se edita `.mvn/maven.config`. Siempre limpiar al cambiar.
No usar activación manual `-Ppostgresql`/`-Pdynamodb`: el selector único es `-Dstorage`.

| Opción | Dependencias/servicios | Migraciones |
|---|---|---|
| memory | ConcurrentHashMap, sin base de datos | No se ejecutan |
| postgresql | JDBC, driver PostgreSQL, Flyway | resources/db/migration |
| dynamodb | AWS SDK v2, tabla existente e IAM | IaC del entorno; no Flyway |

Swagger: `/swagger-ui/index.html`; contrato fuente en `src/main/resources/openapi/openapi.yaml`.
Actuator health: `/actuator/health`. El modo normal deniega la API; demo abre el ejemplo local.
La seguridad de negocio debe completarse con el proveedor de identidad del banco.

## Estructura generada

```text
src/main/java/<package>/
  domain/{models/entry,ports/inbound,ports/outbound,exceptions}
  application/service/entry
  infrastructure/{adapters/inbound/rest/v1,adapters/outbound/persistence/memory,
                  configuration,security,common/exception,common/observability}
src/postgresql/java/<package>/infrastructure/...
src/dynamodb/java/<package>/infrastructure/...
src/main/resources/{openapi/openapi.yaml,db/migration,application*.yaml}
src/test/java/<package>/{domain,application,infrastructure,archunittests}
src/{postgresql-test,dynamodb-test,postgresql-it,dynamodb-it}/java/<package>/...
```

Fuentes del arquetipo: `src/main/resources/archetype-resources`. Archivos Java usan `${package}`;
coordenadas del servicio usan `${groupId}`, `${artifactId}` y `${version}`. No mantener una copia
independiente de un servicio base: el arquetipo es la fuente única. `${symbol_dollar}` escapa
expresiones que deben llegar intactas al proyecto generado. YAML y workflows no son filtrados por Velocity.

## Validación del generador

```sh
sh mvnw clean install
python scripts/verify_generator.py
```

Genera servicios con diferentes coordenadas, valida todos los perfiles, inspecciona las dependencias
empaquetadas y arranca el JAR memory para comprobar health, escritura/lectura, replay/conflicto,
errores, Swagger y logs JSON. Compila las pruebas de integración de motores pero no las ejecuta
sin `--database-it`, que activa PostgreSQL y DynamoDB Local mediante Testcontainers.
La verificación incluye Spotless/Google Java Format; `mvnw spotless:apply` aplica el formato en un servicio generado.
CI ejecuta la validación y las integraciones PostgreSQL y DynamoDB Local en Linux con Docker.
Para el uso normal de los servicios solo se requiere Java/Maven, no Python.

## Publicar la base en GitHub

1. Crear un repositorio privado `java-microservice-archetype` en la organización autorizada.
2. Subir estos archivos, configurar revisiones requeridas y rama protegida con check CI.
3. Validar las coordenadas propuestas en pom.xml según gobierno de paquetes del banco.
4. El workflow Publish Maven Package usa GitHub Packages y `GITHUB_TOKEN` con packages:write.
   Se ejecuta manualmente tras verificar; no contiene credenciales fijas ni publica automáticamente por push.
5. Para consumidores remotos, configurar `github` en settings.xml con credenciales externas
   autorizadas para leer el paquete privado. Ver docs/github-packages.md.

No se ha afirmado disponibilidad de un nombre de organización GitHub ni publicado paquetes.
Decisiones/excepciones: docs/decisions/README.md. Evidencia ejecutada: EVIDENCE.md.
