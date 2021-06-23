/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;

public class TestNamedComponentDetails
    extends NamedComponentDetails
{
  private ComponentDisplayName displayName;

  @Override
  public ComponentDisplayName getDisplayName() {
    return displayName;
  }

  public void setDisplayName(ComponentDisplayName displayName) {
    this.displayName = displayName;
  }
}
