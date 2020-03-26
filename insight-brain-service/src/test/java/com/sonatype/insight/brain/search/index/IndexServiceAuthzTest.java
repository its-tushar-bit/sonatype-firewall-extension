/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class IndexServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private IndexService indexService;

  @Test(expected = UnauthenticatedException.class)
  public void testCreateSearchIndex__Unauthenticated() {
    indexService.createSearchIndexAsync();
  }

  @Test(expected = UnauthorizedException.class)
  public void testCreateSearchIndex_Unauthorized() {
    login();
    indexService.createSearchIndexAsync();
  }

  @Test
  public void testCreateSearchIndex_Authorized() {
    grantConfigureSystemPermission();
    indexService.createSearchIndexAsync();
  }
}
