/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * JUnit 5 base for reused-context H2 component tests that extend {@link BrainInjectedTest} directly (i.e. below
 * {@code AbstractComponentTest} in the chain, without its per-test license/security setup).
 *
 * <p>
 * Like {@link AbstractComponentH2Test} it supplies the Spring TestContext Jupiter wiring
 * ({@code @ExtendWith(SpringExtension.class)}) that the inherited {@code @ContextConfiguration} needs under the
 * Jupiter engine, kept off the shared {@code SpringInjectedTest}/{@code BrainInjectedTest} chain so the JUnit-4
 * (vintage) tests remaining in {@code insight-brain-service} stay vintage-only. Converted tests extend this instead
 * of {@code BrainInjectedTest} and carry {@code @ComponentH2Test}.
 */
@ExtendWith(SpringExtension.class)
public abstract class AbstractBrainInjectedH2Test
    extends BrainInjectedTest
{
}
