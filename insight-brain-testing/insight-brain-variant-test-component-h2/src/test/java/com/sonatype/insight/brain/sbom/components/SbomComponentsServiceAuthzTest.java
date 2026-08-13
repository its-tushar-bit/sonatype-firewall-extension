/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class SbomComponentsServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private SbomComponentsService sbomComponentsService;

  private ThirdPartySbomMetadata sbomMetadata;

  private ThirdPartyFileCoordinate component;

  @Inject
  private SbomComponentsService service;

  @Inject
  private InsightWork work;

  private static final String DUMMY_APP_ID = UUID.randomUUID().toString().replace("-", "");

  private static final String DUMMY_APP_VERSION = RandomStringUtils.random(10, true, true);

  @BeforeEach
  public void before() throws IOException {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE,
            thirdPartyFile.getFilename());
    component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");
    ThirdPartyCoordinateSecurity vulnerability =
        tempEntity.newThirdPartyCoordinateSecurity(component, "cve", "d1", "l1", 9, "d1", "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerability, "cve", "resolved",
        "code_not_reachable", "response", "details");
    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest", tempDir), reportFile);
  }

  @Test
  public void testGetSbomComponentDetails_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> sbomComponentsService.getSbomComponentDetails("appId", "sbomVersion", "componentHash"));
  }

  @Test
  public void testGetSbomComponentDetails_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthorizedException.class,
        () -> sbomComponentsService.getSbomComponentDetails(application.getId(), "sbomVersion", "componentHash"));
  }

  @Test
  public void testGetSbomComponentDetails_Authorized() {
    grantReadPermission(app.getId());
    CDPSbomComponentDetailsDTO componentDetails =
        sbomComponentsService.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), component.getHash());
    assertThat(componentDetails).isNotNull();
  }

  @Test
  public void testGetSbomMetadata_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> service.getBomPageMetadata(DUMMY_APP_ID, DUMMY_APP_VERSION));
  }

  @Test
  public void testGetSbomMetada_Unauthorized() {
    Application app = tempEntity.newApplicationWithParent();
    login();
    assertThrows(UnauthorizedException.class,
        () -> service.getBomPageMetadata(app.getId(), DUMMY_APP_VERSION));
  }

  @Test
  public void testGetSbomMetadata_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantReadPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> service.getBomPageMetadata(app.getId(), DUMMY_APP_VERSION));
  }

  @Test
  public void testGetSbomSummaryForComponents_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> service.getSbomSummaryForComponents(DUMMY_APP_ID, DUMMY_APP_VERSION));
  }

  @Test
  public void testGetSbomSummaryForComponents_Unauthorized() {
    Application app = tempEntity.newApplicationWithParent();
    login();
    assertThrows(UnauthorizedException.class,
        () -> service.getSbomSummaryForComponents(app.getId(), DUMMY_APP_VERSION));
  }

  @Test
  public void testGetSbomSummaryForComponents_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantReadPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> service.getSbomSummaryForComponents(app.getId(), DUMMY_APP_VERSION));
  }
}
