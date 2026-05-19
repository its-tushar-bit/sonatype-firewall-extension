/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlExceptionTest
{
  @Test
  public void legacyConstructor_hasNullCategory() {
    SourceControlException e = new SourceControlException("boom");
    assertThat(e.getCategory()).isNull();
    assertThat(e.isPartialFailure()).isFalse();
    assertThat(e.getMessage()).isEqualTo("boom");
  }

  @Test
  public void messageAndCategoryConstructor_setsBoth() {
    SourceControlException e =
        new SourceControlException("boom", PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND);
    assertThat(e.getCategory()).isEqualTo(PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND);
    assertThat(e.getMessage()).isEqualTo("boom");
  }

  @Test
  public void messageCategoryAndCauseConstructor_setsAll() {
    Throwable cause = new RuntimeException("cause");
    SourceControlException e =
        new SourceControlException("boom", PullRequestFailureCategory.SCM_ERROR, cause);
    assertThat(e.getCategory()).isEqualTo(PullRequestFailureCategory.SCM_ERROR);
    assertThat(e.getCause()).isSameAs(cause);
  }
}
