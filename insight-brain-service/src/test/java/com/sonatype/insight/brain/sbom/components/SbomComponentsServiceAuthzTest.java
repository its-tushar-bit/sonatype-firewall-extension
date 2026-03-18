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
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;

public class SbomComponentsServiceAuthzTest
    extends AbstractServiceAuthzTest
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

  @Before
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

  @Test(expected = UnauthenticatedException.class)
  public void testGetSbomComponentDetails_Unauthenticated() {
    sbomComponentsService.getSbomComponentDetails("appId", "sbomVersion", "componentHash");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSbomComponentDetails_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    sbomComponentsService.getSbomComponentDetails(application.getId(), "sbomVersion", "componentHash");
  }

  @Test
  public void testGetSbomComponentDetails_Authorized() {
    grantReadPermission(app.getId());
    CDPSbomComponentDetailsDTO componentDetails =
        sbomComponentsService.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), component.getHash());
    assertThat(componentDetails).isNotNull();
  }

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

  @Test(expected = UnauthenticatedException.class)
  public void testGetSbomSummaryForComponents_Unauthenticated() {
    service.getSbomSummaryForComponents(DUMMY_APP_ID, DUMMY_APP_VERSION);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSbomSummaryForComponents_Unauthorized() {
    Application app = tempEntity.newApplicationWithParent();
    login();
    service.getSbomSummaryForComponents(app.getId(), DUMMY_APP_VERSION);
  }

  @Test(expected = NotFoundException.class)
  public void testGetSbomSummaryForComponents_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantReadPermission(app.getId());
    service.getSbomSummaryForComponents(app.getId(), DUMMY_APP_VERSION);
  }
}
