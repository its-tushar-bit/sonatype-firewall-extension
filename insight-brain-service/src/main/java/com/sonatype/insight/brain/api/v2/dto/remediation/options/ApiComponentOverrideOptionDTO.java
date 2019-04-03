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
public class ApiComponentOverrideOptionDTO
    implements ApiRemediationOptionDTO<ApiComponentOverrideOptionType, ApiRemediationRestActionDTO>
{
  private ApiComponentOverrideOptionType type;

  private ApiRemediationRestActionDTO data;

  public ApiComponentOverrideOptionDTO(ApiComponentOverrideOptionType type, ApiRemediationRestActionDTO data) {
    this.type = type;
    this.data = data;
  }

  // for JSON
  public ApiComponentOverrideOptionDTO() {
  }

  @Override
  public ApiComponentOverrideOptionType getType() {
    return type;
  }

  @Override
  public ApiRemediationRestActionDTO getData() {
    return data;
  }

  // for JSON
  public void setType(ApiComponentOverrideOptionType type) {
    this.type = type;
  }

  // for JSON
  public void setData(ApiRemediationRestActionDTO data) {
    this.data = data;
  }
}
