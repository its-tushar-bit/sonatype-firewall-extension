/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.util.UUID;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class SbomComponentsServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String DUMMY_APP_ID = UUID.randomUUID().toString().replace("-", "");

  private static final String DUMMY_APP_VERSION = RandomStringUtils.random(10, true, true);

  @Inject
  private SbomComponentsService service;

  @Test(expected = UnauthenticatedException.class)
  public void testGetSbomMetadata_Unauthenticated() {
    service.getBomPageMetadata(DUMMY_APP_ID, DUMMY_APP_VERSION);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSbomMetada_Unauthorized() {
    Application app = tempEntity.newApplicationWithParent();
    login();
    service.getBomPageMetadata(app.getId(), DUMMY_APP_VERSION);
  }

  @Test(expected = NotFoundException.class)
  public void testGetSbomMetadata_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantReadPermission(app.getId());
    service.getBomPageMetadata(app.getId(), DUMMY_APP_VERSION);
  }
}
