/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

@Named
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
    apiSourceControlDTO.token = sourceControl.getToken();
    apiSourceControlDTO.provider = sourceControl.getProvider() == null ? null : sourceControl.getProvider().toString();
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

    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(dto.ownerId).setRepositoryUrl(dto.repositoryUrl).setToken(dto.token)
            .setProvider(getSourceControlProvider(dto.provider)).setEnablePullRequests(dto.enablePullRequests)
            .setEnableStatusChecks(dto.enableStatusChecks).setBaseBranch(dto.baseBranch).build();
    return sourceControl;
  }
}
