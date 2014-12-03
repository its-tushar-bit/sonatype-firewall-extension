/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1;

import com.sonatype.insight.brain.api.AbstractApiApplicationResourceTest;
import com.sonatype.insight.brain.api.PublicApiPaths;

public class ApiApplicationResourceTest
    extends AbstractApiApplicationResourceTest
{

  @Override
  protected String getServiceURL() {
    return getRestBaseUrl() + PublicApiPaths.APP_SERVICE_PATH.replace("{apiVersion: v1|v2}", "v1");
  }
}
