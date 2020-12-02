/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.model.OwnerType;

/**
 * Resource for API Component Label
 */
public interface ApiComponentLabelResourceV2
{
  /**
   * Assigns an existing label to a component identified by hash in a given owner.
   */
  void setComponentLabel(OwnerType ownerType, String internalOwnerId, String componentHash, String labelName);

  /**
   * Deletes the component label identified by hash in a given owner.
   */
  void deleteComponentLabel(OwnerType ownerType, String internalOwnerId, String componentHash, String labelName);
}
