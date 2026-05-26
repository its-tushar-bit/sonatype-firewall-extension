/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.validation;

import jakarta.inject.Named;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;

import org.springframework.context.annotation.Primary;

@Named
@Primary
public class MtiqSourceControlSshValidator
    implements SourceControlSshValidator
{
  @Override
  public void validate(final SourceControl sourceControl) {
    // SSH not enabled on MTIQ
    if (hasInvalidSshConfiguration(sourceControl)) {
      throw new BadRequestException("SSH not supported");
    }
  }

  private boolean hasInvalidSshConfiguration(final SourceControl sourceControl) {
    return Boolean.TRUE.equals(sourceControl.getSshEnabled());
  }
}
