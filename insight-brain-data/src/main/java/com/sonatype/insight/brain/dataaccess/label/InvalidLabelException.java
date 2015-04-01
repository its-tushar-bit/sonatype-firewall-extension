/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import com.sonatype.insight.error.HttpStatusCode;

@SuppressWarnings("serial")
@HttpStatusCode(400)
public class InvalidLabelException
    extends RuntimeException
{
  public InvalidLabelException(String message) {
    super(message);
  }
}
