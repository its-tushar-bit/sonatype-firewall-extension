/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.AbstractApiApplicationResourceTest;
import com.sonatype.insight.brain.api.PublicApiPaths;

public class ApiApplicationResourceTest
    extends AbstractApiApplicationResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.APP_SERVICE_PATH).parameter("v1");
  }
}
