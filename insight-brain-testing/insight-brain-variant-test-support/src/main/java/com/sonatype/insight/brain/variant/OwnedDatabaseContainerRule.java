/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;

/**
 * A per-variant, test-owned {@link DatabaseContainerRule} instance.
 *
 * <p>
 * The legacy {@link DatabaseContainerRule#getInstance(Class)} returns a JVM-wide singleton that holds
 * one mutable {@code DatabaseContainer} and tracks the "current test class" in static fields. The
 * variant test infrastructure boots exactly one server per variant per JVM and reuses it, so it does
 * not need — and does not want — that global mutable single slot. This subclass exists purely to
 * expose a public constructor: {@link DatabaseContainerRule}'s own constructor is {@code protected}
 * (singleton enforcement), and every method the variant path uses
 * ({@link DatabaseContainerRule#ensureInitializedForSpringContext()},
 * {@link DatabaseContainerRule#getDatabaseContainer()}, {@link #getDataSourceProvider()}, the four
 * data-store getters, and {@link DatabaseContainerRule#resetMocks()}) is an instance method that
 * never reads or writes the static {@code INSTANCE}/{@code currentTestClassType}. Only
 * {@code getInstance(...)} touches those.
 *
 * <p>
 * Each variant extension owns and caches one of these keyed by its {@code variantKey()}; the legacy
 * singleton is left entirely untouched for the ~2000 legacy tests that still depend on it.
 */
public class OwnedDatabaseContainerRule
    extends DatabaseContainerRule
{
  public OwnedDatabaseContainerRule() {
    super();
  }
}
