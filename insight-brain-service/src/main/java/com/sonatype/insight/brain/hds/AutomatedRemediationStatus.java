/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.hds;

public enum AutomatedRemediationStatus
{
  MANUAL_PULL_REQUEST_POSSIBLE,
  MANUAL_PULL_REQUEST_NOT_POSSIBLE,
  PULL_REQUEST_CREATION_PENDING,
  PULL_REQUEST_CREATION_FAILED,
  PULL_REQUEST
}
