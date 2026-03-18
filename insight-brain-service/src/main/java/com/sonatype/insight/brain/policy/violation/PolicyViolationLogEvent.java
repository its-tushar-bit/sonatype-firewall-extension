/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

/**
 * @since 1.60
 */
public enum PolicyViolationLogEvent
{
  CREATE, //

  FIX, //

  /**
   * @deprecated use GRANT_LEGACY_STATUS instead
   */
  @Deprecated
  GRANDFATHER, //

  /**
   * @deprecated use REVOKE_LEGACY_STATUS instead
   */
  @Deprecated
  UNGRANDFATHER, //

  GRANT_LEGACY_STATUS, //

  REVOKE_LEGACY_STATUS, //

  WAIVE, //

  AUTOWAIVE,

  UNWAIVE, //

  UNAUTOWAIVE,

  CLEAR
}
