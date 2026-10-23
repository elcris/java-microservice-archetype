#set( $symbol_dollar = '$' )
package ${package}.archunittests;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;

class HexagonalArchitectureTest {
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("${package}");

  @Test
  void domainIsPure() {
    classes()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage("java..", "${package}.domain..")
        .check(CLASSES);
  }

  @Test
  void applicationDependsOnlyOnDomainJdkAndAllowedAnnotations() {
    classes()
        .that()
        .resideInAPackage("..application..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            "java..",
            "${package}.application..",
            "${package}.domain..",
            "org.springframework.stereotype",
            "org.springframework.transaction.annotation")
        .check(CLASSES);
  }

  @Test
  void inboundCannotBypassPorts() {
    noClasses()
        .that()
        .resideInAPackage("..adapters.inbound..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..application..",
            "..adapters.outbound..",
            "..ports.outbound..",
            "org.springframework.data..",
            "org.springframework.jdbc..",
            "software.amazon..")
        .check(CLASSES);
  }

  @Test
  void outboundCannotCallInbound() {
    noClasses()
        .that()
        .resideInAPackage("..adapters.outbound..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..adapters.inbound..", "..application..", "..ports.inbound..")
        .check(CLASSES);
  }

  @Test
  void mappersDoNotOrchestrate() {
    noClasses()
        .that()
        .resideInAPackage("..mapper..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..ports..", "..application..", "..adapters.outbound..")
        .check(CLASSES);
  }

  @Test
  void controllersBelongToRest() {
    classes()
        .that()
        .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
        .should()
        .resideInAPackage("..infrastructure.adapters.inbound.rest..")
        .check(CLASSES);
  }

  @Test
  void noFieldInjection() {
    GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(CLASSES);
  }

  @Test
  void noLayerCycles() {
    SlicesRuleDefinition.slices()
        .matching("${package}.(*)..")
        .should()
        .beFreeOfCycles()
        .check(CLASSES);
  }
}
