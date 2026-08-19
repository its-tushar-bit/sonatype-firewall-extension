/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation.options;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.64
 */
public class ApiVersionChangeOptionDTO
    implements ApiRemediationOptionDTO<ApiVersionChangeOptionType, ApiComponentChangeActionDTO>
{
  private ApiVersionChangeOptionType type;

  private ApiComponentChangeActionDTO data;

  @JsonInclude(Include.NON_NULL)
  private Boolean directDependency;

  @JsonInclude(Include.NON_EMPTY)
  private List<ApiComponentChangeActionDTO> directDependencyData = new ArrayList<>();

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

  public Boolean getDirectDependency() {
    return directDependency;
  }

  public void setDirectDependency(final Boolean directDependency) {
    this.directDependency = directDependency;
  }

  public List<ApiComponentChangeActionDTO> getDirectDependencyData() {
    return directDependencyData;
  }

  public void setDirectDependencyData(final List<ApiComponentChangeActionDTO> directDependencyData) {
    this.directDependencyData = directDependencyData;
  }
}
