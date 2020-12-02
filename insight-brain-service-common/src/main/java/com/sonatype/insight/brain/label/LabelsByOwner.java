/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.model.OwnerType;

public class LabelsByOwner
{
  public String ownerId;

  public String ownerName;

  public OwnerType ownerType;

  public List<ApiLabelDTO> labels;
}
