/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiInnerSourceDataDTO;
import com.sonatype.insight.brain.model.component.Component;

@Named
public class ApiInnerSourceDataAdapter
{
  public ApiInnerSourceDataDTO convertToDTO(final Component component) {
    ApiInnerSourceDataDTO innerSourceDataDTO = new ApiInnerSourceDataDTO();
    innerSourceDataDTO.ownerApplicationName = component.getOwnerApplicationName();
    innerSourceDataDTO.ownerApplicationId = component.getOwnerApplicationId();
    innerSourceDataDTO.innerSource = component.getInnerSource();
    return innerSourceDataDTO;
  }
}
