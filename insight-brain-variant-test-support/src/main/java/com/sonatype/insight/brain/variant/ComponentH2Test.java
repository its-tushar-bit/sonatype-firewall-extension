/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

/**
 * Marks a JUnit 5 component test that runs against the shared, reused Spring {@code
 * ApplicationContext} + H2 (in-memory) database fixture of the
 * {@code insight-brain-variant-test-component-h2} module.
 *
 * <p>
 * A converted component test keeps {@code extends AbstractComponentTest} (or
 * {@code AbstractServiceAuthzTest}) — that is where the helper methods ({@code lookup},
 * {@code setBaseUrl}, {@code createReport}, the {@code grant*} authz helpers, …) and the injected
 * fields ({@code tempEntity}, {@code daoFactory}, {@code subject}, {@code insightConfig}) live — but
 * adds this annotation so the ordered DB/search/entity/Mockito rule lifecycle (which is inert under
 * the Jupiter engine) is reproduced by {@link ComponentTestDbHarnessExtension}. The Spring wiring
 * ({@code @ExtendWith(SpringExtension.class)} + {@code @ContextConfiguration}) is inherited from the
 * base chain, so it does not need to be repeated here.
 *
 * <p>
 * <b>Context reuse (why the explicit {@link TestExecutionListeners}):</b> {@code AbstractComponentTest}
 * carries {@code @DirtiesContext(AFTER_CLASS)} so the JUnit-4 (vintage) subclasses that remain in
 * {@code insight-brain-service} keep their per-class Spring-context isolation (unchanged behavior).
 * This annotation opts the reused-context module OUT of that, per-cohort, by re-declaring the
 * TestContext listeners with {@code inheritListeners = false} and the standard Spring listener set
 * MINUS the two {@code DirtiesContext*TestExecutionListener}s. The inherited {@code @DirtiesContext}
 * therefore has no processor here and is inert, so the Spring context + single H2 fixture are built
 * once and reused across every class in the module — without changing service-module behavior.
 * (Mockito is driven by {@code MockitoAnnotations.openMocks(...)} in {@link ComponentTestDbHarnessExtension},
 * so the Spring Boot {@code @MockBean} listeners are intentionally not needed here.)
 *
 * <p>
 * Tests that need a different fixture ({@code @PostgresTest}), a {@code customizeConfig(...)} override,
 * an actual {@code @DirtiesContext} rebuild, or {@code @MockBean} must NOT use this annotation (they
 * break context reuse) — route them per the recipe (see {@code docs/component-test-module-recipe.md}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@ExtendWith(ComponentTestDbHarnessExtension.class)
@TestExecutionListeners(
    listeners = {
      ServletTestExecutionListener.class,
      ApplicationEventsTestExecutionListener.class,
      DependencyInjectionTestExecutionListener.class,
      TransactionalTestExecutionListener.class,
      SqlScriptsTestExecutionListener.class,
      EventPublishingTestExecutionListener.class
    },
    inheritListeners = false)
public @interface ComponentH2Test
{
}
