# Fuentes y revisión

Revisión: 2026-10-23. Documentos locales leídos, sin modificarlos:
- agenteEspecializadoJavaSPHexV2.agent.md: Java 21/Boot 4, estructura, puertos, reglas, pruebas.
- agenteApenApiV2.agent.md: solicitud histórica; remite a lineamientos-openapi-bian.md.
- instructions-backend.md: fronteras, convenciones y reglas de la muestra previa.
- lineamientos-openapi-bian.md: OAS 3.1, recursos directos, RFC 9457, semántica e idempotencia.
- pocAgentJava/pom.xml y Maven Wrapper: línea de plataforma existente.

Consultas técnicas primarias:
- https://maven.apache.org/archetype/maven-archetype-plugin/
- https://maven.apache.org/archetype/archetype-models/archetype-descriptor/archetype-descriptor.html
- https://springdoc.org/ (línea 3.x para Boot 4)
- https://docs.spring.io/spring-boot/how-to/data-initialization.html
- https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html
- https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_PutItem.html
- https://repo.maven.apache.org/maven2/ (versiones publicadas y resolución real)

No se atribuye conformidad con un Service Domain BIAN porque no se suministró uno para el ejemplo.
No se encontraron docs/standards ni AGENTS.md en las rutas relevantes revisadas;
se incluye una adaptación explícita, no una copia supuesta de esos archivos.
