/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.List;

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.tag.ApplicationTag;

public class ApplicationTagsByOwnerDTO
{
  public ApplicationTagsByOwnerDTO() {
  }

  public ApplicationTagsByOwnerDTO(Owner owner, List<ApplicationTag> applicationTags) {
    this.ownerId = owner.getPublicId();
    this.ownerName = owner.getName();
    this.ownerType = owner.getType();
    this.applicationTags = applicationTags;
  }

  public String ownerId;

  public String ownerName;

  public OwnerType ownerType;

  public List<ApplicationTag> applicationTags;
}
