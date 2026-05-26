/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

public class IndexServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private IndexService indexService;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Test(expected = UnauthenticatedException.class)
  public void testCreateSearchIndex__Unauthenticated() {
    indexService.createIndexAsync();
  }

  @Test(expected = UnauthorizedException.class)
  public void testCreateSearchIndex_Unauthorized() {
    login();
    indexService.createIndexAsync();
  }

  @Test
  public void testCreateSearchIndex_Authorized() {
    grantConfigureSystemPermission();
    indexService.createIndexAsync();
  }
}
