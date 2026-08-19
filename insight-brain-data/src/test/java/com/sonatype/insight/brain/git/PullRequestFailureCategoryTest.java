/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestFailureCategoryTest
{
  @Test
  public void manifestComponentNotFound_isNotRetryable() {
    assertThat(PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND.isRetryable()).isFalse();
  }

  @Test
  public void scmError_isRetryable() {
    assertThat(PullRequestFailureCategory.SCM_ERROR.isRetryable()).isTrue();
  }

  @Test
  public void unknown_isRetryable() {
    assertThat(PullRequestFailureCategory.UNKNOWN.isRetryable()).isTrue();
  }
}
