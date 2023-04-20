/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

public class ApiSourceControlAdapter
{
  @SuppressWarnings("deprecation")
  public static ApiSourceControlDTO convertToDTO(final SourceControl sourceControl) {
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
    apiSourceControlDTO.remediationPullRequestsEnabled = sourceControl.getRemediationPullRequestsEnabled();
    apiSourceControlDTO.enablePullRequests = apiSourceControlDTO.remediationPullRequestsEnabled;
    apiSourceControlDTO.statusChecksEnabled = sourceControl.getStatusChecksEnabled();
    apiSourceControlDTO.enableStatusChecks = apiSourceControlDTO.statusChecksEnabled;
    apiSourceControlDTO.pullRequestCommentingEnabled = sourceControl.getPullRequestCommentingEnabled();
    apiSourceControlDTO.sourceControlEvaluationsEnabled = sourceControl.getSourceControlEvaluationsEnabled();
    apiSourceControlDTO.sourceControlScanTarget = sourceControl.getSourceControlScanTarget();
    apiSourceControlDTO.sshEnabled = sourceControl.getSshEnabled();
    apiSourceControlDTO.commitStatusEnabled = sourceControl.getCommitStatusEnabled();

    return apiSourceControlDTO;
  }

  private static SourceControlProvider getSourceControlProvider(final String provider) {
    try {
      return SourceControlProvider.fromString(provider);
    }
    catch (IllegalArgumentException ex) {
      String allowedValues = Arrays.stream(SourceControlProvider.values())
          .map(SourceControlProvider::toString)
          .collect(Collectors.joining(", "));
      throw new BadRequestException(String
          .format("SourceControl provider value '%s' is invalid, valid options are: %s", provider,
              allowedValues));
    }
  }

  @SuppressWarnings("deprecation")
  static SourceControl convertFromDTO(final ApiSourceControlDTO dto) {
    if (dto == null) {
      return null;
    }

    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(dto.ownerId)
        .setRepositoryUrl(dto.repositoryUrl).setUsername(dto.username).setToken(dto.token)
        .setProvider(getSourceControlProvider(dto.provider))
        .setRemediationPullRequestsEnabled(
            dto.remediationPullRequestsEnabled != null ? dto.remediationPullRequestsEnabled : dto.enablePullRequests)
        .setStatusChecksEnabled(dto.statusChecksEnabled != null ? dto.statusChecksEnabled : dto.enableStatusChecks)
        .setBaseBranch(dto.baseBranch)
        .setPullRequestCommentingEnabled(dto.pullRequestCommentingEnabled)
        .setSourceControlEvaluationsEnabled(dto.sourceControlEvaluationsEnabled)
        .setSourceControlScanTarget(dto.sourceControlScanTarget)
        .setSshEnabled(dto.sshEnabled)
        .setCommitStatusEnabled(dto.commitStatusEnabled)
        .build();
    
    return sourceControl;
  }
}
