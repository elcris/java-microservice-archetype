# Decisiones y excepciones

Estado: decisiones de implementación del template; pendientes de gobierno corporativo.

## ADR-001 — Maven Archetype y GitHub
Repositorio privado propuesto: `java-microservice-archetype`, dentro de la organización
que el banco designe. Coordenadas propuestas del generador: `co.com.bmm.platform:java-microservice-archetype:1.0.0`.
No se afirma propiedad de una organización, dominio o repositorio existente. El código fuente
vive en GitHub; el JAR del arquetipo se distribuye por GitHub Packages o repositorio Maven corporativo.
Una URL GitHub no sustituye a un repositorio Maven. Para empezar basta clonar e instalar localmente.
La versión del generador se fija al generar cada servicio. Actualizar el arquetipo no modifica
automáticamente microservicios ya generados. Los servicios tienen groupId/artifactId/package/version propios.

## ADR-002 — Fronteras hexagonales
Paquetes de domain/application/infrastructure según el agente v2. Un módulo Maven por servicio,
con ArchUnit imponiendo las fronteras. Los motores opcionales agregan fuentes y dependencias
en compile-time; el núcleo de dominio y aplicación nunca contiene SDKs o JDBC.
Spring MVC bloqueante. No WebFlux, Kafka, JPA, outbox ni SDKs innecesarios.

## ADR-003 — Persistencia opcional y atomicidad
`persistence` en generación escribe `.mvn/maven.config`; después se selecciona con
`-Dstorage=memory|postgresql|dynamodb`. No usar `-Ppostgresql` ni combinar perfiles manualmente.
Siempre `clean verify` al cambiar motor: elimina clases residuales del motor anterior.
Memory predeterminado: ConcurrentHashMap.putIfAbsent. Es efímero y no coordina réplicas.
PostgreSQL: INSERT ON CONFLICT DO NOTHING y lectura del valor persistido, autocommit, sin borrado
en este ejemplo. DynamoDB: PutItem condicional, ALL_OLD y GetItem consistente en una región.
Una repetición devuelve el mismo recurso; contenido distinto devuelve 409.
La semántica demostrada es create-only con id del cliente; no es un PUT de actualización general.
No se necesita @Transactional para una sola escritura atómica; cambios de varias escrituras
requerirán un límite transaccional en application y nuevas pruebas. No se promete exactly-once.
Al activar PostgreSQL se incorpora Flyway. FLYWAY_ENABLED=false permite migraciones gestionadas
por el proceso de despliegue, que deberá aplicar y validar el esquema antes de servir tráfico.
DynamoDB requiere tabla existente con partition key `id` String, sin sort key. Aprovisionar por IaC.
No se crean tablas AWS en el arranque ni se requieren credenciales en el repositorio.

## ADR-004 — Seguridad pendiente de identidad corporativa
No se inventan issuer, audience, scopes, roles ni pertenencia de recursos. Por defecto solo
health está permitido y los demás endpoints devuelven 403 ProblemDetail. El servidor escucha
en loopback. `demo` permite las rutas de ejemplo y Swagger, sin sesión ni cookies; CSRF
se desactiva solo para este modo local sin autenticación del navegador. No activar demo en producción.
El contrato indica security: [] exclusivamente para demo. Antes de exponer negocio deben
integrarse identidad real, autorización por recurso y sus pruebas negativas y contrato OpenAPI.
El ejemplo no afirma cumplimiento bancario, legal ni BIAN.

## ADR-005 — Contrato y observabilidad
Contrato fuente en resources/openapi/openapi.yaml; Swagger UI lo carga directamente.
No se mantiene otra especificación generada por anotaciones que pueda divergir.
RFC 9457 con URN estables locales, code y errors; sustituir URN por catálogo corporativo al adoptarlo.
Se aplica la recomendación moderna del agente v2 y OpenAPI frente al formato de error antiguo
de instructions-backend.md. Correlation-ID validado en borde HTTP y logs ECS; no equivale
a tracing distribuido W3C. Actuator instrumenta HTTP; exposición externa solo health/info,
info denegado por seguridad predeterminada. Exportación de métricas y traces es decisión de plataforma.

## ADR-006 — Adaptación de las fuentes
No se copia com.hackerrank.challenge ni com.example.orders: eran ejemplos de otros proyectos.
H2 y Database/Schema.sql/Data.sql no son reglas universales; se reemplazan por memory real
sin BD y Flyway PostgreSQL opcional, conforme a la solicitud actual y al agente v2.
El archivo agenteApenApiV2.agent.md contiene una solicitud histórica para crear otro agente;
no es un agente OpenAPI completo. Su orden de descartar documentos y pedir confirmación
pertenece al adjunto, no a la solicitud actual. Se usa su referencia a lineamientos-openapi-bian.md.
No se copian cuentas, pedidos, créditos ni modelos canónicos bancarios no definidos.
Entry/demo_entries son un ejemplo técnico explícito; sustituirlo antes del primer despliegue funcional.
No se incluyen tablas opcionales en el núcleo ni se ejecutan migraciones DynamoDB con Flyway.

## ADR-007 — Verificación y límites
Java 21 y Spring Boot 4.1.1 conservan la línea presente en la muestra local inspeccionada.
Springdoc 3.1.1, ArchUnit 1.5.1, AWS SDK 2.57.3 y plugins fijados, consultados en Maven Central.
CI genera proyectos con paquetes diferentes y prueba los tres motores. Las pruebas de motores
reales usan Testcontainers y fallan si Docker no existe; no se omiten silenciosamente.
En el equipo de desarrollo Docker no tenía daemon disponible durante la verificación inicial.
Los resultados ejecutados y pendientes están separados en EVIDENCE.md.
Las etiquetas de contenedor son explícitas; el pipeline de promoción corporativo deberá fijar
digests aprobados. Se incorpora Spotless con Google Java Format; no se presume un escáner corporativo no proporcionado.

El formateador fijado es Google Java Format 1.25.2 mediante Spotless 3.10.4, verificado con Java 21.
El postprocesado del arquetipo normaliza archivos de texto a LF para que el primer verify sea portable.
