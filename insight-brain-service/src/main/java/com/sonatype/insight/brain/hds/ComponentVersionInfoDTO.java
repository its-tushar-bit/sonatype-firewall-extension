/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;

public class ComponentVersionInfoDTO
{
  public List<ComponentDetailsDTO> allVersions;

  public ApiComponentRemediationValueDTO remediation;

  public ComponentVersionInfoDTO() {
    // for jackson
  }

  public ComponentVersionInfoDTO(List<ComponentDetailsDTO> allVersions, ApiComponentRemediationValueDTO remediation) {
    this.allVersions = allVersions;
    this.remediation = remediation;
  }
}
