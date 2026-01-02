/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

@Category(SlowTest.class)
public class OrganizationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(OrganizationResource.RESOURCE_PATH);
  }

  @Test
  public void testGenerateIcon() throws Exception {
    HttpRequest request = restRequest().path(OrganizationResource.GENERATE_ICON_PATH).parameter("hash");
    testAuthcGet(request);
  }

  @Test
  public void testGetIcon() throws Exception {
    grantReadPermission(org.getId());

    HttpRequest request = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(org.getId());
    testAuthzGet(request);
  }

  @Test
  public void testSetIcon() throws Exception {
    grantWritePermission(org.getId());

    HttpRequest request = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(org.getId())
        .part("hasRobotSource", "false");
    testAuthzPost(request);
  }
}
