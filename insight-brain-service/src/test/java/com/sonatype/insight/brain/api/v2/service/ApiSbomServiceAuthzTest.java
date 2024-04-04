/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import javax.inject.Inject;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.error.exception.BadRequestException;
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

  private static final String DUMMY_IMPORT_REQUEST_ID = UUID.randomUUID().toString().replace("-", "");

  private static final String DUMMY_APP_VERSION = RandomStringUtils.random(10, true, true);

  private static final String DUMMY_USER_AGENT = RandomStringUtils.random(10, true, true);

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
  public void testDeleteSbomVersion_Authorized() {
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

  @Test(expected = UnauthenticatedException.class)
  public void testGetSbomListForAppId_Unauthenticated() {
    apiSbomService.getSbomListForAppId("app1", "asc", 1, 2);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSbomListForAppId_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    apiSbomService.getSbomListForAppId(application.getId(), "asc", 1, 2);
  }

  @Test
  @PostgresTest
  public void testGetSbomListForAppId_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    grantReadPermission(application.getId());
    apiSbomService.getSbomListForAppId(application.getId(), "asc", 1, 2);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testImportSbomVersion_Unauthenticated() {
    apiSbomService.importSbom(DUMMY_APP_ID, new ByteArrayInputStream(new byte[0]), DUMMY_USER_AGENT);
  }

  @Test(expected = UnauthorizedException.class)
  public void testImportSbomVersion_Unauthorized() {
    Application app = tempEntity.newApplicationWithParent();
    login();
    apiSbomService.importSbom(app.getId(), new ByteArrayInputStream(new byte[0]), DUMMY_USER_AGENT);
  }

  @Test
  public void testImportSbomVersion_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantWritePermission(app.getId());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> apiSbomService.importSbom(app.getId(), new ByteArrayInputStream(new byte[0]), DUMMY_USER_AGENT))
        .withMessage("provided content is not recognizable as an SBOM");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSbomComponents_Unauthenticated() {
    apiSbomService.getSbomComponents("test-app", "test-version");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSbomComponents_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    apiSbomService.getSbomComponents(application.getId(), "test-version");
  }

  @Test
  @PostgresTest
  public void testGetSbomComponents_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withSbomVersion("test-version")
        .build();

    grantReadPermission(application.getId());
    apiSbomService.getSbomComponents(application.getId(), "test-version");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSbomVersionListForAppId_Unauthenticated() {
    apiSbomService.getSbomVersionListByAppId("test-app-id");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSbomVersionListForAppId_Unauthorized() {
    login();
    apiSbomService.getSbomVersionListByAppId(app.getId());
  }

  @Test
  public void testGetSbomVersionListForAppId_Authorized() {
    grantReadPermission(app.getId());
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSbomVersion("1.5")
        .build();
    apiSbomService.getSbomVersionListByAppId(app.getId());
  }

  @Test(expected = NotFoundException.class)
  public void testGetSbomVersionListForAppId_Authorized_NotFound() {
    grantReadPermission("test");
    apiSbomService.getSbomVersionListByAppId("test");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetImportStatus_Unauthenticated() {
    apiSbomService.getImportStatus(DUMMY_APP_ID, DUMMY_IMPORT_REQUEST_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetImportStatus_Unauthorized() {
    Application app = tempEntity.newApplicationWithParent();
    login();
    apiSbomService.getImportStatus(app.getId(), DUMMY_IMPORT_REQUEST_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testGetImportStatus_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantEvaluateApplicationPermission(app.getId());
    apiSbomService.getImportStatus(app.getId(), DUMMY_IMPORT_REQUEST_ID);
  }
}
