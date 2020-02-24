/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class FullTextSearchServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private FullTextSearchService fullTextSearchService;

  @Test
  public void testSetStatus_Authorized() {
    grantConfigureSystemPermission();
    fullTextSearchService.setStatus(new FullTextSearchStatusDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetStatus_Unauthenticated() {
    fullTextSearchService.setStatus(new FullTextSearchStatusDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetStatus_Unauthorized() {
    login();
    fullTextSearchService.setStatus(new FullTextSearchStatusDTO());
  }

  @Test
  public void testGetStatus_Authorized() {
    grantConfigureSystemPermission();
    fullTextSearchService.getStatus();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetStatus_Unauthenticated() {
    fullTextSearchService.getStatus();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetStatus_Unauthorized() {
    login();
    fullTextSearchService.getStatus();
  }
}
