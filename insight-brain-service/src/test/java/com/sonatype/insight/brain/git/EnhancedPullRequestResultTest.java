/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.nexus.iq.manager.PullRequestResult;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnhancedPullRequestResultTest
{
  public static final ComponentIdentifier COMPONENT =
      ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0");

  @Test
  public void test_successMessage() {
    PullRequestResult prResult = new PullRequestResult();
    prResult.setSuccessful(true);
    EnhancedPullRequestResult result = new EnhancedPullRequestResult(prResult, new Date(),
        COMPONENT, "Bump bar to 1.1", false);
    assertThat(result.getReasoning()).isEqualTo(
        "A pull request was successfully created to remediate policy violations related to \"foo : bar : 1.0\".");
  }

  @Test
  public void test_failureMessage() {
    PullRequestResult prResult = new PullRequestResult();
    prResult.setSuccessful(false);
    EnhancedPullRequestResult result = new EnhancedPullRequestResult(prResult, new Date(),
        COMPONENT, "Bump bar to 1.1", false);
    assertThat(result.getReasoning()).isEqualTo(
        "We were unable to remediate policy violations related to \"foo : bar : 1.0\". This usually indicates that we" +
            " could not find a direct dependency in the project configuration.");
  }

  @Test
  public void test_exceptionMessage() {
    PullRequestResult prResult = new PullRequestResult();
    prResult.setSuccessful(false);
    EnhancedPullRequestResult result = new EnhancedPullRequestResult(prResult, new Date(),
        COMPONENT, "Bump bar to 1.1", true);
    assertThat(result.getReasoning())
        .isEqualTo("An error happened trying to create this PR, look in server logs for more information.");
  }

  @Test
  public void testEnhancedPullRequestResult_manualPR_defaultConstructor() {
    PullRequestResult prResult = new PullRequestResult();
    prResult.setSuccessful(true);
    EnhancedPullRequestResult result = new EnhancedPullRequestResult(prResult, new Date(),
        COMPONENT, "Bump bar to 1.1", false);
    assertThat(result.isManualPR()).isFalse();
  }

  @Test
  public void testEnhancedPullRequestResult_manualPR_extendedConstructor() {
    PullRequestResult prResult = new PullRequestResult();
    prResult.setSuccessful(true);
    EnhancedPullRequestResult result = new EnhancedPullRequestResult(prResult, new Date(),
        COMPONENT, "Bump bar to 1.1", false, true);
    assertThat(result.isManualPR()).isTrue();
  }
}
