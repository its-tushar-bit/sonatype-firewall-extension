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

  @Test
  public void getCategory_exceptionThrown_returnsScmError() {
    EnhancedPullRequestResult result = new EnhancedPullRequestResult(null, new Date(),
        COMPONENT, "Bump bar to 1.1", true);
    assertThat(result.getCategory()).isEqualTo(PullRequestFailureCategory.SCM_ERROR);
  }

  @Test
  public void getCategory_nullTiming_notException_returnsManifestComponentNotFound() {
    // intuitReasoning() surfaces FAILURE_MESSAGE ("could not find a direct dependency...")
    // for a null-timing non-exception case, so getCategory() must return the matching
    // category. Otherwise the user-facing reason would say one thing and isRetryable
    // would derive from another (UNKNOWN→retryable=true), leaving the Retry button
    // wrongly enabled for what is plainly a manifest-missing failure.
    EnhancedPullRequestResult result = new EnhancedPullRequestResult(null, new Date(),
        COMPONENT, "Bump bar to 1.1", false);
    assertThat(result.getCategory()).isEqualTo(PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND);
  }

  @Test
  public void getCategory_unsuccessfulTiming_returnsManifestComponentNotFound() {
    PullRequestResult timing = new PullRequestResult();
    timing.setSuccessful(false);
    EnhancedPullRequestResult result = new EnhancedPullRequestResult(timing, new Date(),
        COMPONENT, "Bump bar to 1.1", false);
    assertThat(result.getCategory()).isEqualTo(PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND);
  }

  @Test
  public void getCategory_successfulTiming_returnsUnknown() {
    PullRequestResult timing = new PullRequestResult();
    timing.setSuccessful(true);
    EnhancedPullRequestResult result = new EnhancedPullRequestResult(timing, new Date(),
        COMPONENT, "Bump bar to 1.1", false);
    assertThat(result.getCategory()).isEqualTo(PullRequestFailureCategory.UNKNOWN);
  }
}
