/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

/**
 * The state for a violation policy is based on the conditions in the constraints of that violation policy.
 *
 * @see PolicyViolation#isWaived()
 *
 * @since 1.27
 */
public enum PolicyViolationState
{
  OPEN, WAIVED;
}
