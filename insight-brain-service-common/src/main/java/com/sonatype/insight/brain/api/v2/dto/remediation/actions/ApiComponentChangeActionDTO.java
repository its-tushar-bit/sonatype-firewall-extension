/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation.actions;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;

/**
 * @since 1.64
 */
public class ApiComponentChangeActionDTO
{
  private ApiComponentDTOV2 component;

  public ApiComponentChangeActionDTO(ApiComponentDTOV2 component) {
    this.component = component;
  }

  // for JSON
  public ApiComponentChangeActionDTO() {
  }

  public ApiComponentDTOV2 getComponent() {
    return component;
  }

  // for JSON
  public void setComponent(ApiComponentDTOV2 component) {
    this.component = component;
  }
}
