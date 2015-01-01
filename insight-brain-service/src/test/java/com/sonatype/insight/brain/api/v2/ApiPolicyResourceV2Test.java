/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.AbstractApiPolicyResourceTest;
import com.sonatype.insight.brain.api.PublicApiPaths;

public class ApiPolicyResourceV2Test
    extends AbstractApiPolicyResourceTest
{
  @Override
  protected String getServiceURL() {
    return getRestBaseUrl() + PublicApiPaths.POLICY_SERVICE_PATH.replace("{apiVersion: v1|v2}", "v2");
  }
}
