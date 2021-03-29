/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.dto;

import java.util.List;

public class ValidationResponse
{
  public boolean isValid;

  public List<String> errorMessages;

  public ValidationResponse(final List<String> errorMessages) {
    isValid = errorMessages.isEmpty();
    this.errorMessages = errorMessages;
  }
}
