/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.AbstractApiOrganizationResourceTest;
import com.sonatype.insight.brain.api.PublicApiPaths;

public class ApiOrganizationResourceV2Test
    extends AbstractApiOrganizationResourceTest
{

  @Override
  protected String getServiceURL() {
    return getRestBaseUrl() + PublicApiPaths.ORG_SERVICE_PATH.replace("{apiVersion: v1|v2}", "v2");
  }
}
