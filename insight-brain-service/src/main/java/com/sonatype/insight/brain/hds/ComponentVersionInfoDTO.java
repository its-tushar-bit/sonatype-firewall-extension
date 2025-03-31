/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.repository.RepositorySourceResponseDTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;

public class ComponentVersionInfoDTO
{
  public List<ComponentDetailsDTO> allVersions;

  public ApiComponentRemediationValueDTO remediation;

  public RepositorySourceResponseDTO sourceResponse;

  // temporary hidden: SDEV-751
  @Schema(hidden = true)
  @JsonInclude(Include.NON_NULL)
  public AutomatedRemediationStatusDTO automatedRemediationStatus;

  public ComponentVersionInfoDTO(
      final List<ComponentDetailsDTO> allVersions,
      final ApiComponentRemediationValueDTO remediation,
      final RepositorySourceResponseDTO sourceResponse,
      final AutomatedRemediationStatusDTO automatedRemediationStatus)
  {
    this.allVersions = allVersions;
    this.remediation = remediation;
    this.sourceResponse = sourceResponse;
    this.automatedRemediationStatus = automatedRemediationStatus;
  }

  public ComponentVersionInfoDTO() {
    // for jackson
  }
}
