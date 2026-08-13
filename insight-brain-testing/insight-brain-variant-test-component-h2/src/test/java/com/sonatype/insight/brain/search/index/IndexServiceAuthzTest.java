/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class IndexServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private IndexService indexService;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Test
  public void testCreateSearchIndex__Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> indexService.createIndexAsync());
  }

  @Test
  public void testCreateSearchIndex_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> indexService.createIndexAsync());
  }

  @Test
  public void testCreateSearchIndex_Authorized() {
    grantConfigureSystemPermission();
    indexService.createIndexAsync();
  }

  @Test
  public void testCancelFullRebuild_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> indexService.cancelFullRebuild());
  }

  @Test
  public void testCancelFullRebuild_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> indexService.cancelFullRebuild());
  }

  @Test
  public void testCancelFullRebuild_Authorized() {
    grantConfigureSystemPermission();
    indexService.cancelFullRebuild();
  }

  @Test
  public void testIsFullRebuildInProgress_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> indexService.isFullRebuildInProgress());
  }

  @Test
  public void testIsFullRebuildInProgress_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> indexService.isFullRebuildInProgress());
  }

  @Test
  public void testIsFullRebuildInProgress_Authorized() {
    grantConfigureSystemPermission();
    indexService.isFullRebuildInProgress();
  }
}
