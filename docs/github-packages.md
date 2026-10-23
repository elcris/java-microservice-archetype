# Distribución del arquetipo

Propuesta: organización corporativa privada del banco + repositorio `java-microservice-archetype`.
GitHub aloja fuentes y GitHub Packages aloja el artefacto Maven. También puede usarse Nexus/Artifactory.
La instalación local después de clonar es el camino inicial y no requiere publicar un paquete.

El workflow de publicación usa la URL calculada desde GITHUB_REPOSITORY y el server-id `github`.
Antes del primer release confirmar groupId, propiedad del paquete y permisos de Actions.
La versión 1.0.0 del POM es propuesta; no sobrescribir releases ya publicados.

Ejemplo de settings.xml fuera del repositorio (reemplazar ORGANIZATION/REPOSITORY):

```xml
<settings>
  <servers><server><id>github</id><username>${env.GITHUB_ACTOR}</username><password>${env.GITHUB_TOKEN}</password></server></servers>
  <profiles><profile><id>corporate-archetypes</id><repositories><repository>
    <id>github</id><url>https://maven.pkg.github.com/ORGANIZATION/REPOSITORY</url>
    <releases><enabled>true</enabled></releases>
  </repository></repositories></profile></profiles>
  <activeProfiles><activeProfile>corporate-archetypes</activeProfile></activeProfiles>
</settings>
```

Con ese settings ejecutar la misma generación del README, omitiendo archetypeCatalog=local;
las coordenadas explícitas permiten resolver el JAR del repositorio configurado.
La autenticación a Packages debe seguir la política corporativa; no copiar tokens a este proyecto.
Documentación oficial: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry
