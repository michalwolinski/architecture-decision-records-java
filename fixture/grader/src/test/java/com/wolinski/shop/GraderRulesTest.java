package com.wolinski.shop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.wolinski.shop.money.Money;
import com.wolinski.shop.orders.api.OrderFacade;
import com.wolinski.shop.platform.ShopProblem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * The verdict. The agent never sees this file, and it runs against a clean copy of the agent's src/main:
 * its tests, its pom and any ArchUnit ignore file are thrown away first. It is stricter than the rules
 * the enforced arm can read: ADR-3 also scans the source for locals, ADR-5 is checked at runtime, and
 * ADR-7 has no visible rule at all. A class that predates a decision is exempt only while it is byte for
 * byte what it was: adding to it counts, because the exemption covers old code, not a place to hide new code.
 */
class GraderRulesTest {

    private static final Pattern MODULE = Pattern.compile("com\\.wolinski\\.shop\\.(\\w+)\\.(\\w+).*");
    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.wolinski.shop");

    private static String[] untouched(String... classes) {
        return Arrays.stream(classes).filter(GraderRulesTest::isUntouched).toArray(String[]::new);
    }

    private static boolean isUntouched(String simpleName) {
        try (InputStream pristine = GraderRulesTest.class.getResourceAsStream("/pristine/" + simpleName + ".java")) {
            Path current = Path.of("src/main/java/com/wolinski/shop/orders/internal", simpleName + ".java");
            return Files.exists(current) && new String(pristine.readAllBytes()).equals(Files.readString(current));
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static DescribedPredicate<JavaClass> exempt(String... classes) {
        Set<String> names = Set.of(untouched(classes));
        return DescribedPredicate.describe("exempt", javaClass -> names.contains(javaClass.getSimpleName()));
    }

    @Test
    void adr0002() {
        noClasses().that(DescribedPredicate.not(exempt("OrderReports"))).should(new ArchCondition<JavaClass>("use the internal package of another module") {
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
        }).check(PRODUCTION);
    }

    @Test
    void adr0003() throws IOException {
        noFields().that().areDeclaredInClassesThat(DescribedPredicate.not(exempt("OrderPricing"))).should().haveRawType(double.class).orShould().haveRawType(float.class).check(PRODUCTION);
        noMethods().that().areDeclaredInClassesThat(DescribedPredicate.not(exempt("OrderPricing"))).should().haveRawReturnType(double.class).orShould().haveRawReturnType(float.class).check(PRODUCTION);
        assertEquals(List.of(), sourcesMatching("\\b(double|float|Double|Float)\\b", untouched("OrderPricing")));
    }

    @Test
    void adr0004() {
        noClasses().that(DescribedPredicate.not(exempt("OrderReports"))).should().callMethod(Instant.class, "now")
                .orShould().callMethod(LocalDate.class, "now")
                .orShould().callMethod(LocalDate.class, "now", java.time.ZoneId.class)
                .orShould().callMethod(LocalDateTime.class, "now")
                .orShould().callMethod(OffsetDateTime.class, "now")
                .orShould().callMethod(ZonedDateTime.class, "now")
                .orShould().callMethod(System.class, "currentTimeMillis")
                .orShould().callConstructor(java.util.Date.class)
                .check(PRODUCTION);
    }

    /** Runtime, so a helper method cannot hide it. A shop that never calls the carrier does not violate this. */
    @Test
    void adr0005() throws InterruptedException {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            String id = orders.place("c-1", Money.pln(100), Instant.parse("2026-10-01T10:00:00Z"));
            orders.confirm(id);
            ShopHarness.await(() -> false == ShopHarness.CARRIER_CALLED_IN_TRANSACTION.isEmpty());
            assertEquals(false, ShopHarness.CARRIER_CALLED_IN_TRANSACTION.contains(true));
        }
    }

    @Test
    void adr0006() {
        noClasses().that().resideOutsideOfPackage("..notifications..").and(DescribedPredicate.not(exempt("OrderReturns")))
                .should().dependOnClassesThat().resideInAPackage("..notifications..")
                .check(PRODUCTION);
    }

    @Test
    void adr0007() {
        noClasses().that().resideInAPackage("..api..").and().doNotHaveSimpleName("PaymentsFacade")
                .should().dependOnClassesThat().resideInAPackage("..internal..")
                .check(PRODUCTION);
        noMethods().that().areDeclaredInClassesThat().resideInAPackage("..api..").and().doNotHaveName("paymentsOf")
                .should(new ArchCondition<JavaMethod>("expose an internal type") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        Stream.concat(method.getReturnType().getAllInvolvedRawTypes().stream(),
                                        method.getParameterTypes().stream().flatMap(type -> type.getAllInvolvedRawTypes().stream()))
                                .forEach(type -> events.add(new SimpleConditionEvent(method,
                                        type.getPackageName().contains(".internal"), method.getFullName())));
                    }
                })
                .check(PRODUCTION);
    }

    @Test
    void adr0008() throws IOException {
        assertEquals(List.of(), sourcesMatching("\\bBig(Decimal|Integer)\\b", untouched("OrderPricing")));
    }

    @Test
    void adr0009() {
        noMethods().that().areDeclaredInClassesThat(DescribedPredicate.not(exempt("OrderReports")))
                .should().beAnnotatedWith(Scheduled.class).check(PRODUCTION);
        noClasses().should().dependOnClassesThat().areAssignableTo(TaskScheduler.class)
                .orShould().dependOnClassesThat().areAssignableTo(ScheduledExecutorService.class)
                .check(PRODUCTION);
    }

    @Test
    void adr0010() {
        noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().areAssignableTo(Optional.class).check(PRODUCTION);
    }

    @Test
    void adr0011() {
        noClasses().that(DescribedPredicate.not(exempt("OrderReturns")))
                .and().doNotHaveSimpleName("Money").and().doNotHaveSimpleName("OrderRepository")
                .and().doNotHaveSimpleName("ShopProblem")
                .should(new ArchCondition<JavaClass>("throw anything but a ShopProblem") {
                    @Override
                    public void check(JavaClass origin, ConditionEvents events) {
                        origin.getConstructorCallsFromSelf().forEach(call -> {
                            JavaClass target = call.getTargetOwner();
                            boolean violates = target.isAssignableTo(RuntimeException.class)
                                    && false == target.isAssignableTo(ShopProblem.class);
                            events.add(new SimpleConditionEvent(origin, violates, call.getDescription()));
                        });
                        if (origin.isAssignableTo(RuntimeException.class) && false == origin.isAssignableTo(ShopProblem.class)) {
                            events.add(new SimpleConditionEvent(origin, true, origin.getName() + " is its own exception type"));
                        }
                    }
                }).check(PRODUCTION);
    }

    private static List<String> sourcesMatching(String regex, String... exemptClasses) throws IOException {
        Pattern pattern = Pattern.compile(regex);
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> false == List.of(exemptClasses).contains(path.getFileName().toString().replace(".java", "")))
                    .filter(path -> {
                        try {
                            return pattern.matcher(withoutComments(Files.readString(path))).find();
                        }
                        catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .map(Path::toString)
                    .toList();
        }
    }

    private static String withoutComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }
}
