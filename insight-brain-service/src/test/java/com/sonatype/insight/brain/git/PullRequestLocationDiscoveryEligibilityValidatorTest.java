/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

public class PullRequestLocationDiscoveryEligibilityValidatorTest
{
  @Test
  public void testIsLocationDiscoveryNeededAndAllowed_isNeededAndIsAllowed() {
    // given: the default scenario with everything enabled
    TestScenario testScenario = new TestScenario()
        .withLineCommentingFeatureEnabled(true)
        .withProviderSupportingCodeInsights(true)
        .withProviderSupportingLineCommenting(true)
        .withAppearedViolations(true);

    // when:
    assertThat(testScenario.isLocationDiscoveryNeededAndAllowed()).isTrue();
  }

  @Test
  public void testIsLocationDiscoveryNeededAndAllowed_notEligible() {
    // given: turn off all eligibility indicators
    TestScenario testScenario = new TestScenario()
        .withLineCommentingFeatureEnabled(false)
        .withProviderSupportingCodeInsights(false)
        .withProviderSupportingLineCommenting(false)
        .withAppearedViolations(true);

    // when: needed but not eligible
    assertThat(testScenario.isLocationDiscoveryNeededAndAllowed()).isFalse();
  }

  @Test
  public void testIsLocationDiscoveryNeededAndAllowed_notEligibleButFeatureEnabled() {
    // given: needed but only the feature is enabled
    TestScenario testScenario = new TestScenario()
        .withLineCommentingFeatureEnabled(true)
        .withProviderSupportingCodeInsights(false)
        .withProviderSupportingLineCommenting(false)
        .withAppearedViolations(true);

    // when: needed but not eligible
    assertThat(testScenario.isLocationDiscoveryNeededAndAllowed()).isFalse();
  }

  @Test
  public void testIsLocationDiscoveryNeededAndAllowed_notEligibleButSupportsLineCommenting() {
    // given: needed and provider supports line commenting but line commenting not enabled
    TestScenario testScenario = new TestScenario()
        .withLineCommentingFeatureEnabled(false)
        .withProviderSupportingCodeInsights(false)
        .withProviderSupportingLineCommenting(true)
        .withAppearedViolations(true);

    // when: needed but not eligible
    assertThat(testScenario.isLocationDiscoveryNeededAndAllowed()).isFalse();
  }

  @Test
  public void testIsLocationDiscoveryNeededAndAllowed_supportsCodeInsightsButNotNeeded() {
    // given: scenario supports code insights but discovery not needed
    TestScenario testScenario = new TestScenario()
        .withLineCommentingFeatureEnabled(false)
        .withProviderSupportingCodeInsights(true)
        .withProviderSupportingLineCommenting(false)
        .withAppearedViolations(false);

    // when: needed but not eligible
    assertThat(testScenario.isLocationDiscoveryNeededAndAllowed()).isFalse();
  }

  @Test
  public void testIsLocationDiscoveryNeededAndAllowed_supportsCodeInsightsAndNeeded() {
    // given: scenario supports code insights and discovery is needed
    TestScenario testScenario = new TestScenario()
        .withLineCommentingFeatureEnabled(false)
        .withProviderSupportingCodeInsights(true)
        .withProviderSupportingLineCommenting(false)
        .withAppearedViolations(true);

    // when: needed but not eligible
    assertThat(testScenario.isLocationDiscoveryNeededAndAllowed()).isTrue();
  }

  @Test
  public void testIsLocationDiscoveryNeededAndAllowed_supportsLineCommentingButNotNeeded() {
    // given: scenario supports lince commenting but discovery not needed
    TestScenario testScenario = new TestScenario()
        .withLineCommentingFeatureEnabled(true)
        .withProviderSupportingCodeInsights(false)
        .withProviderSupportingLineCommenting(true)
        .withAppearedViolations(false);

    // when: needed but not eligible
    assertThat(testScenario.isLocationDiscoveryNeededAndAllowed()).isFalse();
  }

  @Test
  public void testIsLocationDiscoveryNeededAndAllowed_supportsLineCommentingAndDiscoveryNeeded() {
    // given: scenario supports lince commenting but discovery not needed
    TestScenario testScenario = new TestScenario()
        .withLineCommentingFeatureEnabled(true)
        .withProviderSupportingCodeInsights(false)
        .withProviderSupportingLineCommenting(true)
        .withAppearedViolations(true);

    // when: needed but not eligible
    assertThat(testScenario.isLocationDiscoveryNeededAndAllowed()).isTrue();
  }

  private class TestScenario
  {
    @Mock
    private InsightConfig mockInsightConfig;

    @Mock
    private PolicyViolationDiff<PolicyViolation> mockPolicyViolationDiff;

    @Mock
    private SourceControlProvider mockSourceControlProvider;

    TestScenario withAppearedViolations(boolean hasAppearedViolations) {
      doReturn(hasAppearedViolations).when(mockPolicyViolationDiff).hasAppeared();
      return this;
    }

    TestScenario withLineCommentingFeatureEnabled(boolean enabled) {
      doReturn(enabled).when(mockInsightConfig).isFeatureEnabled(Feature.PR_LINE_COMMENTING);
      return this;
    }

    TestScenario withProviderSupportingCodeInsights(boolean supportsCodeInsights) {
      doReturn(supportsCodeInsights).when(mockSourceControlProvider).supportsCodeInsights();
      return this;
    }

    TestScenario withProviderSupportingLineCommenting(boolean supportsLineCommenting) {
      doReturn(supportsLineCommenting).when(mockSourceControlProvider).supportsPullRequestLineCommenting();
      return this;
    }

    TestScenario() {
      MockitoAnnotations.openMocks(this);
    }

    boolean isLocationDiscoveryNeededAndAllowed() {
      return new PullRequestLocationDiscoveryEligibilityValidator(mockInsightConfig)
          .isLocationDiscoveryNeededAndAllowed(mockSourceControlProvider, mockPolicyViolationDiff);
    }
  }
}
