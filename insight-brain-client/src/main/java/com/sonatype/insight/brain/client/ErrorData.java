/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

/**
 * DTO that presents the data structure for the exported result file in case of error.
 *
 * @since 1.72.0
 */
public class ErrorData
{
  public String errorMessage;

  public boolean isSystemError;
}
