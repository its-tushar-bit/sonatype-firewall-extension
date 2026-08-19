/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.validation;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;

public interface SourceControlSshValidator
{
  /**
   * Validates if the SSH configuration is valid. In case it is not valid implementations must throw an exception.
   *
   * @param sourceControl sourceControl configuration entity to validate
   */
  void validate(final SourceControl sourceControl);
}
