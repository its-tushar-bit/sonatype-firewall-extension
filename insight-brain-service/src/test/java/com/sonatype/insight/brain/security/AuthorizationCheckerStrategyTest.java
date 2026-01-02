/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static com.sonatype.insight.brain.security.AuthorizationMembershipQueryStrategy.AUTHORIZATION_CHECKER_MEMBERSHIP_QUERY_STRATEGY_ENV;
import static com.sonatype.insight.brain.security.AuthorizationPermissionEntityFilterStrategy.AUTHORIZATION_PERMISSION_ENTITY_FILTER_STRATEGY_ENV;

/**
 * Strategy combination test for AuthorizationChecker functionality.
 * <p>
 * Runs all AuthorizationCheckerTest cases with different combinations of:
 * <ul>
 *   <li>AuthorizationPermissionEntityFilterStrategy (SEQUENTIAL, PARALLEL, EXECUTOR_FIXED, AUTO)</li>
 *   <li>AuthorizationMembershipQueryStrategy (DIRECT_CONTEXT_ID, FULL_MEMBERSHIP_MAPPING_CONTEXT_ID)</li>
 * </ul>
 * <p>
 * This ensures that authorization logic works correctly across all strategy combinations.
 */
@RunWith(Parameterized.class)
public class AuthorizationCheckerStrategyTest
    extends AuthorizationCheckerTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Parameters(name = "filterStrategy={0}, queryStrategy={1}")
  public static Collection<Object[]> data() {
    Object[][] combinations = new Object[
        AuthorizationPermissionEntityFilterStrategy.values().length *
            AuthorizationMembershipQueryStrategy.values().length
        ][];

    int index = 0;
    for (AuthorizationPermissionEntityFilterStrategy filterStrategy :
        AuthorizationPermissionEntityFilterStrategy.values()) {
      for (AuthorizationMembershipQueryStrategy queryStrategy : AuthorizationMembershipQueryStrategy.values()) {
        combinations[index++] = new Object[]{filterStrategy, queryStrategy};
      }
    }

    return Arrays.asList(combinations);
  }

  private final AuthorizationPermissionEntityFilterStrategy filterStrategy;

  private final AuthorizationMembershipQueryStrategy queryStrategy;

  public AuthorizationCheckerStrategyTest(
      AuthorizationPermissionEntityFilterStrategy filterStrategy,
      AuthorizationMembershipQueryStrategy queryStrategy)
  {
    this.filterStrategy = filterStrategy;
    this.queryStrategy = queryStrategy;
  }

  @Override
  public void setUp() {
    // Set environment variables for this test combination
    environmentVariables.set(AUTHORIZATION_PERMISSION_ENTITY_FILTER_STRATEGY_ENV, filterStrategy.name());
    environmentVariables.set(AUTHORIZATION_CHECKER_MEMBERSHIP_QUERY_STRATEGY_ENV, queryStrategy.name());

    // Call parent setup
    super.setUp();
  }
}
