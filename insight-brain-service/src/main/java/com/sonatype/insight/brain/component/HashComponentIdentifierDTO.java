/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;

/**
 * Enhances {@link HashComponentIdentifier} with additional details required by the UI.
 * This keeps the implementation details of how the {@link ComponentIdentifier} should be rendered out of the client
 * code.
 *
 * @since 1.13.0
 */
public class HashComponentIdentifierDTO
{
  public String id;

  public String hash;

  public String comment;

  public Date createTime;

  public String claimerId;

  public String claimerName;

  public ComponentIdentifier componentIdentifier;

  /**
   * Required by the UI for proper display of the coordinate information.
   */
  public ComponentDisplayName displayName;

  /**
   * Required by the UI for sorting by coordinates.
   */
  public String coordinates;

  public HashComponentIdentifierDTO() {
  }

  public HashComponentIdentifierDTO(HashComponentIdentifier hashComponentIdentifier, ComponentDisplayName displayName) {
    this.id = hashComponentIdentifier.getId();
    this.hash = hashComponentIdentifier.getHash();
    this.comment = hashComponentIdentifier.getComment();
    this.createTime = hashComponentIdentifier.getCreateTime();
    this.claimerId = hashComponentIdentifier.getClaimerId();
    this.claimerName = hashComponentIdentifier.getClaimerName();
    this.componentIdentifier = hashComponentIdentifier.getComponentIdentifier();
    this.displayName = displayName;
    this.coordinates = displayName.toString();
  }
}
