/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class AdvancedSearchServiceAuthzTest
    extends AbstractComponentH2AuthzTest
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

  @Test
  public void testSetStatus_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> advancedSearchService.setStatus(new AdvancedSearchStatusDTO()));
  }

  @Test
  public void testSetStatus_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> advancedSearchService.setStatus(new AdvancedSearchStatusDTO()));
  }

  @Test
  public void testGetStatus_Authorized() {
    taskScheduler.createScheduler();
    login();
    advancedSearchService.getStatus();
  }
}
