package com.wolinski.shop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * Each rule is one accepted decision in docs/adr. Change the decision first, then the rule.
 * A class named here predates its decision, as the record says. No new names.
 */
@AnalyzeClasses(packages = "com.wolinski.shop", importOptions = DoNotIncludeTests.class)
class ArchitectureTest {

    private static final Pattern MODULE = Pattern.compile("com\\.wolinski\\.shop\\.(\\w+)\\.(\\w+).*");

    @ArchTest
    static final ArchRule adr0002_modulesTalkThroughTheirApi = noClasses()
            .that().doNotHaveSimpleName("OrderReports")
            .should(new ArchCondition<JavaClass>("use the internal package of another module") {
                @Override
                public void check(JavaClass origin, ConditionEvents events) {
                    origin.getDirectDependenciesFromSelf().forEach(dependency -> {
                        Matcher from = MODULE.matcher(origin.getPackageName() + ".x");
                        Matcher to = MODULE.matcher(dependency.getTargetClass().getPackageName() + ".x");
                        boolean violates = from.matches() && to.matches()
                                && "internal".equals(to.group(2))
                                && false == from.group(1).equals(to.group(1));
                        events.add(new SimpleConditionEvent(origin, violates, dependency.getDescription()));
                    });
                }
            })
            .because("docs/adr/0002: add what you need to the other module's api package");

    @ArchTest
    static final ArchRule adr0003_noFloatingPointFields = noFields()
            .that().areDeclaredInClassesThat().doNotHaveSimpleName("OrderPricing")
            .should().haveRawType(double.class).orShould().haveRawType(float.class)
            .because("docs/adr/0008: amounts are Money, rates are whole basis points");

    @ArchTest
    static final ArchRule adr0003_noFloatingPointSignatures = noMethods()
            .should().haveRawReturnType(double.class).orShould().haveRawReturnType(float.class)
            .because("docs/adr/0008: amounts are Money, rates are whole basis points");

    @ArchTest
    static final ArchRule adr0003_noParsingIntoFloatingPoint = noClasses()
            .that().doNotHaveSimpleName("OrderPricing")
            .should().callMethod(Double.class, "parseDouble", String.class)
            .orShould().callMethod(Double.class, "valueOf", String.class)
            .orShould().callMethod(Float.class, "parseFloat", String.class)
            .orShould().callMethod(Float.class, "valueOf", String.class)
            .because("docs/adr/0008: amounts are Money, rates are whole basis points");

    @ArchTest
    static final ArchRule adr0004_timeComesFromTheClock = noClasses()
            .that().doNotHaveSimpleName("OrderReports")
            .should().callMethod(Instant.class, "now")
            .orShould().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(OffsetDateTime.class, "now")
            .orShould().callMethod(ZonedDateTime.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .because("docs/adr/0004: inject java.time.Clock");

    @ArchTest
    static final ArchRule adr0005_noCarrierCallInATransaction = noMethods()
            .that().areAnnotatedWith(Transactional.class)
            .and().areDeclaredInClassesThat().doNotHaveSimpleName("OrderReturns")
            .should(new ArchCondition<com.tngtech.archunit.core.domain.JavaMethod>("call a remote gateway") {
                @Override
                public void check(com.tngtech.archunit.core.domain.JavaMethod method, ConditionEvents events) {
                    for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                        boolean remote = call.getTargetOwner().getSimpleName().endsWith("Gateway");
                        events.add(new SimpleConditionEvent(method, remote, call.getDescription()));
                    }
                }
            })
            .because("docs/adr/0005: commit first, call the remote system after it");

    @ArchTest
    static final ArchRule adr0006_onlyNotificationsUsesNotifications = noClasses()
            .that().resideOutsideOfPackage("..notifications..")
            .and().doNotHaveSimpleName("OrderReturns")
            .should().dependOnClassesThat().resideInAPackage("..notifications..")
            .because("docs/adr/0006: publish an event from your api package instead");

    @ArchTest
    static final ArchRule adr0008_noBigDecimal = noClasses()
            .that().doNotHaveSimpleName("OrderPricing")
            .should().dependOnClassesThat().areAssignableTo(BigDecimal.class)
            .because("docs/adr/0008: rates are whole basis points in a long");

    @ArchTest
    static final ArchRule adr0009_recurringWorkIsAShopJob = noMethods()
            .that().areDeclaredInClassesThat().doNotHaveSimpleName("OrderReports")
            .should().beAnnotatedWith(Scheduled.class)
            .because("docs/adr/0009: implement platform.ShopJob");

    @ArchTest
    static final ArchRule adr0010_noOptionalInApi = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().areAssignableTo(Optional.class)
            .because("docs/adr/0010: return a <Thing>Lookup record");

    @ArchTest
    static final ArchRule adr0011_domainFailuresAreShopProblems = noClasses()
            .that().doNotHaveSimpleName("Money").and().doNotHaveSimpleName("OrderReturns")
            .should().callConstructor(IllegalStateException.class, String.class)
            .orShould().callConstructor(IllegalArgumentException.class, String.class)
            .because("docs/adr/0011: throw platform.ShopProblem with a stable code");
}
