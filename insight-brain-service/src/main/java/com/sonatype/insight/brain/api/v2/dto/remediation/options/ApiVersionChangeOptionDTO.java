/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation.options;

import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;

/**
 * @since 1.64
 */
public class ApiVersionChangeOptionDTO
    implements ApiRemediationOptionDTO<ApiVersionChangeOptionType, ApiComponentChangeActionDTO>
{
  private ApiVersionChangeOptionType type;

  private ApiComponentChangeActionDTO data;

  public ApiVersionChangeOptionDTO(ApiVersionChangeOptionType type, ApiComponentChangeActionDTO data) {
    this.type = type;
    this.data = data;
  }

  // for JSON
  public ApiVersionChangeOptionDTO() {
  }

  @Override
  public ApiVersionChangeOptionType getType() {
    return type;
  }

  @Override
  public ApiComponentChangeActionDTO getData() {
    return data;
  }

  // for JSON
  public void setType(ApiVersionChangeOptionType type) {
    this.type = type;
  }

  // for JSON
  public void setData(ApiComponentChangeActionDTO data) {
    this.data = data;
  }
}

