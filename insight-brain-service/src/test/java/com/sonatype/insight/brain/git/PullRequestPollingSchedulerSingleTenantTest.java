/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO(CLM-39981): Rebuild this regression test on top of Spring Boot test infrastructure.
 *
 * The Guice-era test bootstrap used by the original test no longer exists on this migration branch.
 * The original coverage verified that pull-request polling skips apps whose parent SCM configuration is
 * missing and still processes apps with a complete inherited configuration.
 */
@Disabled("TODO(CLM-39981): migrate this single-tenant scheduler regression test to Spring Boot test infrastructure")
public class PullRequestPollingSchedulerSingleTenantTest
{
  @Test
  public void springBootMigrationPending() {
    // Disabled at the class level until CLM-39981 migrates the original Guice-based test setup to Spring Boot
    // infrastructure.
  }
}
