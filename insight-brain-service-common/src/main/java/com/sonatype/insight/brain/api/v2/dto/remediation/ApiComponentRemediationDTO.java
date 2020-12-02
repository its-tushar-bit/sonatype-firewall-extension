/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation;

/**
 * @since 1.64
 */
public class ApiComponentRemediationDTO
{
  public ApiComponentRemediationValueDTO remediation;

  public ApiComponentRemediationDTO() {
    this.remediation = new ApiComponentRemediationValueDTO();
  }

  public ApiComponentRemediationDTO(ApiComponentRemediationValueDTO remediation) {
    this.remediation = remediation;
  }
}
