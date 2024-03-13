/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.UUID;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiSbomServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String DUMMY_APP_ID = UUID.randomUUID().toString().replace("-", "");

  private static final String DUMMY_APP_VERSION = RandomStringUtils.random(10, true, true);

  @Inject
  private ApiSbomService apiSbomService;
  
  @Test(expected = UnauthenticatedException.class)
  public void testDeleteSbomVersion_Unauthenticated() throws IOException {
    apiSbomService.deleteSbomVersion(DUMMY_APP_ID, DUMMY_APP_VERSION);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteSbomVersion_Unauthorized() throws IOException {
    login();
    apiSbomService.deleteSbomVersion(app.getId(), DUMMY_APP_VERSION);
  }

  @Test
  public void testDeleteSbomVersion_Authorized() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    grantWritePermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> apiSbomService.deleteSbomVersion(app.getId(), "some-version"))
        .withMessage("Cannot find version some-version for application with ID " + app.getId() + ".");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSbomVersion_Unauthenticated() {
    apiSbomService.getSbomVersion(DUMMY_APP_ID, DUMMY_APP_VERSION, ApiSbomService.SBOM_STATE_ORIGINAL);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSbomVersion_Unauthorized() {
    login();
    apiSbomService.getSbomVersion(app.getId(), DUMMY_APP_VERSION, ApiSbomService.SBOM_STATE_ORIGINAL);
  }

  @Test
  public void testGetSbomVersion_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> apiSbomService.getSbomVersion(app.getId(), "some-version", ApiSbomService.SBOM_STATE_ORIGINAL))
        .withMessage("Cannot find version some-version for application with ID " + app.getId() + ".");
  }
}
