/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoryDTO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

public class TagsByOwnerDTO
{
  public TagsByOwnerDTO() {
  }

  public TagsByOwnerDTO(Owner owner, List<ApiApplicationCategoryDTO> tags) {
    this.ownerId = owner.getPublicId();
    this.ownerName = owner.getName();
    this.ownerType = owner.getType();
    this.applicationCategories = tags;
  }

  public String ownerId;

  public String ownerName;

  public OwnerType ownerType;

  public List<ApiApplicationCategoryDTO> applicationCategories;
}
