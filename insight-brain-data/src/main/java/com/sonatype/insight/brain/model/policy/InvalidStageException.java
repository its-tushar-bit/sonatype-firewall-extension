/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.error.HttpStatusCode;

@SuppressWarnings("serial")
@HttpStatusCode(400)
public class InvalidStageException
    extends RuntimeException
{
  public InvalidStageException(String stageTypeId) {
    super("Invalid stage id=" + stageTypeId);
  }
}
