/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

public enum ManualPullRequestImpossibilityReason
{
  /**
   * A pull request that cannot be created for the specified stage
   */
  UNSUPPORTED_STAGE,

  /**
   * Dependency is not direct or the right format
   */
  UNSUPPORTED_DEPENDENCY_TYPE,
  UNSUPPORTED_FORMAT,

  /**
   * Pull Request already exists
   */
  REMEDIATION_EVENT_EXISTS,

  /**
   * There is no suggested version or valid version change to create a Pull Request
   */
  NO_REMEDIATION_VERSION_AVAILABLE,
  INSUFFICIENT_PERMISSIONS,

  /**
   * Manual Pull Requests are disabled from SCM configuration
   */
  CONFIGURATION_DISABLED,

  /**
   * SCM is not configured
   */
  SCM_NOT_CONFIGURED,
  NOT_SUPPORTED_FOR_LICENSE,
  NOT_SUPPORTED_FOR_PROVIDER,
  NOT_SUPPORTED_FOR_REPOSITORY,
  UNSUPPORTED_OWNER_TYPE,

  NOT_SUPPORTED_FOR_MTIQ,

  NON_DEFAULT_BRANCH,
}
