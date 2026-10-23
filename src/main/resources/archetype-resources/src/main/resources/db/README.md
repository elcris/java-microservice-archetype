# Migraciones

`migration/` contiene SQL PostgreSQL versionado para Flyway. Solo se ejecuta al compilar
con `-Dstorage=postgresql` y arrancar con `FLYWAY_ENABLED=true` (predeterminado para ese motor).
El servicio memory/dynamodb no incorpora Flyway ni JDBC en su classpath.
Use `V2__descripcion.sql`, `V3__descripcion.sql`, etc. No edite migraciones ya aplicadas.
No incluya secretos ni datos de clientes. DBA revisa cada cambio y su plan de recuperación.
Flyway clean y baseline automático están desactivados. No mezclar schema.sql, JPA DDL y Flyway.
El rollback es una nueva migración compensatoria revisada; no existe rollback SQL automático.
La tabla demo_entries es únicamente el ejemplo técnico y debe sustituirse antes de usar negocio real.
DynamoDB se aprovisiona por IaC; Flyway no administra tablas, índices ni datos DynamoDB.
