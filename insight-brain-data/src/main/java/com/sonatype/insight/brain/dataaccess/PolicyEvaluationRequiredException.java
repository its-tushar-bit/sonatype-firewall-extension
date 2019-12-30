/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.error.HttpStatusCode;

@HttpStatusCode(409)
public class PolicyEvaluationRequiredException
    extends RuntimeException
{
  private static final long serialVersionUID = 2284566547971290095L;

  public PolicyEvaluationRequiredException(String message) {
    super(message);
  }
}
