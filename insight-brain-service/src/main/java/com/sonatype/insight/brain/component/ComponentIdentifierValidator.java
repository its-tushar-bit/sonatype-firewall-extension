/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.13.0
 */
public class ComponentIdentifierValidator
{
  public static void validate(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      throw new BadRequestException("The component identifier cannot be null.");
    }

    try {
      componentIdentifier.validate();
    }
    catch (Exception e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }
}
