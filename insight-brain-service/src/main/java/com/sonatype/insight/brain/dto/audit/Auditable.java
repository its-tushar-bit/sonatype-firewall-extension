/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto.audit;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

/**
 * @since 1.13.0
 */
public abstract class Auditable
{
  private ComponentIdentifier componentIdentifier;

  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  public void setComponentIdentifier(final ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }
}
