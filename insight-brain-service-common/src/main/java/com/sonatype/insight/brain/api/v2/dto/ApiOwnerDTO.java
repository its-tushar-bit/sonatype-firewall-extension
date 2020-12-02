/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.model.Owner;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ApiOwnerDTO
{
  @JsonInclude(Include.NON_NULL)
  public String ownerPublicId;

  public String ownerId;

  @JsonInclude(Include.NON_NULL)
  public String ownerName;

  public String ownerType;

  public static ApiOwnerDTO fromOwner(Owner ownerModel) {
    ApiOwnerDTO ownerDTO = new ApiOwnerDTO();
    ownerDTO.ownerType = ownerModel.getType().name();
    ownerDTO.ownerId = ownerModel.getId();
    ownerDTO.ownerPublicId = ownerModel.getPublicId();
    ownerDTO.ownerName = ownerModel.getName();
    return ownerDTO;
  }
}
