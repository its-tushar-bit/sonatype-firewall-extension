/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import com.sonatype.insight.brain.HttpRequest;

@Deprecated
public class DeprecatedVersionResourceTest
    extends VersionResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return HttpRequest.to(getRestBaseUrl()).path(DeprecatedVersionResource.RESOURCE_PATH).anon();
  }
}
