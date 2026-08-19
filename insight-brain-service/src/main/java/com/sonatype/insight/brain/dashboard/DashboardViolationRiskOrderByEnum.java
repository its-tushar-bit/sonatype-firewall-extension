/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

/**
 * @since 1.38
 */
public enum DashboardViolationRiskOrderByEnum
{
  // TODO Remove COMPONENT_NAME after it is removed from the UI (https://sonatype.atlassian.net/browse/CLM-32985)
  // and after it is deployed once to MTIQ prod envs.
  AGE,
  APPLICATION_NAME,
  COMPONENT_NAME,
  POLICY_NAME,
  THREAT_LEVEL,
  POLICY_VIOLATION_ID;
}
