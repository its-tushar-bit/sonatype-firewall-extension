/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.ByteArrayOutputStream;

public class SupportInfo
{
  private final ByteArrayOutputStream supportInfoOutputStream;

  private final String supportInfoName;

  public SupportInfo(ByteArrayOutputStream supportInfoOutputStream, String supportInfoName) {
    this.supportInfoOutputStream = supportInfoOutputStream;
    this.supportInfoName = supportInfoName;
  }

  public ByteArrayOutputStream getSupportInfoOutputStream() {
    return supportInfoOutputStream;
  }

  public String getSupportInfoName() {
    return supportInfoName;
  }
}
