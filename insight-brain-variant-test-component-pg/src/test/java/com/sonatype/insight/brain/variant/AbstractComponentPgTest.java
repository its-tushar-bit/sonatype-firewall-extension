/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * JUnit 5 base for the reused-context Postgres component tests in {@code insight-brain-variant-test-component-pg}.
 *
 * <p>
 * It supplies the Spring TestContext Jupiter wiring ({@code @ExtendWith(SpringExtension.class)}) that the
 * inherited {@code @ContextConfiguration} needs under the Jupiter engine. This is deliberately declared HERE
 * (a cohort-only base) rather than on the shared {@code SpringInjectedTest}/{@code AbstractComponentTest}: putting
 * {@code @ExtendWith(SpringExtension.class)} on the shared chain would make the many JUnit-4 (vintage) tests that
 * remain in {@code insight-brain-service} Jupiter-discoverable, which defeats the JUnit-4 {@code @Category} group
 * exclusions (e.g. {@code PostgresTestCategory}) they rely on in CI.
 *
 * <p>
 * Converted component tests extend this instead of {@code AbstractComponentTest} and additionally carry
 * {@code @ComponentPgTest} (the DB/search/entity/Mockito harness + the Postgres fixture steering + the
 * DirtiesContext-listener opt-out for single-context reuse).
 */
@ExtendWith(SpringExtension.class)
public abstract class AbstractComponentPgTest
    extends AbstractComponentTest
{
}
