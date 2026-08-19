/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomVersionsApplicationSortableField;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.variant.AbstractComponentPgAuthzTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentPgTest
public class ApiSbomServiceAuthzTest
    extends AbstractComponentPgAuthzTest
{
  private static final String DUMMY_APP_ID = UUID.randomUUID().toString().replace("-", "");

  private static final String DUMMY_IMPORT_REQUEST_ID = UUID.randomUUID().toString().replace("-", "");

  private static final String DUMMY_APP_VERSION = RandomStringUtils.random(10, true, true);

  private static final String DUMMY_USER_AGENT = RandomStringUtils.random(10, true, true);

  @Inject
  private ApiSbomService apiSbomService;

  @Test
  public void testDeleteSbomVersion_Unauthenticated() throws IOException {
    assertThrows(UnauthenticatedException.class,
        () -> apiSbomService.deleteSbomVersion(DUMMY_APP_ID, DUMMY_APP_VERSION));
  }

  @Test
  public void testDeleteSbomVersion_Unauthorized() throws IOException {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiSbomService.deleteSbomVersion(app.getId(), DUMMY_APP_VERSION));
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

  @Test
  public void testGetSbomVersion_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiSbomService.getSbomVersion(DUMMY_APP_ID, DUMMY_APP_VERSION, ApiSbomService.SBOM_STATE_ORIGINAL, "",
            ""));
  }

  @Test
  public void testGetSbomVersion_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiSbomService.getSbomVersion(app.getId(), DUMMY_APP_VERSION, ApiSbomService.SBOM_STATE_ORIGINAL, "",
            ""));
  }

  @Test
  public void testGetSbomVersion_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> apiSbomService.getSbomVersion(app.getId(), "some-version", ApiSbomService.SBOM_STATE_ORIGINAL, "",
                ""))
        .withMessage("Cannot find version some-version for application with ID " + app.getId() + ".");
  }

  @Test
  public void testGetSbomMetadataSummaryForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiSbomService.getSbomMetadataSummaryForApplication("app1", "asc", 1, 2,
            SbomVersionsApplicationSortableField.IMPORT_DATE, true));
  }

  @Test
  public void testGetSbomMetadataSummaryForApplication_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthorizedException.class,
        () -> apiSbomService.getSbomMetadataSummaryForApplication(application.getId(), "asc", 1, 2,
            SbomVersionsApplicationSortableField.IMPORT_DATE, true));
  }

  @Test
  public void testGetSbomMetadataSummaryForApplication_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    grantReadPermission(application.getId());
    apiSbomService.getSbomMetadataSummaryForApplication(application.getId(), "asc", 1, 2,
        SbomVersionsApplicationSortableField.IMPORT_DATE, true);
  }

  @Test
  public void testImportSbomVersion_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiSbomService.importSbom(DUMMY_APP_ID, new ByteArrayInputStream(new byte[0]), "file.txt", false,
            DUMMY_USER_AGENT, null, false));
  }

  @Test
  public void testImportSbomVersion_Unauthorized() {
    Application app = tempEntity.newApplicationWithParent();
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiSbomService.importSbom(app.getId(), new ByteArrayInputStream(new byte[0]), "file.txt", false,
            DUMMY_USER_AGENT, null, false));
  }

  @Test
  public void testImportSbomVersion_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantWritePermission(app.getId());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiSbomService.importSbom(app.getId(), new ByteArrayInputStream(new byte[0]),
            "file.txt", false, DUMMY_USER_AGENT, null, false))
        .withMessage("Invalid SBOM file input.");
  }

  @Test
  public void testGetSbomComponents_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiSbomService.getSbomComponents("test-app", "test-version", null, null, null, null, true, 3, 1));
  }

  @Test
  public void testGetSbomComponents_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthorizedException.class,
        () -> apiSbomService.getSbomComponents(application.getId(), "test-version", null, null, null, null, true, 3,
            1));
  }

  @Test
  public void testGetSbomComponents_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withSbomVersion("test-version")
        .build();

    grantReadPermission(application.getId());
    apiSbomService.getSbomComponents(application.getId(), "test-version", null, null, null,
        null, true, 3, 1);
  }

  @Test
  public void tetActiveSbomVersionListByApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiSbomService.getActiveSbomVersionListByApplication("test-app-id"));
  }

  @Test
  public void testGetActiveSbomVersionListByApplication_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiSbomService.getActiveSbomVersionListByApplication(app.getId()));
  }

  @Test
  public void testGetActiveSbomVersionListByApplication_Authorized() {
    grantReadPermission(app.getId());
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSbomVersion("1.5")
        .build();
    apiSbomService.getActiveSbomVersionListByApplication(app.getId());
  }

  @Test
  public void testGetActiveSbomVersionListByApplication_Authorized_NotFound() {
    grantReadPermission("test");
    assertThrows(NotFoundException.class,
        () -> apiSbomService.getActiveSbomVersionListByApplication("test"));
  }

  @Test
  public void testGetImportStatus_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiSbomService.getImportStatus(DUMMY_APP_ID, DUMMY_IMPORT_REQUEST_ID));
  }

  @Test
  public void testGetImportStatus_Unauthorized() {
    Application app = tempEntity.newApplicationWithParent();
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiSbomService.getImportStatus(app.getId(), DUMMY_IMPORT_REQUEST_ID));
  }

  @Test
  public void testGetImportStatus_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantEvaluateApplicationPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> apiSbomService.getImportStatus(app.getId(), DUMMY_IMPORT_REQUEST_ID));
  }
}
