/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

@Named
@Singleton
public class ApiSourceControlAdapter
{
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
    apiSourceControlDTO.remediationPullRequestsEnabled = sourceControl.getRemediationPullRequestsEnabled();
    apiSourceControlDTO.statusChecksEnabled = sourceControl.getStatusChecksEnabled();
    apiSourceControlDTO.pullRequestCommentingEnabled = sourceControl.getStatusChecksEnabled();
    apiSourceControlDTO.sourceControlScansEnabled = sourceControl.getSourceControlScansEnabled();
    apiSourceControlDTO.sourceControlScanTarget = sourceControl.getSourceControlScanTarget();

    return apiSourceControlDTO;
  }

  private SourceControlProvider getSourceControlProvider(final String provider) {
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

  public SourceControl convertFromDTO(final ApiSourceControlDTO dto) {
    if (dto == null) {
      return null;
    }

    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(dto.ownerId)
        .setRepositoryUrl(dto.repositoryUrl).setUsername(dto.username).setToken(dto.token)
        .setProvider(getSourceControlProvider(dto.provider))
        .setRemediationPullRequestsEnabled(dto.remediationPullRequestsEnabled)
        .setStatusChecksEnabled(dto.statusChecksEnabled).setBaseBranch(dto.baseBranch)
        .setPullRequestCommentingEnabled(dto.pullRequestCommentingEnabled)
        .setSourceControlScansEnabled(dto.sourceControlScansEnabled)
        .setSourceControlScanTarget(dto.sourceControlScanTarget).build();
    
    return sourceControl;
  }
}
