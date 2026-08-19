/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

/**
 * Spring-based test base class that provides database and dependency injection support.
 *
 * <p>
 * This class handles creation of the four data store classes for tests. The {@link DatabaseContainerRule} is a junit
 * rule
 * to create the instances and inject them into the legacy *Provider classes. Ultimately any test that accesses a
 * datastore
 * needs to extend this base class.
 * </p>
 *
 * <p>
 * <b>Migration Note:</b> This class uses the Spring-based test infrastructure. Tests should use
 * `@Inject` (jakarta.inject) for dependency injection. The old module-style customization hook is no
 * longer supported - use `@TestConfiguration` inner classes instead.
 * </p>
 */
@ContextConfiguration
public abstract class BrainInjectedTest
    extends SpringBrainInjectedTest
{
  /**
   * Inner configuration class that provides common test beans.
   * Tests can override this by providing their own @TestConfiguration.
   */
  @TestConfiguration
  static class CommonTestConfiguration
  {

    @Bean(destroyMethod = "")
    @Primary
    public ExecutorThreadPools executorThreadPools() {
      return new DefaultExecutorThreadPools();
    }

    @Bean
    public com.sonatype.insight.jaxrs.error.ErrorResponseGenerator errorResponseGenerator() {
      return new com.sonatype.insight.jaxrs.error.ErrorResponseGenerator();
    }
  }
}
