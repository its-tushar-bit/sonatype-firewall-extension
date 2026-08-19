/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.error.HttpStatusCode;

@HttpStatusCode(400 /* HttpServletResponse.SC_BAD_REQUEST */)
@SuppressWarnings("serial")
public class InvalidPolicyException
    extends RuntimeException
{
  public InvalidPolicyException(String message) {
    super(message);
  }

  public InvalidPolicyException(ValidationResult validationResult) {
    super(validationResult.toMessageString());
  }
}
