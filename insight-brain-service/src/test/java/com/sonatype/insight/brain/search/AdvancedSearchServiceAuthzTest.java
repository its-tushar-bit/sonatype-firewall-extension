/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class AdvancedSearchServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private AdvancedSearchService advancedSearchService;

  @Inject
  private TaskScheduler taskScheduler;

  @Test
  public void testSetStatus_Authorized() {
    grantConfigureSystemPermission();
    advancedSearchService.setStatus(new AdvancedSearchStatusDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetStatus_Unauthenticated() {
    advancedSearchService.setStatus(new AdvancedSearchStatusDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetStatus_Unauthorized() {
    login();
    advancedSearchService.setStatus(new AdvancedSearchStatusDTO());
  }

  @Test
  public void testGetStatus_Authorized() {
    taskScheduler.createScheduler();
    login();
    advancedSearchService.getStatus();
  }
}
