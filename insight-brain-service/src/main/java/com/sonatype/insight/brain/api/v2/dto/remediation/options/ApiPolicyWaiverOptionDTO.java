/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation.options;

import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiRemediationRestActionDTO;

/**
 * @since 1.64
 */
public class ApiPolicyWaiverOptionDTO
    implements ApiRemediationOptionDTO<String, ApiRemediationRestActionDTO>
{
  private final String type = "policy-waiver";

  private ApiRemediationRestActionDTO data;

  public ApiPolicyWaiverOptionDTO(ApiRemediationRestActionDTO data) {
    this.data = data;
  }

  // for JSON
  public ApiPolicyWaiverOptionDTO() {
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public ApiRemediationRestActionDTO getData() {
    return data;
  }

  // for JSON
  public void setData(ApiRemediationRestActionDTO data) {
    this.data = data;
  }
}
