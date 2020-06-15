/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.urlrunner;

public class ResponseBody
{
  public ResponseBody(String content, int length) {
    this.content = content;
    this.length = length;
  }

  private String content;

  private int length;

  String getContent() {
    return content;
  }

  void setContent(String content) {
    this.content = content;
  }

  int getLength() {
    return length;
  }

  void setLength(int length) {
    this.length = length;
  }
}
