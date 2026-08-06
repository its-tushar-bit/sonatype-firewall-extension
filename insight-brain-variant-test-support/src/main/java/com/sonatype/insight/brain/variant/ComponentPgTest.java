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

/**
 * Marks a JUnit 5 component test that runs against the shared, reused Spring {@code
 * ApplicationContext} + a real PostgreSQL database fixture of the
 * {@code insight-brain-variant-test-component-pg} module.
 *
 * <p>
 * Identical in purpose to {@code @ComponentH2Test} — a converted component test keeps {@code extends
 * AbstractComponentTest} (or {@code AbstractServiceAuthzTest}) for the helper/injection surface and
 * adds this annotation so the ordered DB/search/entity/Mockito rule lifecycle (inert under the
 * Jupiter engine) is reproduced by {@link ComponentTestPgHarnessExtension}. The only difference from
 * {@code @ComponentH2Test} is that the harness steers the JVM-wide {@code DatabaseContainerRule} onto
 * a Postgres fixture, so converted classes do NOT need any per-class/per-method {@code @PostgresTest}
 * marker — drop those on conversion.
 *
 * <p>
 * The Spring wiring ({@code @ExtendWith(SpringExtension.class)} + {@code @ContextConfiguration}) is
 * inherited from the base chain, so it is not repeated here. Because the whole module uses a single
 * Postgres fixture and no {@code @DirtiesContext}, the Spring context and DB fixture are built once
 * and reused across every class. Tests that need a {@code customizeConfig(...)} override,
 * {@code @DirtiesContext}, or {@code @MockBean} must NOT use this annotation (they break context
 * reuse) — route them per {@code docs/component-test-module-recipe.md}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@ExtendWith(ComponentTestPgHarnessExtension.class)
public @interface ComponentPgTest
{
}
