/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableSet;

@Named
@Singleton
public class ApiSourceControlAdapter
{
  private static final Set<SourceControlProvider> MULTI_TENANT_SCM_PROVIDERS =
      ImmutableSet.of(SourceControlProvider.AZURE, SourceControlProvider.BITBUCKET, SourceControlProvider.GITHUB,
        SourceControlProvider.GITLAB);

  private final ApiConfigFeaturesService configFeaturesService;

  @Inject
  public ApiSourceControlAdapter(final ApiConfigFeaturesService configFeaturesService) {
    this.configFeaturesService = configFeaturesService;
  }

  @SuppressWarnings("deprecation")
  public ApiSourceControlDTO convertToDTO(final SourceControl sourceControl) {
    if (sourceControl == null) {
      return null;
    }

    ApiSourceControlDTO apiSourceControlDTO = new ApiSourceControlDTO();
    apiSourceControlDTO.id = sourceControl.getId();
    apiSourceControlDTO.ownerId = sourceControl.getOwnerId();
    apiSourceControlDTO.repositoryUrl = sourceControl.getRepositoryUrl();
    apiSourceControlDTO.username = sourceControl.getUsername();
    apiSourceControlDTO.token = sourceControl.getToken();
    apiSourceControlDTO.provider = sourceControl.getProvider() == null ? null : sourceControl.getProvider().toString();
    apiSourceControlDTO.baseBranch = sourceControl.getBaseBranch();
    apiSourceControlDTO.closePrOnFailedChecksEnabled = sourceControl.getClosePrOnFailedChecksEnabled();
    apiSourceControlDTO.closePrAfterDaysOpenEnabled = sourceControl.getClosePrAfterDaysOpenEnabled();
    apiSourceControlDTO.closePrAfterDays = sourceControl.getClosePrAfterDays();
    apiSourceControlDTO.remediationPullRequestsEnabled = sourceControl.getRemediationPullRequestsEnabled();
    apiSourceControlDTO.enablePullRequests = apiSourceControlDTO.remediationPullRequestsEnabled;
    apiSourceControlDTO.statusChecksEnabled = sourceControl.getStatusChecksEnabled();
    apiSourceControlDTO.enableStatusChecks = apiSourceControlDTO.statusChecksEnabled;
    apiSourceControlDTO.pullRequestCommentingEnabled = sourceControl.getPullRequestCommentingEnabled();
    apiSourceControlDTO.sourceControlEvaluationsEnabled = sourceControl.getSourceControlEvaluationsEnabled();
    apiSourceControlDTO.sourceControlScanTarget = sourceControl.getSourceControlScanTarget();
    apiSourceControlDTO.sshEnabled = sourceControl.getSshEnabled();
    apiSourceControlDTO.commitStatusEnabled = sourceControl.getCommitStatusEnabled();
    apiSourceControlDTO.manualPullRequestsEnabled = sourceControl.getManualPullRequestsEnabled();
    apiSourceControlDTO.innerSourceAutomatedUpdatesEnabled = sourceControl.getInnerSourceAutomatedUpdatesEnabled();

    return apiSourceControlDTO;
  }

  private SourceControlProvider getSourceControlProvider(final String provider) {
    List<String> availableSourceControlValues = getAvailableSourceControlValues();
    try {
      if (provider != null && availableSourceControlValues.stream().noneMatch(provider::equals)) {
        throw new IllegalArgumentException("Provider not available");
      }
      return SourceControlProvider.fromString(provider);
    }
    catch (IllegalArgumentException ex) {
      String allowedValues = String.join(", ", availableSourceControlValues);
      throw new BadRequestException(String
          .format("SourceControl provider value '%s' is invalid, valid options are: %s", provider,
              allowedValues));
    }
  }

  private List<String> getAvailableSourceControlValues() {
    return Arrays.stream(SourceControlProvider.values())
        .filter(getSourceControlPredicate())
        .map(provider -> provider.name().toLowerCase())
        .collect(Collectors.toList());
  }

  private Predicate<SourceControlProvider> getSourceControlPredicate() {
    if (new TenantUtil().isMultiTenant()) {
      return MULTI_TENANT_SCM_PROVIDERS::contains;
    }
    else {
      return provider -> true;
    }
  }

  private Boolean convertRemediationPullRequestsEnabled(Boolean remediationPullRequestsEnabled) {
    // NOTE: isSaasLifecycleScmPrsEnabled always returns true in self-hosted
    if (configFeaturesService.isSaasLifecycleScmPrsEnabled()) {
      return remediationPullRequestsEnabled;
    }
    return false;
  }

  private Boolean convertManualPullRequestsEnabled(Boolean manualPullRequestsEnabled) {
    if (configFeaturesService.isSaasLifecycleScmPrsEnabled()) {
      return manualPullRequestsEnabled;
    }
    return false;
  }

  private Boolean convertInnerSourceAutomatedUpdatesEnabled(Boolean innerSourceAutomatedUpdatesEnabled) {
    if (configFeaturesService.isSaasLifecycleScmPrsEnabled()) {
      return innerSourceAutomatedUpdatesEnabled;
    }
    return false;
  }

  @SuppressWarnings("deprecation")
  public SourceControl convertFromDTO(final ApiSourceControlDTO dto) {
    if (dto == null) {
      return null;
    }

    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(dto.ownerId)
        .setRepositoryUrl(dto.repositoryUrl).setUsername(dto.username).setToken(dto.token)
        .setProvider(getSourceControlProvider(dto.provider))
        .setRemediationPullRequestsEnabled(
            convertRemediationPullRequestsEnabled(
                dto.remediationPullRequestsEnabled != null ? dto.remediationPullRequestsEnabled : dto.enablePullRequests
            )
        )
        .setStatusChecksEnabled(dto.statusChecksEnabled != null ? dto.statusChecksEnabled : dto.enableStatusChecks)
        .setBaseBranch(dto.baseBranch)
        .setClosePrOnFailedChecksEnabled(dto.closePrOnFailedChecksEnabled)
        .setClosePrAfterDaysOpenEnabled(dto.closePrAfterDaysOpenEnabled)
        .setClosePrAfterDays(dto.closePrAfterDays)
        .setPullRequestCommentingEnabled(dto.pullRequestCommentingEnabled)
        .setSourceControlEvaluationsEnabled(dto.sourceControlEvaluationsEnabled)
        .setSourceControlScanTarget(dto.sourceControlScanTarget)
        .setSshEnabled(dto.sshEnabled)
        .setCommitStatusEnabled(dto.commitStatusEnabled)
        .setManualPullRequestsEnabled(convertManualPullRequestsEnabled(dto.manualPullRequestsEnabled))
        .setInnerSourceAutomatedUpdatesEnabled(
            convertInnerSourceAutomatedUpdatesEnabled(dto.innerSourceAutomatedUpdatesEnabled))
        .build();

    return sourceControl;
  }
}
