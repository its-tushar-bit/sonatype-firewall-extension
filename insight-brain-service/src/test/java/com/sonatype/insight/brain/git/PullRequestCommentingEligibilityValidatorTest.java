/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

public class PullRequestCommentingEligibilityValidatorTest
    extends AbstractComponentTest
{
  @Test
  public void testIsLocationDiscoveryNeededAndAllowed_isNeededAndIsAllowed() {
    // given: the default scenario with everything enabled
    TestScenario testScenario = new TestScenario()
        .withLineCommentingFeatureFlagEnabled(true)
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
        .withLineCommentingFeatureFlagEnabled(false)
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
        .withLineCommentingFeatureFlagEnabled(true)
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
        .withLineCommentingFeatureFlagEnabled(false)
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
        .withLineCommentingFeatureFlagEnabled(false)
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
        .withLineCommentingFeatureFlagEnabled(false)
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
        .withLineCommentingFeatureFlagEnabled(true)
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
        .withLineCommentingFeatureFlagEnabled(true)
        .withProviderSupportingCodeInsights(false)
        .withProviderSupportingLineCommenting(true)
        .withAppearedViolations(true);

    // when: needed but not eligible
    assertThat(testScenario.isLocationDiscoveryNeededAndAllowed()).isTrue();
  }

  @Test
  public void testIsPullRequestCommentingEnabled_dbFieldEnabled() {
    // given: the default scenario
    TestScenario testScenario = new TestScenario()
        .withPrCommentingEnableForApplication(true);

    // when:
    assertThat(testScenario.isPullRequestCommentingEnabled()).isTrue();
  }

  @Test
  public void testIsPullRequestCommentingEnabled_dbFieldDisabled() {
    // given: PR commenting DB feature disabled
    TestScenario testScenario = new TestScenario()
        .withPrCommentingEnableForApplication(false);

    // when:
    assertThat(testScenario.isPullRequestCommentingEnabled()).isFalse();
  }

  @Test
  public void testIsPullRequestCommentingEnabled_scmNotSetUp() {
    // given: IQ for SCM not setup at all
    TestScenario testScenario = new TestScenario();

    // when:
    assertThat(testScenario.isPullRequestCommentingEnabled()).isFalse();
  }

  @Test
  public void testIsPullRequestLineCommentingEnabled_dbFieldEnabled_featureFlagEnabled_providerSupported() {
    testIsPullRequestLineCommentingEnabled(true, true, true, true);
  }

  @Test
  public void testIsPullRequestLineCommentingEnabled_dbFieldEnabled_featureFlagEnabled_providerNotSupported() {
    testIsPullRequestLineCommentingEnabled(true, true, false, false);
  }

  @Test
  public void testIsPullRequestLineCommentingEnabled_dbFieldEnabled_featureFlagDisabled_providerSupported() {
    testIsPullRequestLineCommentingEnabled(true, false, true, false);
  }

  @Test
  public void testIsPullRequestLineCommentingEnabled_dbFieldEnabled_featureFlagDisabled_providerNotSupported() {
    testIsPullRequestLineCommentingEnabled(true, false, false, false);
  }

  @Test
  public void testIsPullRequestLineCommentingEnabled_dbFieldDisabled_featureFlagEnabled_providerSupported() {
    testIsPullRequestLineCommentingEnabled(false, true, true, true);
  }

  @Test
  public void testIsPullRequestLineCommentingEnabled_dbFieldDisabled_featureFlagEnabled_providerNotSupported() {
    testIsPullRequestLineCommentingEnabled(false, true, false, false);
  }

  @Test
  public void testIsPullRequestLineCommentingEnabled_dbFieldDisabled_featureFlagDisabled_providerSupported() {
    testIsPullRequestLineCommentingEnabled(false, false, true, false);
  }

  @Test
  public void testIsPullRequestLineCommentingEnabled_dbFieldDisabled_featureFlagDisabled_providerNotSupported() {
    testIsPullRequestLineCommentingEnabled(false, false, false, false);
  }

  private void testIsPullRequestLineCommentingEnabled(
      boolean dbFieldEnabled,
      boolean featureFlagEnabled,
      boolean providerSupported,
      boolean expectedResult)
  {
    // given:
    TestScenario testScenario = new TestScenario()
        .withPrCommentingEnableForApplication(dbFieldEnabled)
        .withLineCommentingFeatureFlagEnabled(featureFlagEnabled)
        .withProviderSupportingLineCommenting(providerSupported);
    // expect:
    assertThat(testScenario.isPullRequestLineCommentingEnabled()).isEqualTo(expectedResult);
  }

  @Test
  public void testIsPullRequestLineCommentingEnabled_scmNotSetUp() {
    // given: IQ for SCM not setup at all
    TestScenario testScenario = new TestScenario();

    // when:
    assertThat(testScenario.isPullRequestLineCommentingEnabled()).isFalse();
  }

  private static class TestScenario
  {
    @Mock
    private PolicyViolationDiff<PolicyViolation> mockPolicyViolationDiff;

    @Mock
    private SourceControlProvider mockSourceControlProvider;

    private final GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();

    TestScenario withAppearedViolations(boolean hasAppearedViolations) {
      lenient().doReturn(hasAppearedViolations).when(mockPolicyViolationDiff).hasAppeared();
      return this;
    }

    TestScenario withLineCommentingFeatureFlagEnabled(boolean enabled) {
      SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.setEnabled(enabled);
      return this;
    }

    TestScenario withProviderSupportingCodeInsights(boolean supportsCodeInsights) {
      lenient().doReturn(supportsCodeInsights).when(mockSourceControlProvider).supportsCodeInsights();
      return this;
    }

    TestScenario withProviderSupportingLineCommenting(boolean supportsLineCommenting) {
      lenient().doReturn(supportsLineCommenting).when(mockSourceControlProvider).supportsPullRequestLineCommenting();
      return this;
    }

    TestScenario withPrCommentingEnableForApplication(boolean supportsLineCommenting) {
      gitRepositoryInfo.pullRequestCommentingEnabled = supportsLineCommenting;
      return this;
    }

    TestScenario() {
      MockitoAnnotations.openMocks(this);
      gitRepositoryInfo.provider = mockSourceControlProvider;
    }

    boolean isLocationDiscoveryNeededAndAllowed() {
      return new PullRequestCommentingEligibilityValidator()
          .isLocationDiscoveryNeededAndAllowed(mockSourceControlProvider, mockPolicyViolationDiff);
    }

    boolean isPullRequestCommentingEnabled() {
      return new PullRequestCommentingEligibilityValidator().isPullRequestCommentingEnabled(gitRepositoryInfo);
    }

    boolean isPullRequestLineCommentingEnabled() {
      return new PullRequestCommentingEligibilityValidator().isPullRequestLineCommentingEnabled(gitRepositoryInfo);
    }
  }
}
