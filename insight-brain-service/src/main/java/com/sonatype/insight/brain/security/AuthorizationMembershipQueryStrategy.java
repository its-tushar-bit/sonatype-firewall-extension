/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

/**
 * Query strategies for retrieving membership context IDs during authorization checks.
 * <p>
 * Controls how user membership context IDs are retrieved from the database:
 * <ul>
 * <li><strong>DIRECT_CONTEXT_ID</strong>faster, lower memory usage</li>
 * <li><strong>FULL_MEMBERSHIP_MAPPING_CONTEXT_ID</strong> - Traditional approach (default)</li>
 * </ul>
 * <p>
 * Configured via {@code AUTHORIZATION_CHECKER_MEMBERSHIP_QUERY_STRATEGY} environment variable.
 */
public enum AuthorizationMembershipQueryStrategy
{
  /**
   * Optimized query that directly returns context IDs without object materialization.
   * <p>
   * faster with less memory usage. Uses
   * {@code MembershipMappingDAO.getContextIdsByUserCaseInsensitiveAndGroupsAndRoles()}.
   */
  DIRECT_CONTEXT_ID,

  /**
   * Traditional approach that retrieves full MembershipMapping objects then extracts context IDs.
   * <p>
   * Higher memory usage but provides full object compatibility. Uses
   * {@code MembershipMappingDAO.getByUserCaseInsensitiveAndGroupsAndRoles().stream().map(...)}.
   */
  FULL_MEMBERSHIP_MAPPING_CONTEXT_ID;

  public static final String AUTHORIZATION_CHECKER_MEMBERSHIP_QUERY_STRATEGY_ENV =
      "AUTHORIZATION_CHECKER_MEMBERSHIP_QUERY_STRATEGY";

  /**
   * Resolves the membership query strategy from environment configuration.
   * <p>
   * Reads {@code AUTHORIZATION_CHECKER_MEMBERSHIP_QUERY_STRATEGY} environment variable.
   * Returns {@code FULL_MEMBERSHIP_MAPPING_CONTEXT_ID} if not set or invalid.
   *
   * @return the configured strategy, or default if not configured
   */
  public static AuthorizationMembershipQueryStrategy getStrategyFromEnv() {
    String strategyValue = System.getenv(AUTHORIZATION_CHECKER_MEMBERSHIP_QUERY_STRATEGY_ENV);

    try {
      return AuthorizationMembershipQueryStrategy.valueOf(strategyValue.toUpperCase());
    }
    catch (Exception ignore) {
      return FULL_MEMBERSHIP_MAPPING_CONTEXT_ID;
    }
  }
}
