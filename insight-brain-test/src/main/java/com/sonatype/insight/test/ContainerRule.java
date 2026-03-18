/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.GenericContainer;

/**
 * A JUnit 4 {@link TestRule} wrapper for Testcontainers {@link GenericContainer}.
 * <p>
 * Testcontainers 2.x removed the built-in {@code TestRule} implementation to avoid
 * a hard dependency on JUnit 4. This wrapper restores the {@code @Rule} and
 * {@code @ClassRule} functionality by managing the container lifecycle.
 * <p>
 * Example usage:
 *
 * <pre>
 * {@code @ClassRule}
 * public static ContainerRule<PostgreSQLContainer<?>> postgres =
 *     new ContainerRule<>(new PostgreSQLContainer<>("postgres:13"));
 * </pre>
 *
 * @param <T> the type of container being wrapped
 */
public class ContainerRule<T extends GenericContainer<?>>
    implements TestRule
{
  private final T container;

  /**
   * Create a new container rule wrapping the given container.
   *
   * @param container the container to manage
   */
  public ContainerRule(T container) {
    this.container = container;
  }

  /**
   * Get the wrapped container instance.
   * <p>
   * Use this to access the container's methods (e.g., {@code getJdbcUrl()},
   * {@code getEndpoint()}, etc.).
   *
   * @return the container
   */
  public T getContainer() {
    return container;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement()
    {
      @Override
      public void evaluate() throws Throwable {
        container.start();
        try {
          base.evaluate();
        }
        finally {
          container.stop();
        }
      }
    };
  }
}
