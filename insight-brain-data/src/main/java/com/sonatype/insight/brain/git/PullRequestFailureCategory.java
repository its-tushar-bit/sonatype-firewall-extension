/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

/**
 * Classification of a PR-creation failure used to drive UI behavior
 * (e.g. whether the Retry button should be enabled).
 */
public enum PullRequestFailureCategory
{
  MANIFEST_COMPONENT_NOT_FOUND,
  SCM_ERROR,
  UNKNOWN;

  public boolean isRetryable() {
    return this != MANIFEST_COMPONENT_NOT_FOUND;
  }
}
