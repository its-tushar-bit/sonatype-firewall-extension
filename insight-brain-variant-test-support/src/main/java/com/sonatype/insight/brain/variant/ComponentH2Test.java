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
 * Because the whole module uses a single H2 fixture and no {@code @DirtiesContext}, the Spring
 * context and DB fixture are built once and reused across every class — see
 * {@code docs/component-test-module-recipe.md}. Tests that need a different fixture
 * ({@code @PostgresTest}), a {@code customizeConfig(...)} override, {@code @DirtiesContext}, or
 * {@code @MockBean} must NOT use this annotation (they break context reuse) — route them per the
 * recipe.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@ExtendWith(ComponentTestDbHarnessExtension.class)
public @interface ComponentH2Test
{
}
