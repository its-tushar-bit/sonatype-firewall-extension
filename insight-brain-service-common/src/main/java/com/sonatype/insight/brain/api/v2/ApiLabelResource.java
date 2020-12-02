/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.label.ApplicableLabels;
import com.sonatype.insight.brain.model.OwnerType;

/**
 * Resource for API Label
 */
public interface ApiLabelResource
{
  /**
   * @param inherit boolean if {@code true} the returned list will include labels inherited from organization
   *                hierarchy, default is {@code false}
   */
  List<ApiLabelDTO> getLabels(OwnerType ownerType, String ownerId, boolean inherit);

  /**
   * Returns all the labels associated with an ownerId. The labels are grouped by ownerId and the owner name and type
   * are returned.
   */
  ApplicableLabels getApplicableLabels(OwnerType ownerType, String ownerId);

  /**
   * Enumerates the contexts (org/app) in which the given label could be applied.
   */
  ApplicableContext getApplicableContexts(OwnerType ownerType, String ownerId, String labelId);

  ApiLabelDTO addLabel(OwnerType ownerType, String ownerId, ApiLabelDTO labelDTO);

  ApiLabelDTO updateLabel(OwnerType ownerType, String ownerId, ApiLabelDTO labelDTO);

  void deleteLabel(OwnerType ownerType, String ownerId, String labelId);
}
