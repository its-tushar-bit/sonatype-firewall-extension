/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.validation;

import jakarta.inject.Named;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;

@Named
public class DefaultSourceControlSshValidator
    implements SourceControlSshValidator
{
  @Override
  public void validate(final SourceControl sourceControl) {
    // On-prem doesn't impose any restriction in SSH source control configuration
  }
}
