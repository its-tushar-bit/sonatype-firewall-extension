/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ManualPullRequestFeatureCheckTest
    extends AbstractComponentTest
{
  @Inject
  private ManualPullRequestFeatureCheck manualPullRequestFeatureCheck;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private ScmRepoVisibilityService mockScmRepoVisibilityService;

  @Override
  public void configure(final Binder binder) {
    binder.bind(ScmRepoVisibilityService.class).toInstance(mockScmRepoVisibilityService);
    super.configure(binder);
  }

  @Test
  public void testManualPullRequestSupported() {
    for (SourceControlProvider provider : SourceControlProvider.values()) {
      GitRepositoryInfo gitRepositoryInfo = newRepositoryInfo(provider);
      when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(gitRepositoryInfo)).thenReturn(true);

      Optional<ManualPullRequestImpossibilityReason> result = manualPullRequestFeatureCheck
          .isManualPullRequestFeatureSupported(gitRepositoryInfo);

      assertThat(result).isNotPresent();
    }
  }

  @Test
  public void testManualPullRequestLicenseInvalid() {
    testProductLicense.setMissingFeatures(LicensedFeature.AUTOMATION);

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestFeatureCheck.isManualPullRequestFeatureSupported(
            newRepositoryInfo(SourceControlProvider.GITHUB));

    assertThat(result).contains(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_LICENSE);
  }

  @Test
  public void testManualPullRequestConfigurationDisabled() {
    GitRepositoryInfo gitRepositoryInfo = newRepositoryInfo(SourceControlProvider.GITHUB);
    gitRepositoryInfo.manualPullRequestsEnabled = false;

    Optional<ManualPullRequestImpossibilityReason> result = manualPullRequestFeatureCheck
        .isManualPullRequestFeatureSupported(gitRepositoryInfo);

    assertThat(result).contains(ManualPullRequestImpossibilityReason.CONFIGURATION_DISABLED);
  }

  @Test
  public void testManualPullRequestConfigurationNull() {
    GitRepositoryInfo gitRepositoryInfo = newRepositoryInfo(SourceControlProvider.GITHUB);
    gitRepositoryInfo.manualPullRequestsEnabled = null;

    Optional<ManualPullRequestImpossibilityReason> result = manualPullRequestFeatureCheck
        .isManualPullRequestFeatureSupported(gitRepositoryInfo);

    assertThat(result).contains(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_REPOSITORY);
  }

  @Test
  public void testManualPullRequestConfigurationTrue() {
    GitRepositoryInfo gitRepositoryInfo = newRepositoryInfo(SourceControlProvider.GITHUB);
    gitRepositoryInfo.manualPullRequestsEnabled = true;

    Optional<ManualPullRequestImpossibilityReason> result = manualPullRequestFeatureCheck
        .isManualPullRequestFeatureSupported(gitRepositoryInfo);

    assertThat(result).contains(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_REPOSITORY);
  }

  @Test
  public void testManualPullRequestSCMNotConfigured() {
    for (SourceControlProvider provider : SourceControlProvider.values()) {
      GitRepositoryInfo gitRepositoryInfo = null;
      ensureSCMNotConfiguredForManualPRs(gitRepositoryInfo);

      gitRepositoryInfo = newRepositoryInfo(provider);
      gitRepositoryInfo.provider = null;
      ensureSCMNotConfiguredForManualPRs(gitRepositoryInfo);

      gitRepositoryInfo = newRepositoryInfo(provider);
      gitRepositoryInfo.repositoryUrl = "  ";
      ensureSCMNotConfiguredForManualPRs(gitRepositoryInfo);

      if (provider.requiresUsername()) {
        gitRepositoryInfo = newRepositoryInfo(provider);
        gitRepositoryInfo.username = null;
        ensureSCMNotConfiguredForManualPRs(gitRepositoryInfo);
      }

      gitRepositoryInfo = newRepositoryInfo(provider);
      gitRepositoryInfo.token = null;
      ensureSCMNotConfiguredForManualPRs(gitRepositoryInfo);
    }
  }

  private void ensureSCMNotConfiguredForManualPRs(final GitRepositoryInfo gitRepositoryInfo) {
    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestFeatureCheck.isManualPullRequestFeatureSupported(gitRepositoryInfo);

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(ManualPullRequestImpossibilityReason.SCM_NOT_CONFIGURED);
    assertThat(result).contains(ManualPullRequestImpossibilityReason.SCM_NOT_CONFIGURED);
  }

  @Test
  public void testManualPullRequestNotSupportedForProvider() {
    SourceControlProvider provider = mock(SourceControlProvider.class);
    when(provider.supportsPullRequests()).thenReturn(false);

    GitRepositoryInfo gitRepositoryInfo = newRepositoryInfo(provider);

    Optional<ManualPullRequestImpossibilityReason> result = manualPullRequestFeatureCheck
        .isManualPullRequestFeatureSupported(gitRepositoryInfo);

    assertThat(result).contains(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_PROVIDER);
  }

  @Test
  public void testManualPullRequestIsNotSupportedForRepository() {
    GitRepositoryInfo gitRepositoryInfo = newRepositoryInfo(SourceControlProvider.GITHUB);
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(gitRepositoryInfo)).thenReturn(false);

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestFeatureCheck.isManualPullRequestFeatureSupported(gitRepositoryInfo);

    assertThat(result).contains(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_REPOSITORY);
  }

  @Test
  public void testManualPullRequestIsNotSupportedForRepository_UnableToConnectToRepository() {
    GitRepositoryInfo gitRepositoryInfo = newRepositoryInfo(SourceControlProvider.GITHUB);
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(gitRepositoryInfo))
        .thenThrow(new UncheckedIOException("", new IOException()));

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestFeatureCheck.isManualPullRequestFeatureSupported(gitRepositoryInfo);

    assertThat(result).contains(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_REPOSITORY);
  }

  private GitRepositoryInfo newRepositoryInfo(SourceControlProvider provider) {
    return new GitRepositoryInfo("repo-url", "ssh-repo-url", "username", "token", provider, "master", true, true, true,
        true,
        true, true, false, null);
  }
}
