/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock;

import com.sonatype.insight.mock.InsightMockServer.ResponseProvider;

class SimpleResponseProvider
    implements ResponseProvider
{
  private final int status;

  private final Object body;

  public SimpleResponseProvider(int status, Object body) {
    this.status = status;
    this.body = body;
  }

  @Override
  public int getStatus() {
    return status;
  }

  @Override
  public Object getBody() {
    return body;
  }
}
