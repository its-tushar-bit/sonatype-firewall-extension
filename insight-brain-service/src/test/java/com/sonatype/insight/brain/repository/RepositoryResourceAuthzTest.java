/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class RepositoryResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RepositoryResource.RESOURCE_PATH);
  }

  @Test
  public void testGenerateIcon() throws Exception {
    HttpRequest request = restRequest().path(RepositoryResource.GENERATE_ICON_PATH).parameter("hash");
    testAuthcGet(request);
  }

  @Test
  public void testGetIcon() throws Exception {
    grantReadPermission(repositoryManager.getId());

    HttpRequest request =
        restRequest().path(RepositoryResource.REPOSITORY_MANAGER_ICON_PATH).parameter(repositoryManager.getId());
    testAuthzGet(request);
  }

  @Test
  public void testSetIcon() throws Exception {
    grantWritePermission(repositoryManager.getId());

    HttpRequest request =
        restRequest().path(RepositoryResource.REPOSITORY_MANAGER_ICON_PATH)
            .parameter(repositoryManager.getId())
            .part("hasRobotSource", "false");
    testAuthzPost(request);
  }
}
